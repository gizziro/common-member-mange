package com.gizzi.module.board.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// 댓글 수정 요청 DTO
@Getter
public class UpdateCommentRequestDto {

	// 댓글 내용
	@NotBlank(message = "댓글 내용은 필수입니다")
	@Size(max = 5000, message = "댓글은 5000자 이내여야 합니다")
	private String content;
}
