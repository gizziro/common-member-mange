package com.gizzi.core.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

// SMS 수신 가능 회원 수 응답 DTO
@Getter
@Builder
public class SmsRecipientCountDto {

	// 전체 수신 가능 회원 수
	private final long totalCount;
}
