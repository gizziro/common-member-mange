package com.gizzi.core.common.security;

import com.gizzi.core.common.config.JwtProperties;
import com.gizzi.core.domain.setting.service.SettingService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

// JWT 토큰 생성, 파싱, 검증을 담당하는 컴포넌트
// Access Token과 Refresh Token의 생성 및 클레임 추출을 처리한다
@Slf4j
@Component
public class JwtTokenProvider {

	// JWT 서명에 사용할 비밀키
	private final SecretKey     secretKey;

	// JWT 설정 프로퍼티 (기본값 역할)
	private final JwtProperties jwtProperties;

	// 시스템 설정 서비스 (DB 설정 우선, 실패 시 yml 폴백)
	private final SettingService settingService;

	// 생성자: 프로퍼티에서 비밀키 문자열을 SecretKey 객체로 변환
	public JwtTokenProvider(JwtProperties jwtProperties, SettingService settingService) {
		this.jwtProperties = jwtProperties;
		this.settingService = settingService;
		// 비밀키 문자열을 HMAC-SHA 키로 변환 (최소 256bit 필요)
		this.secretKey = Keys.hmacShaKeyFor(
			jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)
		);
	}

	// Access Token 생성
	// userPk: 사용자 PK (UUID), userId: 로그인 ID, sessionId: 세션 PK (UUID)
	public String generateAccessToken(String userPk, String userId, String sessionId) {
		// 현재 시각 기준 만료 시각 계산
		Instant now    = Instant.now();
		Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpiration());

		// Access Token JWT 빌드 (iss: 발급자, sub: userPk, uid: userId, sid: sessionId)
		return Jwts.builder()
			.issuer(jwtProperties.getIssuer())
			.subject(userPk)
			.claim("uid", userId)
			.claim("sid", sessionId)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiry))
			.signWith(secretKey)
			.compact();
	}

	// Refresh Token 생성
	// userPk: 사용자 PK (UUID), sessionId: 세션 PK (UUID)
	public String generateRefreshToken(String userPk, String sessionId) {
		// 현재 시각 기준 만료 시각 계산
		Instant now    = Instant.now();
		Instant expiry = now.plusMillis(jwtProperties.getRefreshTokenExpiration());

		// Refresh Token JWT 빌드 (iss: 발급자, type: refresh 클레임으로 구분)
		return Jwts.builder()
			.issuer(jwtProperties.getIssuer())
			.subject(userPk)
			.claim("sid", sessionId)
			.claim("type", "refresh")
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiry))
			.signWith(secretKey)
			.compact();
	}

	// 토큰에서 클레임 파싱 (엄격 검증: 서명 + issuer + 만료 검증)
	public Claims parseClaims(String token) {
		// 서명 검증 + issuer 일치 확인 + 만료 체크 후 클레임 반환
		return Jwts.parser()
			.verifyWith(secretKey)
			.requireIssuer(jwtProperties.getIssuer())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	// 토큰에서 클레임 파싱 (grace period 허용: /auth/me 전용)
	// 네트워크 지연을 고려하여 만료 후 일정 시간 내에는 유효하게 처리한다
	public Claims parseClaimsWithGrace(String token) {
		try {
			// 먼저 엄격 파싱 시도
			return parseClaims(token);
		} catch (ExpiredJwtException e) {
			// 만료된 토큰이라도 issuer가 다르면 거부 (교차 사용 방지)
			String tokenIssuer = e.getClaims().getIssuer();
			if (!jwtProperties.getIssuer().equals(tokenIssuer)) {
				log.debug("만료 토큰 issuer 불일치: expected={}, actual={}",
					jwtProperties.getIssuer(), tokenIssuer);
				throw e;
			}

			// 만료된 토큰의 만료 시각 추출
			Date expiration = e.getClaims().getExpiration();
			// 현재 시각과 만료 시각 사이의 차이 계산 (초 단위)
			long elapsedSeconds = (Instant.now().toEpochMilli() - expiration.getTime()) / 1000;

			// grace period 이내이면 만료된 토큰의 클레임을 반환
			if (elapsedSeconds <= jwtProperties.getGracePeriodSeconds()) {
				log.debug("토큰 grace period 허용: 만료 후 {}초 경과 (허용: {}초)",
					elapsedSeconds, jwtProperties.getGracePeriodSeconds());
				return e.getClaims();
			}
			// grace period 초과 시 원래 예외를 다시 던짐
			throw e;
		}
	}

	// 클레임에서 사용자 PK (sub) 추출
	public String getUserPk(Claims claims) {
		return claims.getSubject();
	}

	// 클레임에서 세션 ID (sid) 추출
	public String getSessionId(Claims claims) {
		return claims.get("sid", String.class);
	}

	// 클레임에서 로그인 ID (uid) 추출
	public String getUserId(Claims claims) {
		return claims.get("uid", String.class);
	}

	// 토큰이 Refresh Token인지 확인
	public boolean isRefreshToken(Claims claims) {
		// type 클레임이 "refresh"인 경우 Refresh Token으로 판단
		return "refresh".equals(claims.get("type", String.class));
	}

	// Access Token 만료 시간 (ms) 조회
	// DB 설정(session.access_token_exp) 우선, 실패 시 yml 설정 폴백
	public long getAccessTokenExpiration() {
		try {
			return settingService.getSystemNumber("session", "access_token_exp");
		} catch (Exception e) {
			// DB 설정 조회 실패 시 yml 기본값 사용
			log.debug("Access Token 만료 시간 DB 설정 조회 실패, yml 폴백: {}", e.getMessage());
			return jwtProperties.getAccessTokenExpiration();
		}
	}

	// Refresh Token 만료 시간 (ms) 조회
	// DB 설정(session.refresh_token_exp) 우선, 실패 시 yml 설정 폴백
	public long getRefreshTokenExpiration() {
		try {
			return settingService.getSystemNumber("session", "refresh_token_exp");
		} catch (Exception e) {
			// DB 설정 조회 실패 시 yml 기본값 사용
			log.debug("Refresh Token 만료 시간 DB 설정 조회 실패, yml 폴백: {}", e.getMessage());
			return jwtProperties.getRefreshTokenExpiration();
		}
	}
}
