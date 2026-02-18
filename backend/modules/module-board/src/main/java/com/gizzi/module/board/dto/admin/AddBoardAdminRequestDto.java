package com.gizzi.module.board.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// 게시판 관리자 추가 요청 DTO
@Getter
public class AddBoardAdminRequestDto {

	// 관리자로 추가할 사용자 ID
	@NotBlank(message = "사용자 ID는 필수입니다")
	private String userId;
}
