package com.gizzi.core.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로컬 회원가입 요청 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

	// 로그인 ID (4~50자)
	@NotBlank(message = "아이디는 필수입니다")
	@Size(min = 4, max = 50, message = "아이디는 4~50자여야 합니다")
	private String userId;

	// 비밀번호 (8~100자)
	@NotBlank(message = "비밀번호는 필수입니다")
	@Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다")
	private String password;

	// 사용자 이름 (2~100자)
	@NotBlank(message = "이름은 필수입니다")
	@Size(min = 2, max = 100, message = "이름은 2~100자여야 합니다")
	private String username;

	// 이메일 주소
	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "올바른 이메일 형식이 아닙니다")
	private String email;
}
