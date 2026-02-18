package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardCommentClosureEntity;
import com.gizzi.module.board.entity.BoardCommentClosureId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 댓글 계층 구조(클로저 테이블) 리포지토리 (tb_board_comment_closure 테이블 접근)
public interface BoardCommentClosureRepository extends JpaRepository<BoardCommentClosureEntity, BoardCommentClosureId> {

	// 특정 댓글의 모든 자손 조회
	List<BoardCommentClosureEntity> findByAncestorId(String ancestorId);

	// 특정 댓글의 모든 조상 관계 삭제
	void deleteByDescendantId(String descendantId);
}
