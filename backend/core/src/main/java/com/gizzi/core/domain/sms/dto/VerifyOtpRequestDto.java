package com.gizzi.core.domain.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

// OTP 인증 검증 요청 DTO
@Getter
@NoArgsConstructor
public class VerifyOtpRequestDto {

	// 전화번호
	@NotBlank(message = "전화번호는 필수입니다")
	@Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다")
	private String phone;

	// 인증 코드
	@NotBlank(message = "인증 코드는 필수입니다")
	private String code;
}
