package com.gizzi.core.domain.auth.service;

import com.gizzi.core.common.exception.AuthErrorCode;
import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SmsErrorCode;
import com.gizzi.core.common.exception.UserErrorCode;
import com.gizzi.core.common.security.JwtTokenProvider;
import com.gizzi.core.domain.audit.AuditAction;
import com.gizzi.core.domain.audit.AuditTarget;
import com.gizzi.core.domain.audit.service.AuditLogService;
import com.gizzi.core.domain.auth.dto.LoginRequestDto;
import com.gizzi.core.domain.auth.dto.LoginResponseDto;
import com.gizzi.core.domain.auth.dto.OtpVerifyRequestDto;
import com.gizzi.core.domain.auth.dto.TokenRefreshRequestDto;
import com.gizzi.core.domain.auth.dto.TokenRefreshResponseDto;
import com.gizzi.core.domain.auth.dto.UserMeResponseDto;
import com.gizzi.core.domain.session.entity.SessionEntity;
import com.gizzi.core.domain.session.repository.SessionRepository;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import com.gizzi.core.domain.user.service.UserService;
import com.gizzi.core.domain.setting.service.SettingService;
import com.gizzi.core.domain.sms.service.OtpService;
import com.gizzi.core.domain.sms.service.SmsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// 인증 관련 비즈니스 로직을 처리하는 서비스
// 로그인, 로그아웃, 토큰 갱신, 사용자 정보 조회를 담당한다
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

	// JWT 토큰 생성/파싱 컴포넌트
	private final JwtTokenProvider  jwtTokenProvider;

	// Redis 토큰 세션 관리 서비스
	private final RedisTokenService redisTokenService;

	// 사용자 서비스 (로그인 실패 카운트 등)
	private final UserService       userService;

	// 사용자 리포지토리
	private final UserRepository    userRepository;

	// 세션 리포지토리
	private final SessionRepository sessionRepository;

	// 비밀번호 인코더
	private final PasswordEncoder   passwordEncoder;

	// 시스템 설정 서비스 (잠금 자동 해제 시간 등)
	private final SettingService    settingService;

	// 감사 로그 서비스
	private final AuditLogService   auditLogService;

	// Redis 템플릿 (OTP 세션 관리용)
	private final StringRedisTemplate stringRedisTemplate;

	// OTP 서비스 (SMS 인증)
	private final OtpService        otpService;

	// SMS 발송 서비스 (OTP 발송용)
	private final SmsService        smsService;

	// OTP 세션 Redis 키 접두사
	private static final String OTP_SESSION_PREFIX = "auth:otp-session:";

	// OTP 세션 TTL (5분)
	private static final long   OTP_SESSION_TTL    = 300;

	// 로그인 처리: 자격증명 검증 → JWT 발급 → Redis 저장 → DB 세션 기록
	@Transactional
	public LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent) {
		// 1. userId로 사용자 조회 (없으면 INVALID_CREDENTIALS)
		UserEntity user = userRepository.findByUserId(request.getUserId())
			.orElseGet(() -> {
				// 로그인 실패 감사 로그 — 사용자 없음 (독립 트랜잭션)
				auditLogService.logFailure(null, AuditAction.LOGIN, AuditTarget.USER, null,
					"존재하지 않는 사용자 ID: " + request.getUserId(), null);
				throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
			});

		// 2. 계정 잠금 상태 확인 (자동 해제 시도 포함)
		if (Boolean.TRUE.equals(user.getIsLocked())) {
			// 시스템 설정: 잠금 유지 시간(분) 조회
			int lockDuration = (int) settingService.getSystemNumber("auth", "lock_duration_min");
			// 잠금 시간 경과 시 자동 해제 시도
			if (!user.tryAutoUnlock(lockDuration)) {
				// 로그인 실패 감사 로그 — 계정 잠금 상태 (독립 트랜잭션)
				auditLogService.logFailure(user.getId(), AuditAction.LOGIN, AuditTarget.USER, user.getId(),
					"계정 잠금 상태로 로그인 차단", null);
				// 아직 잠금 유지 중 → 로그인 차단
				throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
			}
			// 자동 해제 성공 → 로그인 계속 진행
			log.info("계정 자동 잠금 해제: userId={}, lockDuration={}분", user.getUserId(), lockDuration);
		}

		// 3. 비밀번호 검증 (틀리면 실패 횟수 증가 + 잠금 체크)
		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			// 로그인 실패 감사 로그 — 비밀번호 불일치 (독립 트랜잭션)
			auditLogService.logFailure(user.getId(), AuditAction.LOGIN, AuditTarget.USER, user.getId(),
				"비밀번호 불일치", null);
			// 독립 트랜잭션으로 실패 횟수 증가 (외부 트랜잭션 롤백과 무관하게 커밋)
			userService.incrementLoginFailCount(user.getId());
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}

		// 4. 계정 상태 확인 (PENDING 상태이면 접근 차단)
		if ("PENDING".equals(user.getUserStatus())) {
			// 로그인 실패 감사 로그 — PENDING 상태 (독립 트랜잭션)
			auditLogService.logFailure(user.getId(), AuditAction.LOGIN, AuditTarget.USER, user.getId(),
				"PENDING 상태 계정으로 로그인 시도", null);
			throw new BusinessException(AuthErrorCode.ACCOUNT_PENDING);
		}

		// 5. 로그인 성공 — 실패 횟수 초기화
		user.resetLoginFailCount();

		// 6. SessionEntity 생성을 위한 임시 해시값 (토큰 생성 후 업데이트)
		SessionEntity session = SessionEntity.create(
			user.getId(),
			user.getProvider(),
			"pending",
			"pending",
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getAccessTokenExpiration() / 1000),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000),
			ipAddress,
			userAgent
		);
		// DB에 세션 저장 (ID 자동 생성)
		sessionRepository.save(session);

		// 7. JWT Access/Refresh Token 생성 (세션 ID를 클레임에 포함)
		String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getUserId(), session.getId());
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), session.getId());

		// 8. 세션의 토큰 해시값 업데이트
		session.updateTokens(
			sha256(accessToken),
			sha256(refreshToken),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getAccessTokenExpiration() / 1000),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
		);

		// 9. Redis에 토큰 세션 저장 (TTL 설정)
		redisTokenService.saveAccessToken(user.getId(), session.getId(), jwtTokenProvider.getAccessTokenExpiration());
		redisTokenService.saveRefreshToken(user.getId(), session.getId(), jwtTokenProvider.getRefreshTokenExpiration());

		log.info("로그인 성공: userId={}, sessionId={}", user.getUserId(), session.getId());

		// 10. 로그인 성공 감사 로그
		auditLogService.logSuccess(user.getId(), AuditAction.LOGIN, AuditTarget.USER, user.getId(),
			"로그인 성공: " + user.getUserId(), Map.of("sessionId", session.getId()));

		// 11. 로그인 응답 반환
		return LoginResponseDto.of(user, accessToken, refreshToken);
	}

	// OTP 필요 여부 판단 후 OTP 세션 생성 + SMS 발송
	// 2FA 설정이 활성화되고 사용자가 OTP 사용 설정이 되어있으며 전화번호가 등록된 경우
	// 반환: OTP 세션이 필요한 경우 LoginResponseDto (requireOtp=true), 불필요하면 null
	public LoginResponseDto createOtpSessionIfNeeded(UserEntity user, boolean otpRequired) {
		// 조건 확인: 시스템 OTP 필수 + 사용자 OTP 사용 + 전화번호 등록
		if (!otpRequired) {
			return null;
		}
		if (!Boolean.TRUE.equals(user.getIsOtpUse())) {
			return null;
		}
		if (user.getPhone() == null || user.getPhone().isBlank()) {
			return null;
		}

		// OTP 세션 ID 생성
		String otpSessionId = UUID.randomUUID().toString();

		// Redis에 OTP 세션 저장 (값: userPk, TTL: 5분)
		stringRedisTemplate.opsForValue().set(
			OTP_SESSION_PREFIX + otpSessionId,
			user.getId(),
			OTP_SESSION_TTL,
			TimeUnit.SECONDS
		);

		// 사용자 전화번호로 OTP 발송
		try {
			otpService.generateAndSend(user.getPhone());
		} catch (Exception e) {
			// OTP 발송 실패 시 세션 삭제 후 에러 전파
			stringRedisTemplate.delete(OTP_SESSION_PREFIX + otpSessionId);
			throw e;
		}

		log.info("OTP 세션 생성: userId={}, otpSessionId={}", user.getUserId(), otpSessionId);

		// OTP 필요 응답 반환 (JWT 미발급)
		return LoginResponseDto.requireOtp(user, otpSessionId);
	}

	// OTP 검증 후 JWT 발급 (관리자 로그인 2FA 완료)
	@Transactional
	public LoginResponseDto verifyOtpAndLogin(OtpVerifyRequestDto request,
	                                          String ipAddress, String userAgent) {
		// 1. Redis에서 OTP 세션 조회
		String userPk = stringRedisTemplate.opsForValue()
			.get(OTP_SESSION_PREFIX + request.getOtpSessionId());

		// 세션이 없으면 만료
		if (userPk == null) {
			throw new BusinessException(SmsErrorCode.SMS_OTP_EXPIRED);
		}

		// 2. 사용자 조회
		UserEntity user = userRepository.findById(userPk)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 3. OTP 코드 검증 (전화번호 기반)
		otpService.verify(user.getPhone(), request.getCode());

		// 4. OTP 세션 삭제 (1회용)
		stringRedisTemplate.delete(OTP_SESSION_PREFIX + request.getOtpSessionId());

		// 5. JWT 발급 (일반 로그인과 동일한 절차)
		SessionEntity session = SessionEntity.create(
			user.getId(),
			user.getProvider(),
			"pending",
			"pending",
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getAccessTokenExpiration() / 1000),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000),
			ipAddress,
			userAgent
		);
		sessionRepository.save(session);

		String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getUserId(), session.getId());
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), session.getId());

		session.updateTokens(
			sha256(accessToken),
			sha256(refreshToken),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getAccessTokenExpiration() / 1000),
			LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
		);

		redisTokenService.saveAccessToken(user.getId(), session.getId(), jwtTokenProvider.getAccessTokenExpiration());
		redisTokenService.saveRefreshToken(user.getId(), session.getId(), jwtTokenProvider.getRefreshTokenExpiration());

		log.info("OTP 검증 후 로그인 성공: userId={}, sessionId={}", user.getUserId(), session.getId());

		// OTP 검증 성공 감사 로그
		auditLogService.logSuccess(user.getId(), AuditAction.LOGIN, AuditTarget.USER, user.getId(),
			"OTP 검증 후 로그인 성공: " + user.getUserId(),
			Map.of("sessionId", session.getId(), "2fa", "true"));

		return LoginResponseDto.of(user, accessToken, refreshToken);
	}

	// 로그아웃 처리: Redis 키 삭제 → DB 세션 폐기
	@Transactional
	public void logout(String accessToken) {
		// 토큰에서 클레임 추출
		Claims claims   = jwtTokenProvider.parseClaims(accessToken);
		String userPk   = jwtTokenProvider.getUserPk(claims);
		String sessionId = jwtTokenProvider.getSessionId(claims);

		// Redis에서 토큰 세션 삭제 (즉시 무효화)
		redisTokenService.deleteTokens(userPk, sessionId);

		// DB 세션 폐기 기록
		sessionRepository.findByIdAndRevokedAtIsNull(sessionId)
			.ifPresent(session -> session.revoke("LOGOUT"));

		log.info("로그아웃 완료: userPk={}, sessionId={}", userPk, sessionId);

		// 로그아웃 감사 로그
		auditLogService.logSuccess(userPk, AuditAction.LOGOUT, AuditTarget.USER, userPk,
			"로그아웃", Map.of("sessionId", sessionId));
	}

	// 토큰 갱신: Refresh Token 검증 → 새 토큰 쌍 발급 (Rotation)
	@Transactional
	public TokenRefreshResponseDto refresh(TokenRefreshRequestDto request) {
		// 1. Refresh Token 파싱 (만료/무효 시 예외)
		Claims claims;
		try {
			claims = jwtTokenProvider.parseClaims(request.getRefreshToken());
		} catch (ExpiredJwtException e) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
		} catch (JwtException e) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		// 2. Refresh Token 타입 확인
		if (!jwtTokenProvider.isRefreshToken(claims)) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		// 3. 클레임에서 정보 추출
		String userPk    = jwtTokenProvider.getUserPk(claims);
		String sessionId = jwtTokenProvider.getSessionId(claims);

		// 4. Redis에서 Refresh Token 활성 상태 확인
		if (!redisTokenService.isRefreshTokenActive(userPk, sessionId)) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		// 5. 사용자 존재 확인
		UserEntity user = userRepository.findById(userPk)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 6. 기존 Redis 키 삭제 (Rotation)
		redisTokenService.deleteTokens(userPk, sessionId);

		// 7. 새 Access/Refresh Token 생성 (동일 세션 ID 유지)
		String newAccessToken  = jwtTokenProvider.generateAccessToken(userPk, user.getUserId(), sessionId);
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(userPk, sessionId);

		// 8. 새 Redis 키 저장
		redisTokenService.saveAccessToken(userPk, sessionId, jwtTokenProvider.getAccessTokenExpiration());
		redisTokenService.saveRefreshToken(userPk, sessionId, jwtTokenProvider.getRefreshTokenExpiration());

		// 9. DB SessionEntity 토큰 해시 업데이트
		sessionRepository.findByIdAndRevokedAtIsNull(sessionId)
			.ifPresent(session -> session.updateTokens(
				sha256(newAccessToken),
				sha256(newRefreshToken),
				LocalDateTime.now().plusSeconds(jwtTokenProvider.getAccessTokenExpiration() / 1000),
				LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
			));

		log.info("토큰 갱신 완료: userPk={}, sessionId={}", userPk, sessionId);

		// 10. 토큰 갱신 감사 로그
		auditLogService.logSuccess(userPk, AuditAction.TOKEN_REFRESH, AuditTarget.USER, userPk,
			"토큰 갱신", Map.of("sessionId", sessionId));

		// 11. 새 토큰 쌍 반환
		return TokenRefreshResponseDto.builder()
			.accessToken(newAccessToken)
			.refreshToken(newRefreshToken)
			.build();
	}

	// 현재 사용자 정보 조회 (grace period + Redis 세션 확인)
	public UserMeResponseDto me(String accessToken) {
		// grace period 적용하여 토큰 파싱
		Claims claims;
		try {
			claims = jwtTokenProvider.parseClaimsWithGrace(accessToken);
		} catch (ExpiredJwtException e) {
			// grace period 초과 시 토큰 만료 에러
			throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);
		} catch (JwtException e) {
			// 파싱 실패 시 무효 토큰 에러
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		// 클레임에서 사용자 PK, 세션 ID 추출
		String userPk    = jwtTokenProvider.getUserPk(claims);
		String sessionId = jwtTokenProvider.getSessionId(claims);

		// Redis에서 Access Token 활성 상태 확인 (로그아웃된 토큰 차단)
		if (!redisTokenService.isAccessTokenActive(userPk, sessionId)) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
		}

		// 사용자 PK로 사용자 조회
		UserEntity user = userRepository.findById(userPk)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 사용자 정보 응답 반환
		return UserMeResponseDto.from(user);
	}

	// Authorization 헤더에서 Bearer 토큰 추출
	public String extractToken(String authHeader) {
		// "Bearer " 접두사 확인 후 토큰 문자열 추출
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}
		return null;
	}

	// 문자열의 SHA-256 해시 생성 (토큰 DB 저장용)
	private String sha256(String input) {
		try {
			// SHA-256 다이제스트 생성
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			// 바이트 배열을 16진수 문자열로 변환
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			// SHA-256은 모든 JVM에서 지원하므로 도달 불가
			throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
		}
	}
}
