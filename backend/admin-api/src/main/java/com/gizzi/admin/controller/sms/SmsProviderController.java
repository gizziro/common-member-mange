package com.gizzi.admin.controller.sms;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.sms.dto.SmsProviderResponseDto;
import com.gizzi.core.domain.sms.dto.TestSmsRequestDto;
import com.gizzi.core.domain.sms.dto.UpdateSmsProviderRequestDto;
import com.gizzi.core.domain.sms.service.SmsProviderService;
import com.gizzi.core.domain.sms.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// SMS 프로바이더 관리 API 컨트롤러 (관리자 전용)
// 프로바이더 목록 조회, 설정 수정, 테스트 발송을 담당한다
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sms/providers")
public class SmsProviderController {

	// SMS 프로바이더 관리 서비스
	private final SmsProviderService smsProviderService;

	// SMS 발송 서비스 (테스트 발송용)
	private final SmsService         smsService;

	// 전체 프로바이더 목록 조회
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<SmsProviderResponseDto>>> getProviders() {
		// 모든 SMS 프로바이더 목록 반환
		List<SmsProviderResponseDto> providers = smsProviderService.getAllProviders();
		return ResponseEntity.ok(ApiResponseDto.ok(providers));
	}

	// 프로바이더 설정 수정
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDto<SmsProviderResponseDto>> updateProvider(
		@PathVariable("id") String id,
		@Valid @RequestBody UpdateSmsProviderRequestDto request) {
		// 설정 수정 후 응답 반환
		SmsProviderResponseDto provider = smsProviderService.updateProvider(id, request);
		return ResponseEntity.ok(ApiResponseDto.ok(provider));
	}

	// 테스트 SMS 발송
	@PostMapping("/{id}/test")
	public ResponseEntity<ApiResponseDto<Void>> testSend(
		@PathVariable("id") String id,
		@Valid @RequestBody TestSmsRequestDto request) {
		// 지정 프로바이더로 테스트 SMS 발송
		smsService.sendTest(id, request.getPhone());
		return ResponseEntity.ok(ApiResponseDto.ok(null));
	}
}
