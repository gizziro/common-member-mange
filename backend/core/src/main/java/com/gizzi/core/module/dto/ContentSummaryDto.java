package com.gizzi.core.module.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

// 모듈 간 콘텐츠 참조 시 사용하는 요약 정보 DTO
// ModuleContentProvider를 통해 다른 모듈의 콘텐츠를 조회할 때 반환된다
//
// 사용 예:
//   가계부 모듈에서 게시글을 참조할 때
//   moduleContentRegistry.getContent("board", "post", "post-uuid-123")
//   → ContentSummaryDto { title: "공지사항", url: "/board/notice/posts/123", ... }
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentSummaryDto {

	// 모듈 코드 (예: "board")
	private final String moduleCode;

	// 리소스 유형 (예: "post", "comment")
	private final String resourceType;

	// 리소스 PK (예: "post-uuid-123")
	private final String resourceId;

	// 콘텐츠 제목 (예: "공지사항")
	private final String title;

	// 콘텐츠 접근 URL (예: "/board/notice/posts/123")
	private final String url;

	// 콘텐츠 요약 (본문 미리보기 등, 선택)
	private final String summary;
}
