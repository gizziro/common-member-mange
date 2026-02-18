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
import java.util.UUID;

// 게시판 투표 엔티티 (tb_board_votes 테이블 매핑)
// 게시글/댓글에 대한 추천/비추천 투표를 관리한다
@Entity
@Table(name = "tb_board_votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardVoteEntity {

	// 투표 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 투표 대상 유형 (POST / COMMENT)
	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 20)
	private VoteTargetType targetType;

	// 투표 대상 ID (게시글 또는 댓글의 UUID)
	@Column(name = "target_id", nullable = false, length = 36)
	private String targetId;

	// 투표한 사용자 ID (FK)
	@Column(name = "user_id", nullable = false, length = 36)
	private String userId;

	// 투표 유형 (UP / DOWN)
	@Enumerated(EnumType.STRING)
	@Column(name = "vote_type", nullable = false, length = 10)
	private VoteType voteType;

	// 생성 일시 (수정 일시 없음)
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 엔티티 저장 전 UUID PK + 생성 일시 자동 생성
	@PrePersist
	private void prePersist() {
		// PK가 없으면 UUID 자동 생성
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
		// 생성 일시 자동 설정
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}

	// 투표 생성 팩토리 메서드
	public static BoardVoteEntity create(VoteTargetType targetType, String targetId,
	                                     String userId, VoteType voteType) {
		BoardVoteEntity entity = new BoardVoteEntity();
		entity.targetType      = targetType;
		entity.targetId        = targetId;
		entity.userId          = userId;
		entity.voteType        = voteType;
		return entity;
	}
}
