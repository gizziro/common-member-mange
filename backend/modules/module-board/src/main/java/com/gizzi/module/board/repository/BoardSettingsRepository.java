package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// 게시판 설정 리포지토리 (tb_board_settings 테이블 접근)
public interface BoardSettingsRepository extends JpaRepository<BoardSettingsEntity, String> {
	// 기본 CRUD는 JpaRepository에서 제공
}
