package com.gizzi.core.domain.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

// OTP 발송 요청 DTO (회원가입/셋업 위자드에서 사용)
// 수신 전화번호를 01X로 시작하는 10~11자리 숫자로 검증한다
@Getter
@NoArgsConstructor
public class SendOtpRequestDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 수신 전화번호 (01X로 시작하는 10~11자리 숫자)
	@NotBlank(message = "전화번호는 필수입니다")
	@Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다")
	private String phone;
}
