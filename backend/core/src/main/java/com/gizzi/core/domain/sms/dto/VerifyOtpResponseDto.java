package com.gizzi.core.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

// OTP 인증 검증 응답 DTO
@Getter
@Builder
public class VerifyOtpResponseDto {

	// 인증 성공 여부
	private final boolean verified;

	// 인증 성공 시 검증 토큰 (회원가입/셋업에서 phoneVerified 증명용)
	private final String verificationToken;

	// 인증 성공 응답 생성
	public static VerifyOtpResponseDto success(String verificationToken) {
		return VerifyOtpResponseDto.builder()
			.verified(true)
			.verificationToken(verificationToken)
			.build();
	}
}
