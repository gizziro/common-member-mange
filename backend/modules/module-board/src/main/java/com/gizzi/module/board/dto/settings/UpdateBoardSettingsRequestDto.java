package com.gizzi.module.board.dto.settings;

import lombok.Getter;

// 게시판 설정 수정 요청 DTO (모든 필드 선택, 서비스에서 검증)
@Getter
public class UpdateBoardSettingsRequestDto {

	// 에디터 유형 (MARKDOWN / HTML / TEXT)
	private String editorType;

	// 페이지당 게시글 수
	private Integer postsPerPage;

	// 목록 표시 형식 (LIST / GALLERY / CARD)
	private String displayFormat;

	// 페이지네이션 유형 (NUMBERED / INFINITE_SCROLL / LOAD_MORE)
	private String paginationType;

	// 비로그인 접근 허용 여부
	private Boolean allowAnonymousAccess;

	// 파일 업로드 허용 여부
	private Boolean allowFileUpload;

	// 허용 파일 유형 (쉼표 구분)
	private String allowedFileTypes;

	// 최대 파일 크기 (바이트)
	private Long maxFileSize;

	// 게시글당 최대 첨부 파일 수
	private Integer maxFilesPerPost;

	// 최대 답글 깊이
	private Integer maxReplyDepth;

	// 최대 댓글 깊이
	private Integer maxCommentDepth;

	// 비밀글 허용 여부
	private Boolean allowSecretPosts;

	// 임시 저장 허용 여부
	private Boolean allowDraft;

	// 태그 허용 여부
	private Boolean allowTags;

	// 추천/비추천 허용 여부
	private Boolean allowVote;

	// 카테고리 사용 여부
	private Boolean useCategory;
}
