package com.gizzi.core.domain.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// 그룹 생성 요청 DTO
@Getter
public class CreateGroupRequestDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 그룹 코드 (영소문자+숫자+하이픈, 2~50자)
	@NotBlank(message = "그룹 코드는 필수입니다")
	@Size(min = 2, max = 50, message = "그룹 코드는 2~50자여야 합니다")
	@Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "그룹 코드는 영소문자, 숫자, 하이픈만 사용할 수 있습니다")
	private String groupCode;

	// 그룹 표시명 (2~100자)
	@NotBlank(message = "그룹 이름은 필수입니다")
	@Size(min = 2, max = 100, message = "그룹 이름은 2~100자여야 합니다")
	private String name;

	// 그룹 설명 (선택)
	@Size(max = 255, message = "그룹 설명은 255자 이하여야 합니다")
	private String description;
}
