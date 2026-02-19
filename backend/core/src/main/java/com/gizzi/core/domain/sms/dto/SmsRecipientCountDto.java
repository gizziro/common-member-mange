package com.gizzi.core.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

// SMS 수신 가능 회원 수 응답 DTO
// 전체 수신 가능 회원 수를 담는 불변 객체
@Getter
@Builder
public class SmsRecipientCountDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final long totalCount;		// 전체 수신 가능 회원 수
}
