package com.gizzi.core.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

// SMS 대량 발송 결과 응답 DTO
@Getter
@Builder
public class SmsSendResultDto {

	// 대량 발송 묶음 ID (배치별 추적용)
	private final String batchId;

	// 발송 대상 총 건수
	private final int    totalCount;

	// 발송 성공 건수
	private final int    successCount;

	// 발송 실패 건수
	private final int    failCount;
}
