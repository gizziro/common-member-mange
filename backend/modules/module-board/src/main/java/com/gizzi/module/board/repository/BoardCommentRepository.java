package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 게시판 댓글 리포지토리 (tb_board_comments 테이블 접근)
public interface BoardCommentRepository extends JpaRepository<BoardCommentEntity, String> {

	// 게시글의 전체 댓글 목록 (시간순)
	List<BoardCommentEntity> findByPostIdOrderByCreatedAtAsc(String postId);

	// 삭제되지 않은 댓글 목록
	List<BoardCommentEntity> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(String postId);

	// 삭제되지 않은 댓글 수
	long countByPostIdAndIsDeletedFalse(String postId);
}
