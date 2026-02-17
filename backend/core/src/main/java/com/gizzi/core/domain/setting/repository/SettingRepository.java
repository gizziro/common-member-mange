package com.gizzi.core.domain.setting.repository;

import com.gizzi.core.domain.setting.entity.SettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// 설정 리포지토리 — tb_settings 테이블 접근
public interface SettingRepository extends JpaRepository<SettingEntity, String> {

	// 모듈 코드로 전체 설정 조회 (그룹 정렬 → 정렬 순서 → 키 순)
	List<SettingEntity> findByModuleCodeOrderBySettingGroupAscSortOrderAscSettingKeyAsc(String moduleCode);

	// 모듈 코드 + 설정 그룹으로 설정 목록 조회
	List<SettingEntity> findByModuleCodeAndSettingGroupOrderBySortOrderAscSettingKeyAsc(String moduleCode, String settingGroup);

	// 모듈 코드 + 설정 그룹 + 설정 키로 단건 조회
	Optional<SettingEntity> findByModuleCodeAndSettingGroupAndSettingKey(String moduleCode, String settingGroup, String settingKey);

	// 복합 유니크 존재 여부 확인 (SettingsRegistry에서 중복 방지)
	boolean existsByModuleCodeAndSettingGroupAndSettingKey(String moduleCode, String settingGroup, String settingKey);

	// 설정이 존재하는 모듈 코드 목록 (시스템 제외)
	@Query("SELECT DISTINCT s.moduleCode FROM SettingEntity s WHERE s.moduleCode <> 'system' ORDER BY s.moduleCode")
	List<String> findDistinctModuleCodes();
}
