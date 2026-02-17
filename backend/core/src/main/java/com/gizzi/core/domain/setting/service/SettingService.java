package com.gizzi.core.domain.setting.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SettingErrorCode;
import com.gizzi.core.domain.audit.AuditAction;
import com.gizzi.core.domain.audit.AuditTarget;
import com.gizzi.core.domain.audit.service.AuditLogService;
import com.gizzi.core.domain.setting.entity.SettingEntity;
import com.gizzi.core.domain.setting.entity.SettingValueType;
import com.gizzi.core.domain.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 설정 서비스 — 읽기(캐시 우선) + 쓰기(write-through) + 관리 API용 CRUD
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingService {

	// 설정 리포지토리
	private final SettingRepository settingRepository;

	// 인메모리 설정 캐시
	private final SettingCache settingCache;

	// 감사 로그 서비스
	private final AuditLogService auditLogService;

	// ─── 타입별 접근자 (캐시 우선 → DB 폴백) ───

	// 문자열 설정 값 조회 (캐시 미스 시 DB 조회 후 캐시 적재)
	public String getString(String moduleCode, String settingGroup, String settingKey) {
		// 캐시에서 먼저 조회
		Optional<String> cached = settingCache.get(moduleCode, settingGroup, settingKey);
		if (cached.isPresent()) {
			return cached.get();
		}

		// 캐시 미스: DB에서 조회 후 캐시 적재
		SettingEntity entity = settingRepository
				.findByModuleCodeAndSettingGroupAndSettingKey(moduleCode, settingGroup, settingKey)
				.orElseThrow(() -> new BusinessException(SettingErrorCode.SETTING_NOT_FOUND));

		// 캐시에 적재
		settingCache.put(moduleCode, settingGroup, settingKey, entity.getSettingValue());
		return entity.getSettingValue();
	}

	// 불리언 설정 값 조회
	public boolean getBoolean(String moduleCode, String settingGroup, String settingKey) {
		String value = getString(moduleCode, settingGroup, settingKey);
		return "true".equalsIgnoreCase(value);
	}

	// 숫자(long) 설정 값 조회
	public long getNumber(String moduleCode, String settingGroup, String settingKey) {
		String value = getString(moduleCode, settingGroup, settingKey);
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new BusinessException(SettingErrorCode.SETTING_INVALID_VALUE);
		}
	}

	// 시스템 2단 편의 메서드: getString("system", "general", key)
	public String getSystemSetting(String key) {
		return getString("system", "general", key);
	}

	// 시스템 3단 편의 메서드: getString("system", group, key)
	public String getSystemSetting(String group, String key) {
		return getString("system", group, key);
	}

	// 시스템 불리언 편의 메서드
	public boolean getSystemBoolean(String group, String key) {
		return getBoolean("system", group, key);
	}

	// 시스템 숫자 편의 메서드
	public long getSystemNumber(String group, String key) {
		return getNumber("system", group, key);
	}

	// ─── 관리 API용 조회 ───

	// 특정 모듈의 전체 설정 조회 (그룹별 묶음)
	@Transactional(readOnly = true)
	public Map<String, List<SettingEntity>> getSettingsByModule(String moduleCode) {
		// DB에서 모듈 코드로 전체 설정 조회 (정렬됨)
		List<SettingEntity> settings = settingRepository
				.findByModuleCodeOrderBySettingGroupAscSortOrderAscSettingKeyAsc(moduleCode);

		// 그룹별로 묶어서 반환 (LinkedHashMap으로 정렬 순서 유지)
		Map<String, List<SettingEntity>> grouped = new LinkedHashMap<>();
		for (SettingEntity setting : settings) {
			grouped.computeIfAbsent(setting.getSettingGroup(), k -> new java.util.ArrayList<>())
					.add(setting);
		}
		return grouped;
	}

	// 특정 모듈 + 그룹의 설정 목록 조회
	@Transactional(readOnly = true)
	public List<SettingEntity> getSettingsByModuleAndGroup(String moduleCode, String settingGroup) {
		return settingRepository
				.findByModuleCodeAndSettingGroupOrderBySortOrderAscSettingKeyAsc(moduleCode, settingGroup);
	}

	// 설정이 존재하는 모듈 코드 목록 (시스템 제외)
	@Transactional(readOnly = true)
	public List<String> getAllModuleCodes() {
		return settingRepository.findDistinctModuleCodes();
	}

	// ─── 설정 값 변경 ───

	// 그룹 단위 일괄 수정 (관리자 UI에서 그룹별 저장 버튼 클릭 시)
	@Transactional
	public void updateGroupSettings(String moduleCode, String settingGroup,
									Map<String, String> keyValueMap, String updatedBy) {
		// 해당 그룹의 전체 설정 조회
		List<SettingEntity> settings = settingRepository
				.findByModuleCodeAndSettingGroupOrderBySortOrderAscSettingKeyAsc(moduleCode, settingGroup);

		for (SettingEntity setting : settings) {
			// 요청에 포함된 키만 업데이트
			if (!keyValueMap.containsKey(setting.getSettingKey())) {
				continue;
			}

			// 읽기 전용 설정은 스킵 (에러 발생하지 않고 무시)
			if (setting.isReadonly()) {
				log.warn("읽기 전용 설정 변경 시도 무시: {}/{}/{}", moduleCode, settingGroup, setting.getSettingKey());
				continue;
			}

			String newValue = keyValueMap.get(setting.getSettingKey());

			// 값 타입 검증
			validateValue(setting.getValueType(), newValue);

			// DB 업데이트 + 캐시 갱신 (write-through)
			setting.updateValue(newValue, updatedBy);
			settingRepository.save(setting);
			settingCache.put(moduleCode, settingGroup, setting.getSettingKey(), newValue);
		}

		log.info("설정 그룹 수정 완료: {}/{} — {}개 항목 (수정자: {})",
				moduleCode, settingGroup, keyValueMap.size(), updatedBy);

		// 설정 변경 감사 로그
		auditLogService.logSuccess(null, AuditAction.SETTING_CHANGE, AuditTarget.SETTING, null,
			"설정 변경: " + moduleCode + "/" + settingGroup,
			Map.of("moduleCode", moduleCode, "settingGroup", settingGroup, "changedKeys", String.join(",", keyValueMap.keySet())));
	}

	// 값 타입 검증
	private void validateValue(SettingValueType valueType, String value) {
		switch (valueType) {
			case NUMBER -> {
				try {
					Long.parseLong(value);
				} catch (NumberFormatException e) {
					throw new BusinessException(SettingErrorCode.SETTING_INVALID_VALUE);
				}
			}
			case BOOLEAN -> {
				if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
					throw new BusinessException(SettingErrorCode.SETTING_INVALID_VALUE);
				}
			}
			// STRING, JSON은 별도 검증 없음
			default -> { }
		}
	}
}
