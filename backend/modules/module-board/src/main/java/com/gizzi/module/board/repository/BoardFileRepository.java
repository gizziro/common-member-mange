package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 게시판 첨부파일 리포지토리 (tb_board_files 테이블 접근)
public interface BoardFileRepository extends JpaRepository<BoardFileEntity, String> {

	// 게시글의 첨부파일 목록
	List<BoardFileEntity> findByPostIdOrderBySortOrderAsc(String postId);

	// 게시글의 첨부파일 수
	long countByPostId(String postId);
}
