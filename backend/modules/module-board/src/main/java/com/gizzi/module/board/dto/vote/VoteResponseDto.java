package com.gizzi.module.board.dto.vote;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

// 투표(추천/비추천) 응답 DTO
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoteResponseDto {

	// 추천 수
	private final Integer voteUpCount;

	// 비추천 수
	private final Integer voteDownCount;

	// 현재 사용자의 투표 유형 (UP / DOWN / null — 투표하지 않은 경우)
	private final String userVoteType;
}
