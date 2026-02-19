package com.gizzi.core.domain.sms.service;

import com.gizzi.core.domain.sms.dto.SmsLogResponseDto;
import com.gizzi.core.domain.sms.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// SMS 발송 이력 조회 서비스
// 복합 필터 + 페이지네이션을 지원하는 읽기 전용 서비스
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SmsLogService
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final SmsLogRepository smsLogRepository;	// SMS 로그 리포지토리

	//======================================================================================================================
	// [ 핵심 비즈니스 메서드 ]
	//======================================================================================================================

	// 발송 이력 페이지네이션 조회 (복합 필터)
	// sendType: MANUAL/AUTO (null이면 전체)
	// startTime, endTime: 발송 일시 범위 (null이면 무시)
	public Page<SmsLogResponseDto> getLogs(String sendType, LocalDateTime startTime,
	                                       LocalDateTime endTime, Pageable pageable)
	{
		// 복합 필터로 조회 → DTO 변환
		return smsLogRepository.findByFilters(sendType, startTime, endTime, pageable)
			.map(SmsLogResponseDto::from);
	}
}
