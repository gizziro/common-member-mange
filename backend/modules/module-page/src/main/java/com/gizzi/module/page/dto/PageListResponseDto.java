package com.gizzi.module.page.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 페이지 목록 항목 응답 DTO (본문 제외)
@Getter
@Builder
public class PageListResponseDto {

	// 페이지 PK
	private final String id;

	// URL 슬러그
	private final String slug;

	// 페이지 제목
	private final String title;

	// 콘텐츠 유형
	private final String contentType;

	// 공개 여부
	private final Boolean isPublished;

	// 정렬 순서
	private final Integer sortOrder;

	// 생성 일시
	private final LocalDateTime createdAt;

	// 수정 일시
	private final LocalDateTime updatedAt;
}
