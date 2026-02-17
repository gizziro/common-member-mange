package com.gizzi.module.page.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 페이지 상세 응답 DTO
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponseDto {

	// 페이지 PK
	private final String id;

	// URL 슬러그
	private final String slug;

	// 페이지 제목
	private final String title;

	// 페이지 본문
	private final String content;

	// 콘텐츠 유형 (HTML / MARKDOWN / TEXT)
	private final String contentType;

	// 공개 여부
	private final Boolean isPublished;

	// 정렬 순서
	private final Integer sortOrder;

	// 연결된 모듈 인스턴스 ID (권한 관리용)
	private final String moduleInstanceId;

	// 생성자
	private final String createdBy;

	// 생성 일시
	private final LocalDateTime createdAt;

	// 수정자
	private final String updatedBy;

	// 수정 일시
	private final LocalDateTime updatedAt;
}
