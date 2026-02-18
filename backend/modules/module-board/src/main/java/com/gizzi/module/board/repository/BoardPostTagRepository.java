package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardPostTagEntity;
import com.gizzi.module.board.entity.BoardPostTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 게시글-태그 연결 리포지토리 (tb_board_post_tags 테이블 접근)
public interface BoardPostTagRepository extends JpaRepository<BoardPostTagEntity, BoardPostTagId> {

	// 게시글의 태그 연결 목록
	List<BoardPostTagEntity> findByPostId(String postId);

	// 게시글의 모든 태그 연결 삭제
	void deleteByPostId(String postId);

	// 태그에 연결된 게시글 목록
	List<BoardPostTagEntity> findByTagId(String tagId);
}
