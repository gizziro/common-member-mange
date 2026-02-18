package com.gizzi.module.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 게시판 게시글 엔티티 (tb_board_posts 테이블 매핑)
// 게시글의 본문, 작성자, 상태, 통계 정보를 관리한다
@Entity
@Table(name = "tb_board_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPostEntity {

	// 게시글 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 게시판 인스턴스 ID (어떤 게시판에 속하는지)
	@Column(name = "board_instance_id", nullable = false, length = 50)
	private String boardInstanceId;

	// 카테고리 ID (카테고리 미사용 시 null)
	@Column(name = "category_id", length = 36)
	private String categoryId;

	// 부모 게시글 ID (답글인 경우, 원글이면 null)
	@Column(name = "parent_id", length = 36)
	private String parentId;

	// 답글 깊이 (원글: 0, 1차 답글: 1, ...)
	@Column(name = "depth", nullable = false)
	private Integer depth;

	// 게시글 제목
	@Column(name = "title", nullable = false, length = 300)
	private String title;

	// 게시글 본문 (LONGTEXT)
	@Lob
	@Column(name = "content", columnDefinition = "LONGTEXT")
	private String content;

	// 콘텐츠 유형 (PLAIN_TEXT / MARKDOWN)
	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", nullable = false, length = 20)
	private PostContentType contentType;

	// URL 슬러그 (게시판 내 유니크, 선택적 — null 허용)
	@Column(name = "slug", nullable = true, length = 200)
	private String slug;

	// 비밀글 여부
	@Column(name = "is_secret", nullable = false)
	private Boolean isSecret;

	// 공지 여부
	@Column(name = "is_notice", nullable = false)
	private Boolean isNotice;

	// 공지 범위 (BOARD: 게시판 내 / GLOBAL: 전체, 공지가 아닌 경우 null)
	@Enumerated(EnumType.STRING)
	@Column(name = "notice_scope", length = 20)
	private NoticeScope noticeScope;

	// 임시 저장(초안) 여부
	@Column(name = "is_draft", nullable = false)
	private Boolean isDraft;

	// 작성자 사용자 PK (UUID)
	@Column(name = "author_id", nullable = false, length = 36)
	private String authorId;

	// 작성자 표시 이름 (작성 시점 스냅샷)
	@Column(name = "author_name", nullable = false, length = 100)
	private String authorName;

	// 조회수
	@Column(name = "view_count", nullable = false)
	private Integer viewCount;

	// 추천수
	@Column(name = "vote_up_count", nullable = false)
	private Integer voteUpCount;

	// 비추천수
	@Column(name = "vote_down_count", nullable = false)
	private Integer voteDownCount;

	// 댓글 수
	@Column(name = "comment_count", nullable = false)
	private Integer commentCount;

	// SEO 메타 제목 (null이면 게시글 제목 사용)
	@Column(name = "meta_title", length = 200)
	private String metaTitle;

	// SEO 메타 설명 (null이면 본문 앞부분 사용)
	@Column(name = "meta_description", length = 500)
	private String metaDescription;

	// 삭제 여부 (소프트 삭제)
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted;

	// 삭제 일시
	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	// 삭제 처리자 ID
	@Column(name = "deleted_by", length = 36)
	private String deletedBy;

	// 생성 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 수정 일시
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 엔티티 저장 전 UUID PK + 생성/수정 일시 자동 생성
	@PrePersist
	private void prePersist() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
		this.updatedAt = LocalDateTime.now();
	}

	// 게시글 생성 팩토리 메서드
	public static BoardPostEntity create(String boardInstanceId, String categoryId,
	                                      String parentId, int depth,
	                                      String title, String content,
	                                      PostContentType contentType, String slug,
	                                      boolean isSecret, boolean isDraft,
	                                      String authorId, String authorName) {
		BoardPostEntity entity = new BoardPostEntity();
		entity.boardInstanceId = boardInstanceId;
		entity.categoryId      = categoryId;
		entity.parentId        = parentId;
		entity.depth           = depth;
		entity.title           = title;
		entity.content         = content;
		entity.contentType     = contentType;
		entity.slug            = slug;
		entity.isSecret        = isSecret;
		entity.isNotice        = false;
		entity.noticeScope     = null;
		entity.isDraft         = isDraft;
		entity.authorId        = authorId;
		entity.authorName      = authorName;
		entity.viewCount       = 0;
		entity.voteUpCount     = 0;
		entity.voteDownCount   = 0;
		entity.commentCount    = 0;
		entity.metaTitle       = null;
		entity.metaDescription = null;
		entity.isDeleted       = false;
		entity.deletedAt       = null;
		entity.deletedBy       = null;
		return entity;
	}

	// 게시글 내용 수정
	public void updateContent(String title, String content, PostContentType contentType,
	                           String categoryId, String slug,
	                           String metaTitle, String metaDescription) {
		this.title           = title;
		this.content         = content;
		this.contentType     = contentType;
		this.categoryId      = categoryId;
		this.slug            = slug;
		this.metaTitle       = metaTitle;
		this.metaDescription = metaDescription;
		this.updatedAt       = LocalDateTime.now();
	}

	// 게시글 소프트 삭제 처리
	public void markAsDeleted(String deletedBy) {
		this.isDeleted = true;
		this.deletedAt = LocalDateTime.now();
		this.deletedBy = deletedBy;
		this.updatedAt = LocalDateTime.now();
	}

	// 공지 설정/해제 토글 (공지로 설정 시 범위 지정)
	public void toggleNotice(NoticeScope scope) {
		if (this.isNotice) {
			// 공지 해제
			this.isNotice    = false;
			this.noticeScope = null;
		} else {
			// 공지 설정
			this.isNotice    = true;
			this.noticeScope = scope;
		}
		this.updatedAt = LocalDateTime.now();
	}

	// 초안을 정식 게시글로 발행
	public void publishDraft() {
		this.isDraft   = false;
		this.updatedAt = LocalDateTime.now();
	}

	// 조회수 1 증가
	public void incrementViewCount() {
		this.viewCount = this.viewCount + 1;
	}

	// 추천수 1 증가
	public void incrementVoteUpCount() {
		this.voteUpCount = this.voteUpCount + 1;
		this.updatedAt   = LocalDateTime.now();
	}

	// 추천수 1 감소 (0 미만 방지)
	public void decrementVoteUpCount() {
		if (this.voteUpCount > 0) {
			this.voteUpCount = this.voteUpCount - 1;
		}
		this.updatedAt = LocalDateTime.now();
	}

	// 비추천수 1 증가
	public void incrementVoteDownCount() {
		this.voteDownCount = this.voteDownCount + 1;
		this.updatedAt     = LocalDateTime.now();
	}

	// 비추천수 1 감소 (0 미만 방지)
	public void decrementVoteDownCount() {
		if (this.voteDownCount > 0) {
			this.voteDownCount = this.voteDownCount - 1;
		}
		this.updatedAt = LocalDateTime.now();
	}

	// 댓글 수 1 증가
	public void incrementCommentCount() {
		this.commentCount = this.commentCount + 1;
		this.updatedAt    = LocalDateTime.now();
	}

	// 댓글 수 1 감소 (0 미만 방지)
	public void decrementCommentCount() {
		if (this.commentCount > 0) {
			this.commentCount = this.commentCount - 1;
		}
		this.updatedAt = LocalDateTime.now();
	}
}
