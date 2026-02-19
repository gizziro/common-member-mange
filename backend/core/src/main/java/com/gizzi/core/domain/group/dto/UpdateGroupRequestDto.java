package com.gizzi.core.domain.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// 그룹 수정 요청 DTO
// 그룹 코드는 수정 불가이므로 포함하지 않음
@Getter
public class UpdateGroupRequestDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 그룹 표시명 (2~100자)
	@NotBlank(message = "그룹 이름은 필수입니다")
	@Size(min = 2, max = 100, message = "그룹 이름은 2~100자여야 합니다")
	private String name;

	// 그룹 설명 (선택)
	@Size(max = 255, message = "그룹 설명은 255자 이하여야 합니다")
	private String description;
}
