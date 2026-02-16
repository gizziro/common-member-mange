package com.gizzi.core.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// 소셜 전용 사용자의 로컬 자격증명 설정 요청 DTO
// 로컬 ID + 비밀번호를 설정하여 소셜 연동 추가/해제가 가능해진다
@Getter
public class SetPasswordRequestDto {

	// 새 로컬 로그인 ID
	@NotBlank(message = "아이디는 필수입니다")
	@Size(min = 4, max = 50, message = "아이디는 4~50자여야 합니다")
	private String userId;

	// 비밀번호
	@NotBlank(message = "비밀번호는 필수입니다")
	@Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다")
	private String password;
}
