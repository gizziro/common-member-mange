package com.gizzi.core.domain.setting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 개별 설정 변경 요청 DTO (그룹 일괄 수정 내의 단일 항목)
@Getter
@NoArgsConstructor
public class UpdateSettingRequestDto {

	// 설정 키 (예: "site_name", "enabled")
	@NotBlank(message = "설정 키는 필수입니다")
	private String settingKey;

	// 변경할 설정 값
	@NotBlank(message = "설정 값은 필수입니다")
	private String settingValue;
}
