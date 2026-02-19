package com.gizzi.core.domain.sms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// SMS 프로바이더 설정 수정 요청 DTO
// apiSecret이 null이면 기존 값을 유지한다
@Getter
@NoArgsConstructor
public class UpdateSmsProviderRequestDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private String apiKey;			// API Key
	private String apiSecret;		// API Secret (null이면 기존 값 유지)
	private String senderNumber;	// 발신 번호

	// 활성화 여부 (필수)
	@NotNull(message = "활성화 여부는 필수입니다")
	private Boolean isEnabled;

	private String configJson;		// 추가 설정 JSON
}
