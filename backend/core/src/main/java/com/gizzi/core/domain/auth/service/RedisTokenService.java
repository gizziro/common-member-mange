package com.gizzi.core.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

// Redis 기반 토큰 세션 관리 서비스
// Access/Refresh Token의 활성 상태를 Redis에 저장하고 검증한다
// 키 패턴: auth:{type}:{userPk}:{sessionId}
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {

	// Redis 문자열 작업 템플릿
	private final StringRedisTemplate redisTemplate;

	// Access Token 키 접두사
	private static final String ACCESS_PREFIX  = "auth:access:";

	// Refresh Token 키 접두사
	private static final String REFRESH_PREFIX = "auth:refresh:";

	// 토큰 활성 상태 값
	private static final String ACTIVE_VALUE   = "active";

	// Access Token 세션을 Redis에 저장
	public void saveAccessToken(String userPk, String sessionId, long expirationMs) {
		// 키: auth:access:{userPk}:{sessionId}, 값: "active", TTL: 만료시간
		String key = ACCESS_PREFIX + userPk + ":" + sessionId;
		redisTemplate.opsForValue().set(key, ACTIVE_VALUE, expirationMs, TimeUnit.MILLISECONDS);
		log.debug("Access Token 저장: key={}", key);
	}

	// Refresh Token 세션을 Redis에 저장
	public void saveRefreshToken(String userPk, String sessionId, long expirationMs) {
		// 키: auth:refresh:{userPk}:{sessionId}, 값: "active", TTL: 만료시간
		String key = REFRESH_PREFIX + userPk + ":" + sessionId;
		redisTemplate.opsForValue().set(key, ACTIVE_VALUE, expirationMs, TimeUnit.MILLISECONDS);
		log.debug("Refresh Token 저장: key={}", key);
	}

	// Access Token 활성 상태 확인
	public boolean isAccessTokenActive(String userPk, String sessionId) {
		// Redis에 키가 존재하면 활성 상태
		String key = ACCESS_PREFIX + userPk + ":" + sessionId;
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	// Refresh Token 활성 상태 확인
	public boolean isRefreshTokenActive(String userPk, String sessionId) {
		// Redis에 키가 존재하면 활성 상태
		String key = REFRESH_PREFIX + userPk + ":" + sessionId;
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	// 특정 세션의 Access + Refresh Token 삭제 (로그아웃)
	public void deleteTokens(String userPk, String sessionId) {
		// Access Token 키 삭제
		String accessKey  = ACCESS_PREFIX + userPk + ":" + sessionId;
		// Refresh Token 키 삭제
		String refreshKey = REFRESH_PREFIX + userPk + ":" + sessionId;
		redisTemplate.delete(accessKey);
		redisTemplate.delete(refreshKey);
		log.debug("토큰 삭제: userPk={}, sessionId={}", userPk, sessionId);
	}

	// 사용자의 모든 토큰 삭제 (전체 로그아웃 / 보안 사고 대응)
	public void deleteAllUserTokens(String userPk) {
		// SCAN으로 해당 사용자의 모든 Access Token 키 조회 후 삭제
		deleteKeysByPattern(ACCESS_PREFIX + userPk + ":*");
		// SCAN으로 해당 사용자의 모든 Refresh Token 키 조회 후 삭제
		deleteKeysByPattern(REFRESH_PREFIX + userPk + ":*");
		log.info("사용자 전체 토큰 삭제: userPk={}", userPk);
	}

	// 패턴에 매칭되는 Redis 키 일괄 삭제
	private void deleteKeysByPattern(String pattern) {
		// SCAN 명령으로 패턴 매칭 키 조회 (KEYS 대비 프로덕션 안전)
		Set<String> keys = redisTemplate.keys(pattern);
		// 매칭되는 키가 있으면 일괄 삭제
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}
	}
}
