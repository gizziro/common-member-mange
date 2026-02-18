package com.gizzi.core.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 로그인 OTP 검증 요청 DTO
@Getter
@NoArgsConstructor
public class OtpVerifyRequestDto {

	// OTP 세션 ID (로그인 시 발급받은 임시 세션)
	@NotBlank(message = "OTP 세션 ID는 필수입니다")
	private String otpSessionId;

	// 사용자가 입력한 OTP 코드
	@NotBlank(message = "인증 코드는 필수입니다")
	private String code;
}
