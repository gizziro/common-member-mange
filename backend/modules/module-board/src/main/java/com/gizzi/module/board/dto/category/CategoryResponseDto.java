package com.gizzi.module.board.dto.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 카테고리 응답 DTO
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponseDto {

	// 카테고리 PK
	private final String id;

	// 게시판 인스턴스 ID
	private final String boardInstanceId;

	// 카테고리 이름
	private final String name;

	// URL 슬러그
	private final String slug;

	// 카테고리 설명
	private final String description;

	// 정렬 순서
	private final Integer sortOrder;

	// 활성화 여부
	private final Boolean isActive;

	// 생성 일시
	private final LocalDateTime createdAt;

	// 수정 일시
	private final LocalDateTime updatedAt;
}
