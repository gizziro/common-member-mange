package com.gizzi.core.domain.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// SMS 발송 요청 DTO (내부 서비스용)
@Getter
@Builder
@AllArgsConstructor
public class SendSmsRequestDto {

	// 수신 전화번호
	private final String to;

	// 발송 메시지 내용
	private final String message;
}
