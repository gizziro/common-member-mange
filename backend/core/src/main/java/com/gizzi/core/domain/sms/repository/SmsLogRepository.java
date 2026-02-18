package com.gizzi.core.domain.sms.repository;

import com.gizzi.core.domain.sms.entity.SmsLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

// SMS 발송 이력 리포지토리 (tb_sms_logs 테이블 접근)
public interface SmsLogRepository extends JpaRepository<SmsLogEntity, String> {

	// 복합 필터 조건으로 SMS 발송 이력 페이지네이션 조회
	// sendType: MANUAL/AUTO (null이면 전체)
	// startTime, endTime: 발송 일시 범위 (null이면 무시)
	@Query("SELECT l FROM SmsLogEntity l " +
		"WHERE (:sendType IS NULL OR l.sendType = :sendType) " +
		"AND (:startTime IS NULL OR l.createdAt >= :startTime) " +
		"AND (:endTime IS NULL OR l.createdAt <= :endTime) " +
		"ORDER BY l.createdAt DESC")
	Page<SmsLogEntity> findByFilters(
		@Param("sendType") String sendType,
		@Param("startTime") LocalDateTime startTime,
		@Param("endTime") LocalDateTime endTime,
		Pageable pageable
	);
}
