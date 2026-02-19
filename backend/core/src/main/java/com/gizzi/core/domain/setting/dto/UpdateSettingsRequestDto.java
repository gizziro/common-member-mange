package com.gizzi.core.domain.setting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 그룹 단위 설정 일괄 수정 요청 DTO
// 관리자 UI에서 그룹별 "저장" 버튼 클릭 시 전송
@Getter
@NoArgsConstructor
public class UpdateSettingsRequestDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 설정 항목 목록 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Valid
	@NotEmpty(message = "변경할 설정 항목이 필요합니다")
	private List<UpdateSettingRequestDto> settings;	// 변경할 설정 항목 목록
}
