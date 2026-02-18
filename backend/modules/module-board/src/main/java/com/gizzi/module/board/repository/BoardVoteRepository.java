package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardVoteEntity;
import com.gizzi.module.board.entity.VoteTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 게시판 추천/비추천 리포지토리 (tb_board_votes 테이블 접근)
public interface BoardVoteRepository extends JpaRepository<BoardVoteEntity, String> {

	// 특정 대상에 대한 사용자 투표 조회
	Optional<BoardVoteEntity> findByTargetTypeAndTargetIdAndUserId(VoteTargetType targetType, String targetId, String userId);

	// 투표 존재 여부 확인
	boolean existsByTargetTypeAndTargetIdAndUserId(VoteTargetType targetType, String targetId, String userId);
}
