package com.gizzi.core.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 비밀번호 변경 요청 DTO
// 관리자가 특정 사용자의 비밀번호를 강제 변경할 때 사용
@Getter
@NoArgsConstructor
public class ChangePasswordRequestDto {

	// 새 비밀번호 (8~100자)
	@NotBlank(message = "새 비밀번호는 필수입니다")
	@Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다")
	private String newPassword;
}
