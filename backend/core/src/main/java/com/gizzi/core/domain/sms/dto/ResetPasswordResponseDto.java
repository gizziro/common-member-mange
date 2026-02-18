package com.gizzi.core.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

// 비밀번호 초기화 응답 DTO
// 임시 비밀번호와 SMS 발송 여부를 포함한다
@Getter
@Builder
public class ResetPasswordResponseDto {

	// 생성된 임시 비밀번호
	private final String  temporaryPassword;

	// SMS 발송 성공 여부 (전화번호 없거나 발송 실패 시 false)
	private final boolean smsSent;
}
