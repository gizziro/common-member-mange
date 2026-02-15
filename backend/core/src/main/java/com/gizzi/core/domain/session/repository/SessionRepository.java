package com.gizzi.core.domain.session.repository;

import com.gizzi.core.domain.session.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 세션 리포지토리 (tb_sessions 테이블 접근)
public interface SessionRepository extends JpaRepository<SessionEntity, String> {

	// 세션 ID로 활성 세션 조회 (폐기되지 않은 세션만)
	Optional<SessionEntity> findByIdAndRevokedAtIsNull(String id);

	// 사용자의 활성 세션 목록 조회 (폐기되지 않은 세션만)
	List<SessionEntity> findByUserIdAndRevokedAtIsNull(String userId);
}
