package com.gizzi.core.domain.auth.service;

import com.gizzi.core.common.exception.AuthErrorCode;
import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.UserErrorCode;
import com.gizzi.core.common.security.JwtTokenProvider;
import com.gizzi.core.domain.auth.dto.LoginRequestDto;
import com.gizzi.core.domain.auth.dto.LoginResponseDto;
import com.gizzi.core.domain.auth.dto.TokenRefreshRequestDto;
import com.gizzi.core.domain.auth.dto.TokenRefreshResponseDto;
import com.gizzi.core.domain.auth.dto.UserMeResponseDto;
import com.gizzi.core.domain.session.entity.SessionEntity;
import com.gizzi.core.domain.session.repository.SessionRepository;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import com.gizzi.core.domain.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

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

	// 로그인 처리: 자격증명 검증 → JWT 발급 → Redis 저장 → DB 세션 기록
	@Transactional
	public LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent) {
		// 1. userId로 사용자 조회 (없으면 INVALID_CREDENTIALS)
		UserEntity user = userRepository.findByUserId(request.getUserId())
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

		// 2. 계정 잠금 상태 확인
		if (Boolean.TRUE.equals(user.getIsLocked())) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
		}

		// 3. 비밀번호 검증 (틀리면 실패 횟수 증가 + 잠금 체크)
		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			// 독립 트랜잭션으로 실패 횟수 증가 (외부 트랜잭션 롤백과 무관하게 커밋)
			userService.incrementLoginFailCount(user.getId());
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}

		// 4. 계정 상태 확인 (PENDING 상태이면 접근 차단)
		if ("PENDING".equals(user.getUserStatus())) {
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

		// 10. 로그인 응답 반환
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

		// 10. 새 토큰 쌍 반환
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
