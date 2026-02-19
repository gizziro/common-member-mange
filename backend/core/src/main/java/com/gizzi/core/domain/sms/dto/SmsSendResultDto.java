package com.gizzi.core.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

// SMS 대량 발송 결과 응답 DTO
// 배치 ID, 총 건수, 성공/실패 건수를 담는다
@Getter
@Builder
public class SmsSendResultDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String batchId;		// 대량 발송 묶음 ID (배치별 추적용)
	private final int    totalCount;	// 발송 대상 총 건수
	private final int    successCount;	// 발송 성공 건수
	private final int    failCount;		// 발송 실패 건수
}
