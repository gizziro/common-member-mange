package com.gizzi.core.domain.auth.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.OAuth2ErrorCode;
import com.gizzi.core.common.exception.UserErrorCode;
import com.gizzi.core.common.security.JwtTokenProvider;
import com.gizzi.core.domain.auth.dto.LoginResponseDto;
import com.gizzi.core.domain.auth.dto.OAuth2LoginResultDto;
import com.gizzi.core.domain.auth.dto.OAuth2ProviderDto;
import com.gizzi.core.domain.auth.dto.OAuth2UserInfo;
import com.gizzi.core.domain.auth.dto.SetPasswordRequestDto;
import com.gizzi.core.domain.auth.dto.UserIdentityResponseDto;
import com.gizzi.core.domain.auth.entity.AuthProviderEntity;
import com.gizzi.core.domain.auth.entity.UserIdentityEntity;
import com.gizzi.core.domain.auth.repository.AuthProviderRepository;
import com.gizzi.core.domain.auth.repository.UserIdentityRepository;
import com.gizzi.core.domain.auth.service.oauth2.OAuth2UserInfoExtractor;
import com.gizzi.core.domain.group.service.GroupService;
import com.gizzi.core.domain.session.entity.SessionEntity;
import com.gizzi.core.domain.session.repository.SessionRepository;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// OAuth2 소셜 로그인 핵심 서비스
// Authorization URL 생성, 콜백 처리(토큰 교환 → 사용자 조회/생성 → JWT 발급)를 담당한다
@Slf4j
@Service
@Transactional(readOnly = true)
public class OAuth2Service {

	// 인증 제공자 리포지토리
	private final AuthProviderRepository   authProviderRepository;

	// 소셜 연동 리포지토리
	private final UserIdentityRepository   userIdentityRepository;

	// 사용자 리포지토리
	private final UserRepository           userRepository;

	// 세션 리포지토리
	private final SessionRepository        sessionRepository;

	// 그룹 서비스 (기본 그룹 배정용)
	private final GroupService             groupService;

	// JWT 토큰 생성 컴포넌트
	private final JwtTokenProvider         jwtTokenProvider;

	// Redis 토큰 세션 관리
	private final RedisTokenService        redisTokenService;

	// Redis (OAuth2 state 저장용)
	private final StringRedisTemplate      redisTemplate;

	// 비밀번호 인코더 (소셜 사용자 랜덤 비밀번호 해싱)
	private final PasswordEncoder          passwordEncoder;

	// HTTP 클라이언트 (토큰 교환 + 사용자 정보 조회)
	private final RestTemplate             restTemplate;

	// Provider별 사용자 정보 파서 Map (providerCode → extractor)
	private final Map<String, OAuth2UserInfoExtractor> extractorMap;

	// OAuth2 state Redis 키 접두사
	private static final String STATE_PREFIX        = "oauth2:state:";

	// OAuth2 state TTL (5분)
	private static final long   STATE_TTL_MINUTES   = 5;

	// OAuth2 연동 대기 Redis 키 접두사
	private static final String LINK_PENDING_PREFIX = "oauth2:link-pending:";

	// 연동 대기 TTL (10분)
	private static final long   LINK_PENDING_TTL_MINUTES = 10;

	// 생성자: Provider별 Extractor를 Map으로 변환
	public OAuth2Service(AuthProviderRepository authProviderRepository,
	                     UserIdentityRepository userIdentityRepository,
	                     UserRepository userRepository,
	                     SessionRepository sessionRepository,
	                     GroupService groupService,
	                     JwtTokenProvider jwtTokenProvider,
	                     RedisTokenService redisTokenService,
	                     StringRedisTemplate redisTemplate,
	                     PasswordEncoder passwordEncoder,
	                     List<OAuth2UserInfoExtractor> extractors) {
		this.authProviderRepository = authProviderRepository;
		this.userIdentityRepository = userIdentityRepository;
		this.userRepository         = userRepository;
		this.sessionRepository      = sessionRepository;
		this.groupService           = groupService;
		this.jwtTokenProvider       = jwtTokenProvider;
		this.redisTokenService      = redisTokenService;
		this.redisTemplate          = redisTemplate;
		this.passwordEncoder        = passwordEncoder;
		// RestTemplate 인스턴스 생성 (빈으로 등록하지 않고 직접 생성)
		this.restTemplate           = new RestTemplate();
		// Extractor 리스트 → Map(providerCode → extractor) 변환
		this.extractorMap = extractors.stream()
			.collect(Collectors.toMap(OAuth2UserInfoExtractor::getProviderCode, e -> e));
	}

	// 활성 소셜 Provider 목록 조회 (로그인 페이지 표시용)
	public List<OAuth2ProviderDto> getEnabledProviders() {
		// 활성화된 Provider 중 local 제외 (소셜만 반환)
		return authProviderRepository.findByIsEnabledTrueOrderByDisplayOrder().stream()
			.filter(p -> !"local".equals(p.getCode()))
			.map(OAuth2ProviderDto::from)
			.toList();
	}

	// Authorization URL 생성 (state를 Redis에 5분 저장)
	public String getAuthorizationUrl(String providerCode) {
		// 1. Provider 조회 및 검증
		AuthProviderEntity provider = getValidProvider(providerCode);

		// 2. CSRF 방지를 위한 랜덤 state 생성
		String state = UUID.randomUUID().toString();

		// 3. Redis에 state 저장 (키: oauth2:state:{state}, 값: providerCode, TTL: 5분)
		String redisKey = STATE_PREFIX + state;
		redisTemplate.opsForValue().set(redisKey, providerCode, STATE_TTL_MINUTES, TimeUnit.MINUTES);

		// 4. Authorization URL 구성
		String authorizationUrl = provider.getAuthorizationUri()
			+ "?response_type=code"
			+ "&client_id=" + provider.getClientId()
			+ "&redirect_uri=" + provider.getRedirectUri()
			+ "&state=" + state;

		// scope가 있으면 추가
		if (provider.getScope() != null && !provider.getScope().isEmpty()) {
			authorizationUrl += "&scope=" + provider.getScope();
		}

		log.info("OAuth2 Authorization URL 생성: provider={}, state={}", providerCode, state);

		return authorizationUrl;
	}

	// OAuth2 콜백 처리: 인가 코드 → 토큰 교환 → 사용자 조회/생성 → JWT 발급
	// 동일 이메일 계정 발견 시 자동 연동하지 않고 LINK_PENDING 결과를 반환한다
	@Transactional
	public OAuth2LoginResultDto processCallback(String providerCode, String code, String state,
	                                            String ipAddress, String userAgent) {
		// 1. state 검증 (Redis에서 확인 후 삭제)
		validateState(state, providerCode);

		// 2. Provider 조회
		AuthProviderEntity provider = getValidProvider(providerCode);

		// 3. 인가 코드로 액세스 토큰 교환
		String providerAccessToken = exchangeCodeForToken(provider, code);

		// 4. 액세스 토큰으로 사용자 정보 조회
		OAuth2UserInfo userInfo = fetchUserInfo(provider, providerAccessToken);

		// 5. 사용자 조회 또는 생성 (연동 확인 분기 포함)
		return findOrCreateUserWithLinkCheck(userInfo, provider, ipAddress, userAgent);
	}

	// Provider 조회 및 유효성 검증 (활성 + 설정 완료 확인)
	private AuthProviderEntity getValidProvider(String providerCode) {
		// 코드로 Provider 조회
		AuthProviderEntity provider = authProviderRepository.findByCode(providerCode)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.PROVIDER_NOT_FOUND));

		// 활성화 여부 확인
		if (!Boolean.TRUE.equals(provider.getIsEnabled())) {
			throw new BusinessException(OAuth2ErrorCode.PROVIDER_DISABLED);
		}

		// Client ID/Secret 설정 여부 확인
		if (provider.getClientId() == null || provider.getClientId().isEmpty()
			|| provider.getClientSecret() == null || provider.getClientSecret().isEmpty()) {
			throw new BusinessException(OAuth2ErrorCode.PROVIDER_NOT_CONFIGURED);
		}

		return provider;
	}

	// OAuth2 state 검증 (Redis에서 확인 후 삭제 — 일회용)
	private void validateState(String state, String providerCode) {
		// Redis에서 state 조회
		String redisKey       = STATE_PREFIX + state;
		String storedProvider = redisTemplate.opsForValue().get(redisKey);

		// state가 존재하지 않거나 Provider 코드가 불일치하면 거부
		if (storedProvider == null || !storedProvider.equals(providerCode)) {
			throw new BusinessException(OAuth2ErrorCode.INVALID_STATE);
		}

		// 사용된 state 즉시 삭제 (재사용 방지)
		redisTemplate.delete(redisKey);
	}

	// 인가 코드로 Provider 액세스 토큰 교환
	private String exchangeCodeForToken(AuthProviderEntity provider, String code) {
		try {
			// POST 요청 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			// 요청 바디 구성 (grant_type, client_id, client_secret, code, redirect_uri)
			MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
			body.add("grant_type", "authorization_code");
			body.add("client_id", provider.getClientId());
			body.add("client_secret", provider.getClientSecret());
			body.add("code", code);
			body.add("redirect_uri", provider.getRedirectUri());

			// 토큰 엔드포인트에 POST 요청
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
				provider.getTokenUri(),
				HttpMethod.POST,
				request,
				new ParameterizedTypeReference<>() {}
			);

			// 응답에서 access_token 추출
			Map<String, Object> responseBody = response.getBody();
			if (responseBody == null || !responseBody.containsKey("access_token")) {
				log.error("토큰 교환 응답에 access_token이 없음: provider={}", provider.getCode());
				throw new BusinessException(OAuth2ErrorCode.TOKEN_EXCHANGE_FAILED);
			}

			String accessToken = (String) responseBody.get("access_token");
			log.info("OAuth2 토큰 교환 성공: provider={}", provider.getCode());

			return accessToken;
		} catch (BusinessException e) {
			// BusinessException은 그대로 전달
			throw e;
		} catch (Exception e) {
			// 기타 예외는 토큰 교환 실패로 래핑
			log.error("OAuth2 토큰 교환 실패: provider={}, error={}", provider.getCode(), e.getMessage());
			throw new BusinessException(OAuth2ErrorCode.TOKEN_EXCHANGE_FAILED);
		}
	}

	// Provider 액세스 토큰으로 사용자 정보 조회
	private OAuth2UserInfo fetchUserInfo(AuthProviderEntity provider, String accessToken) {
		try {
			// Authorization: Bearer 헤더로 사용자 정보 API 호출
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);

			HttpEntity<Void> request = new HttpEntity<>(headers);
			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
				provider.getUserinfoUri(),
				HttpMethod.GET,
				request,
				new ParameterizedTypeReference<>() {}
			);

			Map<String, Object> attributes = response.getBody();
			if (attributes == null) {
				throw new BusinessException(OAuth2ErrorCode.USERINFO_FAILED);
			}

			// Provider별 Extractor로 공통 DTO 변환
			OAuth2UserInfoExtractor extractor = extractorMap.get(provider.getCode());
			if (extractor == null) {
				log.error("지원하지 않는 Provider extractor: {}", provider.getCode());
				throw new BusinessException(OAuth2ErrorCode.PROVIDER_NOT_FOUND);
			}

			OAuth2UserInfo userInfo = extractor.extract(attributes);
			log.info("OAuth2 사용자 정보 조회 성공: provider={}, subject={}", provider.getCode(), userInfo.getProviderSubject());

			return userInfo;
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("OAuth2 사용자 정보 조회 실패: provider={}, error={}", provider.getCode(), e.getMessage());
			throw new BusinessException(OAuth2ErrorCode.USERINFO_FAILED);
		}
	}

	// 소셜 로그인 사용자 조회 또는 생성 (연동 확인 분기 포함)
	// 정책: 1) 기존 연동 → 바로 로그인  2) 동일 이메일 → 연동 대기  3) 신규 → 계정 생성
	private OAuth2LoginResultDto findOrCreateUserWithLinkCheck(OAuth2UserInfo userInfo,
	                                                          AuthProviderEntity provider,
	                                                          String ipAddress, String userAgent) {
		// 1. 기존 소셜 연동이 있는지 확인
		var existingIdentity = userIdentityRepository
			.findByProviderCodeAndProviderSubject(userInfo.getProviderCode(), userInfo.getProviderSubject());

		if (existingIdentity.isPresent()) {
			// 기존 연동된 사용자 → 바로 로그인
			UserEntity user = existingIdentity.get().getUser();
			log.info("기존 소셜 연동 사용자: provider={}, userId={}", userInfo.getProviderCode(), user.getUserId());
			LoginResponseDto loginResponse = createLoginSession(user, userInfo.getProviderCode(), ipAddress, userAgent);
			return OAuth2LoginResultDto.success(loginResponse);
		}

		// 2. 이메일 필수 확인
		if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
			throw new BusinessException(OAuth2ErrorCode.EMAIL_NOT_PROVIDED);
		}

		// 3. 동일 이메일 계정이 있는지 확인
		var existingUser = userRepository.findByEmail(userInfo.getEmail());

		if (existingUser.isPresent()) {
			// 동일 이메일 계정 존재 → 자동 연동하지 않고 연동 대기 상태로 전환
			String pendingId = UUID.randomUUID().toString();

			// Redis에 연동 대기 정보 저장 (파이프 구분자)
			String pendingValue = userInfo.getProviderCode() + "|"
				+ userInfo.getProviderSubject() + "|"
				+ userInfo.getEmail() + "|"
				+ (userInfo.getName() != null ? userInfo.getName() : "");
			String redisKey = LINK_PENDING_PREFIX + pendingId;
			redisTemplate.opsForValue().set(redisKey, pendingValue, LINK_PENDING_TTL_MINUTES, TimeUnit.MINUTES);

			log.info("소셜 연동 대기: email={}, provider={}, pendingId={}", userInfo.getEmail(), userInfo.getProviderCode(), pendingId);

			return OAuth2LoginResultDto.linkPending(pendingId, userInfo.getEmail(), provider.getCode(), provider.getName());
		}

		// 4. 신규 사용자 생성
		String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
		String username = userInfo.getName() != null ? userInfo.getName() : userInfo.getEmail().split("@")[0];

		UserEntity newUser = UserEntity.createSocialUser(
			userInfo.getProviderCode(),
			userInfo.getProviderSubject(),
			userInfo.getEmail(),
			username,
			randomPassword
		);
		userRepository.save(newUser);

		// 기본 그룹(user) 자동 배정
		groupService.assignToDefaultGroup(newUser.getId());

		// 소셜 연동 정보 저장
		UserIdentityEntity identity = UserIdentityEntity.create(newUser, provider, userInfo.getProviderSubject());
		userIdentityRepository.save(identity);

		log.info("소셜 신규 사용자 생성: userId={}, provider={}", newUser.getUserId(), userInfo.getProviderCode());

		LoginResponseDto loginResponse = createLoginSession(newUser, userInfo.getProviderCode(), ipAddress, userAgent);
		return OAuth2LoginResultDto.success(loginResponse);
	}

	// 소셜 연동 확인 처리 (기존 계정에 로컬 ID/PW 검증 후 연동 완료 + JWT 발급)
	@Transactional
	public LoginResponseDto confirmLink(String pendingId, String userId, String password,
	                                    String ipAddress, String userAgent) {
		// 1. Redis에서 연동 대기 정보 조회
		String redisKey     = LINK_PENDING_PREFIX + pendingId;
		String pendingValue = redisTemplate.opsForValue().get(redisKey);

		// 대기 정보가 없으면 만료 또는 잘못된 pendingId
		if (pendingValue == null) {
			throw new BusinessException(OAuth2ErrorCode.LINK_PENDING_NOT_FOUND);
		}

		// 2. 파이프 구분자로 파싱 (providerCode|providerSubject|email|name)
		String[] parts           = pendingValue.split("\\|", 4);
		String   providerCode    = parts[0];
		String   providerSubject = parts[1];

		// 3. 로컬 ID/PW 검증
		UserEntity user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.LINK_CONFIRM_FAILED));

		// 비밀번호 불일치 시 실패
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new BusinessException(OAuth2ErrorCode.LINK_CONFIRM_FAILED);
		}

		// 4. Provider 조회
		AuthProviderEntity provider = authProviderRepository.findByCode(providerCode)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.PROVIDER_NOT_FOUND));

		// 5. 소셜 연동 생성 및 저장
		UserIdentityEntity identity = UserIdentityEntity.create(user, provider, providerSubject);
		userIdentityRepository.save(identity);

		// 6. Redis 대기 정보 삭제 (일회용)
		redisTemplate.delete(redisKey);

		log.info("소셜 연동 확인 완료: userId={}, provider={}", user.getUserId(), providerCode);

		// 7. JWT 발급 + 세션 생성
		return createLoginSession(user, providerCode, ipAddress, userAgent);
	}

	// JWT 발급 + 세션 생성 (기존 AuthService 로직과 동일한 흐름)
	private LoginResponseDto createLoginSession(UserEntity user, String providerCode,
	                                            String ipAddress, String userAgent) {
		// 1. DB 세션 생성
		SessionEntity session = SessionEntity.create(
			user.getId(),
			providerCode.toUpperCase(),
			"pending",
			"pending",
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getAccessTokenExpiration() / 1000),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000),
			ipAddress,
			userAgent
		);
		sessionRepository.save(session);

		// 2. JWT Access/Refresh Token 생성
		String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getUserId(), session.getId());
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), session.getId());

		// 3. 세션 토큰 해시값 업데이트
		session.updateTokens(
			sha256(accessToken),
			sha256(refreshToken),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getAccessTokenExpiration() / 1000),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
		);

		// 4. Redis에 토큰 세션 저장
		redisTokenService.saveAccessToken(user.getId(), session.getId(), jwtTokenProvider.getAccessTokenExpiration());
		redisTokenService.saveRefreshToken(user.getId(), session.getId(), jwtTokenProvider.getRefreshTokenExpiration());

		log.info("소셜 로그인 JWT 발급: userId={}, provider={}, sessionId={}",
			user.getUserId(), providerCode, session.getId());

		// 5. 로그인 응답 반환
		return LoginResponseDto.of(user, accessToken, refreshToken);
	}

	// 현재 사용자의 소셜 연동 목록 조회 (마이페이지용)
	public List<UserIdentityResponseDto> getUserIdentities(String userPk) {
		// 사용자 PK로 소셜 연동 목록 조회 → DTO 변환
		return userIdentityRepository.findByUserId(userPk).stream()
			.map(UserIdentityResponseDto::from)
			.toList();
	}

	// 마이페이지 소셜 연동용 Authorization URL 생성
	// state에 mode=link + userPk를 포함하여 콜백에서 기존 사용자에 연동 추가
	// 정책: 소셜 전용 사용자(provider != LOCAL)는 비밀번호 설정 전까지 연동 추가 불가
	public String getLinkAuthorizationUrl(String providerCode, String userPk) {
		// 0. 사용자 조회 + 비밀번호 설정 여부 체크
		UserEntity user = userRepository.findById(userPk)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.LINK_CONFIRM_FAILED));
		if (!"LOCAL".equals(user.getProvider())) {
			throw new BusinessException(OAuth2ErrorCode.PASSWORD_REQUIRED);
		}

		// 1. Provider 조회 및 검증
		AuthProviderEntity provider = getValidProvider(providerCode);

		// 2. state 생성 (link 모드 + userPk 포함, 파이프 구분)
		String state    = UUID.randomUUID().toString();
		String redisKey = STATE_PREFIX + state;
		// 값 형식: {providerCode}|link|{userPk}
		String value    = providerCode + "|link|" + userPk;
		redisTemplate.opsForValue().set(redisKey, value, STATE_TTL_MINUTES, TimeUnit.MINUTES);

		// 3. Authorization URL 구성
		String authorizationUrl = provider.getAuthorizationUri()
			+ "?response_type=code"
			+ "&client_id=" + provider.getClientId()
			+ "&redirect_uri=" + provider.getRedirectUri()
			+ "&state=" + state;

		// scope가 있으면 추가
		if (provider.getScope() != null && !provider.getScope().isEmpty()) {
			authorizationUrl += "&scope=" + provider.getScope();
		}

		log.info("OAuth2 연동 Authorization URL 생성: provider={}, userPk={}", providerCode, userPk);

		return authorizationUrl;
	}

	// 마이페이지 소셜 연동 콜백 처리 (기존 사용자에 연동 추가, 신규 계정 생성 안 함)
	@Transactional
	public void processLinkCallback(String providerCode, String code, String state) {
		// 1. state 검증 (link 모드 파싱)
		String redisKey    = STATE_PREFIX + state;
		String storedValue = redisTemplate.opsForValue().get(redisKey);

		// state가 존재하지 않으면 거부
		if (storedValue == null) {
			throw new BusinessException(OAuth2ErrorCode.INVALID_STATE);
		}

		// 파이프 구분자로 파싱: {providerCode}|link|{userPk}
		String[] parts = storedValue.split("\\|", 3);
		if (parts.length != 3 || !"link".equals(parts[1]) || !parts[0].equals(providerCode)) {
			throw new BusinessException(OAuth2ErrorCode.INVALID_STATE);
		}

		String userPk = parts[2];

		// state 삭제 (일회용)
		redisTemplate.delete(redisKey);

		// 2. Provider 조회
		AuthProviderEntity provider = getValidProvider(providerCode);

		// 3. 인가 코드로 액세스 토큰 교환
		String providerAccessToken = exchangeCodeForToken(provider, code);

		// 4. 사용자 정보 조회
		OAuth2UserInfo userInfo = fetchUserInfo(provider, providerAccessToken);

		// 5. 이미 다른 계정에 연동되어 있는지 확인
		var existingIdentity = userIdentityRepository
			.findByProviderCodeAndProviderSubject(providerCode, userInfo.getProviderSubject());
		if (existingIdentity.isPresent()) {
			throw new BusinessException(OAuth2ErrorCode.ALREADY_LINKED);
		}

		// 6. 사용자 조회
		UserEntity user = userRepository.findById(userPk)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.LINK_CONFIRM_FAILED));

		// 7. 소셜 연동 생성 및 저장
		UserIdentityEntity identity = UserIdentityEntity.create(user, provider, userInfo.getProviderSubject());
		userIdentityRepository.save(identity);

		log.info("마이페이지 소셜 연동 추가: userId={}, provider={}", user.getUserId(), providerCode);
	}

	// 소셜 연동 해제
	// 정책: 소셜 전용 사용자(provider != LOCAL)는 연동 해제 불가 (비밀번호 설정 필요)
	@Transactional
	public void unlinkProvider(String userPk, String identityId) {
		// 1. 연동 정보 조회
		UserIdentityEntity identity = userIdentityRepository.findById(identityId)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.IDENTITY_NOT_FOUND));

		// 2. 본인 소유 확인
		if (!identity.getUser().getId().equals(userPk)) {
			throw new BusinessException(OAuth2ErrorCode.IDENTITY_NOT_FOUND);
		}

		// 3. 소셜 전용 사용자는 연동 해제 불가 (비밀번호 미설정)
		UserEntity user = identity.getUser();
		if (!"LOCAL".equals(user.getProvider())) {
			throw new BusinessException(OAuth2ErrorCode.PASSWORD_REQUIRED);
		}

		// 4. 연동 삭제
		userIdentityRepository.delete(identity);

		log.info("소셜 연동 해제: userId={}, provider={}", user.getUserId(), identity.getProvider().getCode());
	}

	// OAuth2 state의 모드 확인 (Redis 값을 소비하지 않고 모드만 판별)
	// 반환값: "link" = 마이페이지 연동, "login" = 일반 소셜 로그인, null = state 없음
	public String peekStateMode(String state) {
		// Redis에서 state 값 조회 (삭제하지 않음)
		String redisKey = STATE_PREFIX + state;
		String value    = redisTemplate.opsForValue().get(redisKey);

		// state가 존재하지 않으면 null
		if (value == null) {
			return null;
		}

		// link 모드 여부 판별 (값에 "|link|" 포함)
		return value.contains("|link|") ? "link" : "login";
	}

	// 소셜 전용 사용자에게 로컬 자격증명(ID + 비밀번호) 설정
	// 설정 후 provider가 LOCAL로 변경되어 소셜 연동 추가/해제가 가능해진다
	@Transactional
	public void setPassword(String userPk, SetPasswordRequestDto request) {
		// 1. 사용자 조회
		UserEntity user = userRepository.findById(userPk)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.LINK_CONFIRM_FAILED));

		// 2. 이미 로컬 자격증명이 있으면 거부
		if ("LOCAL".equals(user.getProvider())) {
			throw new BusinessException(OAuth2ErrorCode.ALREADY_LOCAL);
		}

		// 3. 새 로컬 ID 중복 검증
		if (userRepository.existsByUserId(request.getUserId())) {
			throw new BusinessException(UserErrorCode.DUPLICATE_USER_ID);
		}

		// 4. 비밀번호 인코딩 + 로컬 자격증명 설정
		String encodedPassword = passwordEncoder.encode(request.getPassword());
		user.setLocalCredentials(request.getUserId(), encodedPassword);

		log.info("소셜 사용자 로컬 자격증명 설정: userPk={}, newUserId={}", userPk, request.getUserId());
	}

	// 문자열의 SHA-256 해시 생성 (토큰 DB 저장용)
	private String sha256(String input) {
		try {
			// SHA-256 다이제스트 생성
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
		}
	}
}
