package com.gizzi.core.domain.setting.service;

import com.gizzi.core.domain.setting.entity.SettingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 설정 인메모리 캐시 — ConcurrentHashMap 기반
// 앱 시작 시 전체 설정을 로딩하고, 설정 변경 시 write-through로 동기화
// 캐시 키 형식: "{module_code}::{setting_group}::{setting_key}"
@Slf4j
@Component
public class SettingCache {

	// 캐시 키 구분자
	private static final String SEPARATOR = "::";

	// 캐시 저장소 (키 → 설정 값)
	private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

	// 캐시 키 생성
	private String buildKey(String moduleCode, String settingGroup, String settingKey) {
		return moduleCode + SEPARATOR + settingGroup + SEPARATOR + settingKey;
	}

	// 캐시에서 설정 값 조회
	public Optional<String> get(String moduleCode, String settingGroup, String settingKey) {
		String value = cache.get(buildKey(moduleCode, settingGroup, settingKey));
		return Optional.ofNullable(value);
	}

	// 캐시에 설정 값 저장 (또는 갱신)
	public void put(String moduleCode, String settingGroup, String settingKey, String value) {
		cache.put(buildKey(moduleCode, settingGroup, settingKey), value);
	}

	// 캐시에서 설정 값 제거
	public void remove(String moduleCode, String settingGroup, String settingKey) {
		cache.remove(buildKey(moduleCode, settingGroup, settingKey));
	}

	// 전체 설정 목록으로 캐시 일괄 로딩 (앱 시작 시 호출)
	public void loadAll(List<SettingEntity> settings) {
		cache.clear();
		for (SettingEntity setting : settings) {
			cache.put(
					buildKey(setting.getModuleCode(), setting.getSettingGroup(), setting.getSettingKey()),
					setting.getSettingValue()
			);
		}
		log.info("설정 캐시 로딩 완료: {}개 항목", cache.size());
	}

	// 특정 모듈의 모든 설정 조회 (키 접두사 매칭)
	public Map<String, String> getAllByModule(String moduleCode) {
		String prefix = moduleCode + SEPARATOR;
		return cache.entrySet().stream()
				.filter(entry -> entry.getKey().startsWith(prefix))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	// 현재 캐시 크기 조회
	public int size() {
		return cache.size();
	}
}
