package com.gizzi.module.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 댓글 Closure Table 엔티티 (tb_board_comment_closure 테이블 매핑)
// 댓글 간 계층(대댓글) 관계를 Closure Table 패턴으로 관리한다
@Entity
@Table(name = "tb_board_comment_closure")
@IdClass(BoardCommentClosureId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardCommentClosureEntity {

	// 조상 댓글 ID (복합 PK 구성 요소)
	@Id
	@Column(name = "ancestor_id", length = 36)
	private String ancestorId;

	// 자손 댓글 ID (복합 PK 구성 요소)
	@Id
	@Column(name = "descendant_id", length = 36)
	private String descendantId;

	// 조상과 자손 간의 깊이 (0: 자기 자신, 1: 직계 자식, ...)
	@Column(name = "depth", nullable = false)
	private Integer depth;

	// 댓글 Closure 관계 생성 팩토리 메서드
	public static BoardCommentClosureEntity create(String ancestorId, String descendantId, int depth) {
		BoardCommentClosureEntity entity = new BoardCommentClosureEntity();
		entity.ancestorId                = ancestorId;
		entity.descendantId              = descendantId;
		entity.depth                     = depth;
		return entity;
	}
}
