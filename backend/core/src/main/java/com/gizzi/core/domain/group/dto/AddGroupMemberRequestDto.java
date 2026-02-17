package com.gizzi.core.domain.group.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// 그룹 멤버 추가 요청 DTO
@Getter
public class AddGroupMemberRequestDto {

	// 추가할 사용자 로그인 ID
	@NotBlank(message = "사용자 ID는 필수입니다")
	private String userId;
}
