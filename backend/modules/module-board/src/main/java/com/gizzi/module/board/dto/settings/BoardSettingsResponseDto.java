package com.gizzi.module.board.dto.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

// 게시판 설정 응답 DTO
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardSettingsResponseDto {

	// 에디터 유형 (MARKDOWN / HTML / TEXT)
	private final String editorType;

	// 페이지당 게시글 수
	private final Integer postsPerPage;

	// 목록 표시 형식 (LIST / GALLERY / CARD)
	private final String displayFormat;

	// 페이지네이션 유형 (NUMBERED / INFINITE_SCROLL / LOAD_MORE)
	private final String paginationType;

	// 비로그인 접근 허용 여부
	private final Boolean allowAnonymousAccess;

	// 파일 업로드 허용 여부
	private final Boolean allowFileUpload;

	// 허용 파일 유형 (쉼표 구분)
	private final String allowedFileTypes;

	// 최대 파일 크기 (바이트)
	private final Long maxFileSize;

	// 게시글당 최대 첨부 파일 수
	private final Integer maxFilesPerPost;

	// 최대 답글 깊이
	private final Integer maxReplyDepth;

	// 최대 댓글 깊이
	private final Integer maxCommentDepth;

	// 비밀글 허용 여부
	private final Boolean allowSecretPosts;

	// 임시 저장 허용 여부
	private final Boolean allowDraft;

	// 태그 허용 여부
	private final Boolean allowTags;

	// 추천/비추천 허용 여부
	private final Boolean allowVote;

	// 카테고리 사용 여부
	private final Boolean useCategory;
}
