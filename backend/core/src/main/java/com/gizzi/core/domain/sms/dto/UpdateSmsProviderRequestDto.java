package com.gizzi.core.domain.sms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// SMS 프로바이더 설정 수정 요청 DTO
@Getter
@NoArgsConstructor
public class UpdateSmsProviderRequestDto {

	// API Key
	private String apiKey;

	// API Secret (null이면 기존 값 유지)
	private String apiSecret;

	// 발신 번호
	private String senderNumber;

	// 활성화 여부 (필수)
	@NotNull(message = "활성화 여부는 필수입니다")
	private Boolean isEnabled;

	// 추가 설정 JSON
	private String configJson;
}
