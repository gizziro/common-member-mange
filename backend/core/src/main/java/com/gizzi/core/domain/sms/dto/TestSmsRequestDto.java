package com.gizzi.core.domain.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

// SMS 테스트 발송 요청 DTO
@Getter
@NoArgsConstructor
public class TestSmsRequestDto {

	// 수신 전화번호 (숫자만 허용, 10~11자리)
	@NotBlank(message = "전화번호는 필수입니다")
	@Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다")
	private String phone;
}
