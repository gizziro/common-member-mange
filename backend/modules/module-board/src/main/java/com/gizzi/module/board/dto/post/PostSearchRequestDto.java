package com.gizzi.module.board.dto.post;

import lombok.Getter;

// 게시글 검색 요청 DTO
@Getter
public class PostSearchRequestDto {

	// 검색 키워드 (선택)
	private String keyword;

	// 카테고리 ID 필터 (선택)
	private String categoryId;

	// 태그 슬러그 필터 (선택)
	private String tagSlug;
}
