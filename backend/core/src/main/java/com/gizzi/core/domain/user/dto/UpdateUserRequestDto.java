package com.gizzi.core.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 사용자 정보 수정 요청 DTO
// 관리자가 수정 가능한 필드만 포함
@Getter
@NoArgsConstructor
public class UpdateUserRequestDto
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 사용자 이름 (2~100자)
	@NotBlank(message = "사용자 이름은 필수입니다")
	@Size(min = 2, max = 100, message = "사용자 이름은 2~100자여야 합니다")
	private String username;

	// 이메일 주소
	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "올바른 이메일 형식이 아닙니다")
	private String email;

	// 사용자 상태 (PENDING, ACTIVE, SUSPENDED)
	@NotBlank(message = "사용자 상태는 필수입니다")
	private String userStatus;

	// SMS 수신 동의 여부
	private Boolean isSmsAgree;
}
