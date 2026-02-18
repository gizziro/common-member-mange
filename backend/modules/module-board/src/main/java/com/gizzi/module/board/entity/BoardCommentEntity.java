package com.gizzi.module.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 게시판 댓글 엔티티 (tb_board_comments 테이블 매핑)
// 게시글에 달린 댓글과 대댓글을 관리한다
@Entity
@Table(name = "tb_board_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardCommentEntity {

	// 댓글 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 게시글 ID (어떤 게시글에 속하는지)
	@Column(name = "post_id", nullable = false, length = 36)
	private String postId;

	// 부모 댓글 ID (대댓글인 경우, 최상위 댓글이면 null)
	@Column(name = "parent_id", length = 36)
	private String parentId;

	// 대댓글 깊이 (최상위 댓글: 0, 1차 대댓글: 1, ...)
	@Column(name = "depth", nullable = false)
	private Integer depth;

	// 댓글 내용 (TEXT)
	@Lob
	@Column(name = "content", columnDefinition = "TEXT", nullable = false)
	private String content;

	// 작성자 사용자 PK (UUID)
	@Column(name = "author_id", nullable = false, length = 36)
	private String authorId;

	// 작성자 표시 이름 (작성 시점 스냅샷)
	@Column(name = "author_name", nullable = false, length = 100)
	private String authorName;

	// 추천수
	@Column(name = "vote_up_count", nullable = false)
	private Integer voteUpCount;

	// 비추천수
	@Column(name = "vote_down_count", nullable = false)
	private Integer voteDownCount;

	// 삭제 여부 (소프트 삭제)
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted;

	// 삭제 일시
	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

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

	// 댓글 생성 팩토리 메서드
	public static BoardCommentEntity create(String postId, String parentId, int depth,
	                                         String content, String authorId, String authorName) {
		BoardCommentEntity entity = new BoardCommentEntity();
		entity.postId             = postId;
		entity.parentId           = parentId;
		entity.depth              = depth;
		entity.content            = content;
		entity.authorId           = authorId;
		entity.authorName         = authorName;
		entity.voteUpCount        = 0;
		entity.voteDownCount      = 0;
		entity.isDeleted          = false;
		entity.deletedAt          = null;
		return entity;
	}

	// 댓글 내용 수정
	public void updateContent(String content) {
		this.content   = content;
		this.updatedAt = LocalDateTime.now();
	}

	// 댓글 소프트 삭제 처리
	public void markAsDeleted() {
		this.isDeleted = true;
		this.deletedAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
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
}
