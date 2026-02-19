package com.gizzi.core.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

//----------------------------------------------------------------------------------------------------------------------
// 로그인 요청 DTO
//----------------------------------------------------------------------------------------------------------------------
@Getter
@NoArgsConstructor
public class LoginRequestDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	@NotBlank(message = "아이디는 필수입니다")
	private String userId;		// 로그인 아이디

	@NotBlank(message = "비밀번호는 필수입니다")
	private String password;	// 비밀번호
}
