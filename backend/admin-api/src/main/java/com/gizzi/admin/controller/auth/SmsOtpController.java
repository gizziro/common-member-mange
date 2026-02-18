package com.gizzi.admin.controller.auth;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.sms.dto.SendOtpRequestDto;
import com.gizzi.core.domain.sms.dto.VerifyOtpRequestDto;
import com.gizzi.core.domain.sms.dto.VerifyOtpResponseDto;
import com.gizzi.core.domain.sms.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// SMS OTP 인증 API 컨트롤러 (관리자 셋업 위자드에서 사용)
// 인증 불필요 (SecurityConfig에서 /auth/sms/** permitAll)
@Slf4j
@RestController
@RequestMapping("/auth/sms")
@RequiredArgsConstructor
public class SmsOtpController {

	// OTP 서비스
	private final OtpService otpService;

	// OTP 발송 API
	@PostMapping("/send-otp")
	public ResponseEntity<ApiResponseDto<Void>> sendOtp(
		@Valid @RequestBody SendOtpRequestDto request) {
		// OTP 생성 → 레이트 체크 → SMS 발송 → Redis 저장
		otpService.generateAndSend(request.getPhone());
		return ResponseEntity.ok(ApiResponseDto.ok(null));
	}

	// OTP 검증 API
	@PostMapping("/verify-otp")
	public ResponseEntity<ApiResponseDto<VerifyOtpResponseDto>> verifyOtp(
		@Valid @RequestBody VerifyOtpRequestDto request) {
		// OTP 코드 검증 → 성공 시 검증 토큰 반환
		VerifyOtpResponseDto response = otpService.verify(request.getPhone(), request.getCode());
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
