package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 게시판 카테고리 리포지토리 (tb_board_categories 테이블 접근)
public interface BoardCategoryRepository extends JpaRepository<BoardCategoryEntity, String> {

	// 게시판의 카테고리 목록 조회 (정렬 순서)
	List<BoardCategoryEntity> findByBoardInstanceIdOrderBySortOrderAsc(String boardInstanceId);

	// 슬러그 중복 확인
	boolean existsByBoardInstanceIdAndSlug(String boardInstanceId, String slug);

	// 슬러그로 카테고리 조회
	Optional<BoardCategoryEntity> findByBoardInstanceIdAndSlug(String boardInstanceId, String slug);
}
