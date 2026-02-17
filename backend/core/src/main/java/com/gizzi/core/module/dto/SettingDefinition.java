package com.gizzi.core.module.dto;

import com.gizzi.core.domain.setting.entity.SettingValueType;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 모듈 기본 설정 정의 — ModuleDefinition.getDefaultSettings()에서 사용
// SettingsRegistry가 앱 시작 시 이 정의를 기반으로 tb_settings에 기본 설정을 등록한다
// 기존 값이 있으면 스킵하여 관리자가 수정한 값을 보존한다
@Getter
@AllArgsConstructor
public class SettingDefinition {

	// 설정 그룹 (예: "general", "display")
	private final String group;

	// 설정 키 (예: "default_content_type", "max_pages")
	private final String key;

	// 기본 값 (문자열)
	private final String defaultValue;

	// 값 타입 (STRING/NUMBER/BOOLEAN/JSON)
	private final SettingValueType valueType;

	// 표시명 (관리자 UI에 표시)
	private final String name;

	// 설명 (관리자 UI 부가 정보)
	private final String description;

	// 그룹 내 정렬 순서
	private final int sortOrder;
}
