package com.gizzi.module.board.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 게시글 목록 응답 DTO (경량 버전)
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostListResponseDto {

	// 게시글 PK
	private final String id;

	// 게시판 인스턴스 ID
	private final String boardInstanceId;

	// 카테고리 ID
	private final String categoryId;

	// 카테고리 이름
	private final String categoryName;

	// 게시글 제목
	private final String title;

	// 콘텐츠 유형 (MARKDOWN / HTML / TEXT)
	private final String contentType;

	// URL 슬러그
	private final String slug;

	// 비밀글 여부
	private final Boolean isSecret;

	// 공지 여부
	private final Boolean isNotice;

	// 공지 범위 (BOARD / GLOBAL)
	private final String noticeScope;

	// 임시 저장 여부
	private final Boolean isDraft;

	// 작성자 ID
	private final String authorId;

	// 작성자 이름
	private final String authorName;

	// 조회수
	private final Long viewCount;

	// 추천 수
	private final Integer voteUpCount;

	// 비추천 수
	private final Integer voteDownCount;

	// 댓글 수
	private final Integer commentCount;

	// 첨부 파일 존재 여부
	private final Boolean hasFiles;

	// 생성 일시
	private final LocalDateTime createdAt;

	// 수정 일시
	private final LocalDateTime updatedAt;
}
