package com.gizzi.module.board.dto.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 댓글 응답 DTO (트리 구조 지원)
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponseDto {

	// 댓글 PK
	private final String id;

	// 게시글 ID
	private final String postId;

	// 부모 댓글 ID (대댓글인 경우)
	private final String parentId;

	// 댓글 깊이
	private final Integer depth;

	// 댓글 내용
	private final String content;

	// 작성자 ID
	private final String authorId;

	// 작성자 이름
	private final String authorName;

	// 추천 수
	private final Integer voteUpCount;

	// 비추천 수
	private final Integer voteDownCount;

	// 삭제 여부 (삭제된 댓글은 내용 숨김, 구조 유지)
	private final Boolean isDeleted;

	// 하위 댓글 목록
	private final List<CommentResponseDto> children;

	// 생성 일시
	private final LocalDateTime createdAt;

	// 수정 일시
	private final LocalDateTime updatedAt;
}
