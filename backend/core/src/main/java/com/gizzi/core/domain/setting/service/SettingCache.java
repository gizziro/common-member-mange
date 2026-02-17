package com.gizzi.core.domain.setting.service;

import com.gizzi.core.domain.setting.entity.SettingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 설정 인메모리 캐시 — ConcurrentHashMap 기반 + TTL 만료
// 앱 시작 시 전체 설정을 로딩하고, 설정 변경 시 write-through로 동기화
// admin-api와 user-api가 별도 JVM이므로 TTL(30초) 후 DB에서 최신 값을 다시 읽는다
// 캐시 키 형식: "{module_code}::{setting_group}::{setting_key}"
@Slf4j
@Component
public class SettingCache {

	// 캐시 키 구분자
	private static final String SEPARATOR = "::";

	// 캐시 항목 TTL (밀리초, 기본 30초)
	private static final long TTL_MILLIS = 30_000L;

	// 캐시 저장소 (키 → 캐시 항목)
	private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

	// 캐시 항목 — 값 + 만료 시각
	private record CacheEntry(String value, long expiresAt) {
		// 만료 여부 확인
		boolean isExpired() {
			return System.currentTimeMillis() > expiresAt;
		}
	}

	// 캐시 키 생성
	private String buildKey(String moduleCode, String settingGroup, String settingKey) {
		return moduleCode + SEPARATOR + settingGroup + SEPARATOR + settingKey;
	}

	// 캐시에서 설정 값 조회 (TTL 만료 시 Optional.empty 반환 → DB 폴백 유도)
	public Optional<String> get(String moduleCode, String settingGroup, String settingKey) {
		CacheEntry entry = cache.get(buildKey(moduleCode, settingGroup, settingKey));
		// 항목이 없거나 TTL 만료 시 캐시 미스 처리
		if (entry == null || entry.isExpired()) {
			return Optional.empty();
		}
		return Optional.of(entry.value());
	}

	// 캐시에 설정 값 저장 (또는 갱신, TTL 리셋)
	public void put(String moduleCode, String settingGroup, String settingKey, String value) {
		cache.put(buildKey(moduleCode, settingGroup, settingKey),
				new CacheEntry(value, System.currentTimeMillis() + TTL_MILLIS));
	}

	// 캐시에서 설정 값 제거
	public void remove(String moduleCode, String settingGroup, String settingKey) {
		cache.remove(buildKey(moduleCode, settingGroup, settingKey));
	}

	// 전체 설정 목록으로 캐시 일괄 로딩 (앱 시작 시 호출)
	public void loadAll(List<SettingEntity> settings) {
		cache.clear();
		long expiresAt = System.currentTimeMillis() + TTL_MILLIS;
		for (SettingEntity setting : settings) {
			cache.put(
					buildKey(setting.getModuleCode(), setting.getSettingGroup(), setting.getSettingKey()),
					new CacheEntry(setting.getSettingValue(), expiresAt)
			);
		}
		log.info("설정 캐시 로딩 완료: {}개 항목", cache.size());
	}

	// 특정 모듈의 모든 설정 조회 (키 접두사 매칭, 만료된 항목 포함)
	public Map<String, String> getAllByModule(String moduleCode) {
		String prefix = moduleCode + SEPARATOR;
		return cache.entrySet().stream()
				.filter(entry -> entry.getKey().startsWith(prefix))
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().value()));
	}

	// 현재 캐시 크기 조회
	public int size() {
		return cache.size();
	}
}
