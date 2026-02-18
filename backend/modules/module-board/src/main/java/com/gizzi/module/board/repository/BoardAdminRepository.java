package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardAdminEntity;
import com.gizzi.module.board.entity.BoardAdminId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 게시판 부관리자 리포지토리 (tb_board_admins 테이블 접근)
public interface BoardAdminRepository extends JpaRepository<BoardAdminEntity, BoardAdminId> {

	// 게시판의 부관리자 목록
	List<BoardAdminEntity> findByBoardInstanceId(String boardInstanceId);

	// 부관리자 존재 여부 확인
	boolean existsByBoardInstanceIdAndUserId(String boardInstanceId, String userId);

	// 부관리자 삭제
	void deleteByBoardInstanceIdAndUserId(String boardInstanceId, String userId);
}
