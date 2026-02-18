package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 게시판 태그 리포지토리 (tb_board_tags 테이블 접근)
public interface BoardTagRepository extends JpaRepository<BoardTagEntity, String> {

	// 게시판의 태그 목록 (인기순)
	List<BoardTagEntity> findByBoardInstanceIdOrderByPostCountDesc(String boardInstanceId);

	// 게시판의 태그 목록 (이름순)
	List<BoardTagEntity> findByBoardInstanceIdOrderByNameAsc(String boardInstanceId);

	// 이름으로 태그 조회
	Optional<BoardTagEntity> findByBoardInstanceIdAndName(String boardInstanceId, String name);

	// 슬러그로 태그 조회
	Optional<BoardTagEntity> findByBoardInstanceIdAndSlug(String boardInstanceId, String slug);
}
