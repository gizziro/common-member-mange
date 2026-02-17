package com.gizzi.core.domain.audit.repository;

import com.gizzi.core.domain.audit.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

// 감사 로그 리포지토리 — 복합 필터 + 페이지네이션 지원
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {

	// 복합 필터 조회 (각 파라미터가 null이면 해당 조건 무시)
	@Query("SELECT a FROM AuditLogEntity a WHERE "
		+ "(:actorUserId IS NULL OR a.actorUserId = :actorUserId) "
		+ "AND (:actionType IS NULL OR a.actionType = :actionType) "
		+ "AND (:startTime IS NULL OR a.createdAt >= :startTime) "
		+ "AND (:endTime IS NULL OR a.createdAt <= :endTime) "
		+ "ORDER BY a.createdAt DESC")
	Page<AuditLogEntity> findByFilters(@Param("actorUserId") String actorUserId,
	                                   @Param("actionType") String actionType,
	                                   @Param("startTime") LocalDateTime startTime,
	                                   @Param("endTime") LocalDateTime endTime,
	                                   Pageable pageable);
}
