package com.gizzi.module.board.dto.board;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 게시판 상세 응답 DTO
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardResponseDto {

	// 게시판 인스턴스 ID
	private final String id;

	// 게시판 이름
	private final String name;

	// URL 슬러그
	private final String slug;

	// 게시판 설명
	private final String description;

	// 소유자 ID
	private final String ownerId;

	// 활성화 여부
	private final Boolean enabled;

	// 게시글 수
	private final Long postCount;

	// 생성 일시
	private final LocalDateTime createdAt;

	// 수정 일시
	private final LocalDateTime updatedAt;
}
