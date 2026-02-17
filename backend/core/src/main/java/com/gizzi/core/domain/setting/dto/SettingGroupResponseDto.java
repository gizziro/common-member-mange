package com.gizzi.core.domain.setting.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 설정 그룹 응답 DTO — 그룹 이름 + 해당 그룹의 설정 목록
@Getter
@Builder
public class SettingGroupResponseDto {

	// 설정 그룹 이름 (예: "general", "signup", "auth")
	private final String group;

	// 그룹에 속한 설정 목록
	private final List<SettingResponseDto> settings;
}
