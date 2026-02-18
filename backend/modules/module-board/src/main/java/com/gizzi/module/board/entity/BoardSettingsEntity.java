package com.gizzi.module.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 게시판 설정 엔티티 (tb_board_settings 테이블 매핑)
// 각 게시판 인스턴스별 에디터, 표시, 파일, 댓글 등의 설정을 관리한다
@Entity
@Table(name = "tb_board_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardSettingsEntity {

	// 게시판 인스턴스 ID (PK, 모듈 인스턴스 ID와 동일)
	@Id
	@Column(name = "board_instance_id", length = 50)
	private String boardInstanceId;

	// 에디터 유형 (PLAIN_TEXT / MARKDOWN)
	@Enumerated(EnumType.STRING)
	@Column(name = "editor_type", nullable = false, length = 20)
	private PostContentType editorType;

	// 페이지당 게시글 수
	@Column(name = "posts_per_page", nullable = false)
	private Integer postsPerPage;

	// 게시판 표시 형식 (LIST / GALLERY / CARD)
	@Enumerated(EnumType.STRING)
	@Column(name = "display_format", nullable = false, length = 20)
	private DisplayFormat displayFormat;

	// 페이지네이션 유형 (OFFSET / CURSOR)
	@Enumerated(EnumType.STRING)
	@Column(name = "pagination_type", nullable = false, length = 20)
	private PaginationType paginationType;

	// 비회원(익명) 접근 허용 여부
	@Column(name = "allow_anonymous_access", nullable = false)
	private Boolean allowAnonymousAccess;

	// 파일 업로드 허용 여부
	@Column(name = "allow_file_upload", nullable = false)
	private Boolean allowFileUpload;

	// 허용 파일 확장자 목록 (콤마 구분, 예: "jpg,png,pdf")
	@Column(name = "allowed_file_types", length = 500)
	private String allowedFileTypes;

	// 최대 파일 크기 (바이트 단위, 기본 10MB)
	@Column(name = "max_file_size", nullable = false)
	private Long maxFileSize;

	// 게시글당 최대 첨부 파일 수
	@Column(name = "max_files_per_post", nullable = false)
	private Integer maxFilesPerPost;

	// 최대 답글 깊이 (0이면 답글 불가)
	@Column(name = "max_reply_depth", nullable = false)
	private Integer maxReplyDepth;

	// 최대 댓글 깊이 (0이면 대댓글 불가)
	@Column(name = "max_comment_depth", nullable = false)
	private Integer maxCommentDepth;

	// 비밀글 작성 허용 여부
	@Column(name = "allow_secret_posts", nullable = false)
	private Boolean allowSecretPosts;

	// 임시 저장(초안) 허용 여부
	@Column(name = "allow_draft", nullable = false)
	private Boolean allowDraft;

	// 태그 사용 허용 여부
	@Column(name = "allow_tags", nullable = false)
	private Boolean allowTags;

	// 추천/비추천 투표 허용 여부
	@Column(name = "allow_vote", nullable = false)
	private Boolean allowVote;

	// 카테고리 사용 여부
	@Column(name = "use_category", nullable = false)
	private Boolean useCategory;

	// 생성 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 수정 일시
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 엔티티 저장 전 생성/수정 일시 자동 설정
	@PrePersist
	private void prePersist() {
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
		this.updatedAt = LocalDateTime.now();
	}

	// 게시판 설정 생성 팩토리 메서드 (모든 필드를 기본값으로 초기화)
	public static BoardSettingsEntity create(String instanceId) {
		BoardSettingsEntity entity      = new BoardSettingsEntity();
		entity.boardInstanceId          = instanceId;
		entity.editorType               = PostContentType.MARKDOWN;
		entity.postsPerPage             = 20;
		entity.displayFormat            = DisplayFormat.LIST;
		entity.paginationType           = PaginationType.OFFSET;
		entity.allowAnonymousAccess     = false;
		entity.allowFileUpload          = true;
		entity.allowedFileTypes         = null;
		entity.maxFileSize              = 10485760L;
		entity.maxFilesPerPost          = 5;
		entity.maxReplyDepth            = 0;
		entity.maxCommentDepth          = 2;
		entity.allowSecretPosts         = false;
		entity.allowDraft               = false;
		entity.allowTags                = false;
		entity.allowVote                = true;
		entity.useCategory              = false;
		return entity;
	}

	// 게시판 설정 일괄 수정
	public void updateSettings(PostContentType editorType, Integer postsPerPage,
	                            DisplayFormat displayFormat, PaginationType paginationType,
	                            Boolean allowAnonymousAccess, Boolean allowFileUpload,
	                            String allowedFileTypes, Long maxFileSize, Integer maxFilesPerPost,
	                            Integer maxReplyDepth, Integer maxCommentDepth,
	                            Boolean allowSecretPosts, Boolean allowDraft,
	                            Boolean allowTags, Boolean allowVote, Boolean useCategory) {
		this.editorType           = editorType;
		this.postsPerPage         = postsPerPage;
		this.displayFormat        = displayFormat;
		this.paginationType       = paginationType;
		this.allowAnonymousAccess = allowAnonymousAccess;
		this.allowFileUpload      = allowFileUpload;
		this.allowedFileTypes     = allowedFileTypes;
		this.maxFileSize          = maxFileSize;
		this.maxFilesPerPost      = maxFilesPerPost;
		this.maxReplyDepth        = maxReplyDepth;
		this.maxCommentDepth      = maxCommentDepth;
		this.allowSecretPosts     = allowSecretPosts;
		this.allowDraft           = allowDraft;
		this.allowTags            = allowTags;
		this.allowVote            = allowVote;
		this.useCategory          = useCategory;
		this.updatedAt            = LocalDateTime.now();
	}
}
