package com.gizzi.module.board.dto.vote;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// 투표(추천/비추천) 요청 DTO
@Getter
public class VoteRequestDto {

	// 투표 유형 (UP 또는 DOWN)
	@NotBlank(message = "투표 유형은 필수입니다")
	private String voteType;
}
