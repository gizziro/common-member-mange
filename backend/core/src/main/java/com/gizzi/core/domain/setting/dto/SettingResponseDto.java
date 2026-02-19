package com.gizzi.core.domain.setting.dto;

import com.gizzi.core.domain.setting.entity.SettingEntity;
import com.gizzi.core.domain.setting.entity.SettingValueType;
import lombok.Builder;
import lombok.Getter;

// 개별 설정 항목 응답 DTO
@Getter
@Builder
public class SettingResponseDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 설정 키/값 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String           settingKey;		// 설정 키 (예: "site_name", "enabled")
	private final String           settingValue;	// 설정 값 (문자열)
	private final SettingValueType valueType;		// 값 타입 (STRING/NUMBER/BOOLEAN/JSON)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 표시 정보 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String  name;			// 표시명 (관리자 UI)
	private final String  description;	// 설명
	private final boolean readonly;		// 읽기 전용 여부
	private final int     sortOrder;	// 그룹 내 정렬 순서

	//----------------------------------------------------------------------------------------------------------------------
	// 엔티티 → DTO 변환
	//----------------------------------------------------------------------------------------------------------------------
	public static SettingResponseDto from(SettingEntity entity)
	{
		return SettingResponseDto.builder()
				.settingKey(entity.getSettingKey())
				.settingValue(entity.getSettingValue())
				.valueType(entity.getValueType())
				.name(entity.getName())
				.description(entity.getDescription())
				.readonly(entity.isReadonly())
				.sortOrder(entity.getSortOrder())
				.build();
	}
}
