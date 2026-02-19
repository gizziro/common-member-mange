package com.gizzi.core.domain.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

// 수동 SMS 발송 요청 DTO
// 관리자가 회원에게 직접 SMS를 발송할 때 사용한다
@Getter
public class ManualSmsRequestDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 수신 대상 유형 (ALL / GROUP / INDIVIDUAL)
	@NotNull(message = "수신 대상 유형은 필수입니다")
	private String recipientType;

	private List<String> groupIds;		// GROUP 유형 시 대상 그룹 ID 목록
	private List<String> userIds;		// INDIVIDUAL 유형 시 대상 사용자 ID 목록

	// 발송할 메시지 내용
	@NotBlank(message = "메시지는 필수입니다")
	private String message;
}
