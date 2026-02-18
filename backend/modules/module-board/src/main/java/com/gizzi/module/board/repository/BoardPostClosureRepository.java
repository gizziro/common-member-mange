package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardPostClosureEntity;
import com.gizzi.module.board.entity.BoardPostClosureId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 게시글 계층 구조(클로저 테이블) 리포지토리 (tb_board_post_closure 테이블 접근)
public interface BoardPostClosureRepository extends JpaRepository<BoardPostClosureEntity, BoardPostClosureId> {

	// 특정 게시글의 모든 자손 조회
	List<BoardPostClosureEntity> findByAncestorId(String ancestorId);

	// 특정 게시글의 모든 조상 조회
	List<BoardPostClosureEntity> findByDescendantId(String descendantId);

	// 특정 게시글의 모든 조상 관계 삭제
	void deleteByDescendantId(String descendantId);
}
