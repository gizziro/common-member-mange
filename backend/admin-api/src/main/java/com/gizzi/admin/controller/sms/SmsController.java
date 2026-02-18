package com.gizzi.admin.controller.sms;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.dto.PageResponseDto;
import com.gizzi.core.domain.sms.dto.GroupRecipientsDto;
import com.gizzi.core.domain.sms.dto.ManualSmsRequestDto;
import com.gizzi.core.domain.sms.dto.SmsLogResponseDto;
import com.gizzi.core.domain.sms.dto.SmsRecipientCountDto;
import com.gizzi.core.domain.sms.dto.SmsSendResultDto;
import com.gizzi.core.domain.sms.service.ManualSmsService;
import com.gizzi.core.domain.sms.service.SmsLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

// SMS 발송 및 이력 관리 API 컨트롤러 (관리자 전용)
// 수동 SMS 대량 발송, 발송 이력 조회, 수신 대상 조회를 담당한다
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sms")
public class SmsController {

	// 수동 SMS 발송 서비스
	private final ManualSmsService manualSmsService;

	// SMS 이력 조회 서비스
	private final SmsLogService    smsLogService;

	// 수동 SMS 대량 발송
	@PostMapping("/send")
	public ResponseEntity<ApiResponseDto<SmsSendResultDto>> sendManualSms(
			@Valid @RequestBody ManualSmsRequestDto request,
			Authentication authentication) {
		// 현재 관리자 PK
		String adminPk = authentication.getName();

		// 수동 SMS 발송
		SmsSendResultDto result = manualSmsService.sendManualSms(adminPk, request);

		// 200 OK + 발송 결과 반환
		return ResponseEntity.ok(ApiResponseDto.ok(result));
	}

	// SMS 발송 이력 조회 (페이지네이션 + 필터)
	@GetMapping("/logs")
	public ResponseEntity<ApiResponseDto<PageResponseDto<SmsLogResponseDto>>> getLogs(
			@RequestParam(required = false) String sendType,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		// 복합 필터 + 페이지네이션 조회
		Page<SmsLogResponseDto> page = smsLogService.getLogs(sendType, startTime, endTime, pageable);

		// PageResponseDto로 래핑하여 응답
		PageResponseDto<SmsLogResponseDto> response = PageResponseDto.from(page, dto -> dto);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 전체 SMS 수신 가능 회원 수 조회
	@GetMapping("/recipients/count")
	public ResponseEntity<ApiResponseDto<SmsRecipientCountDto>> getRecipientCount() {
		// 수신 가능 회원 수 조회
		SmsRecipientCountDto result = manualSmsService.getRecipientCount();
		return ResponseEntity.ok(ApiResponseDto.ok(result));
	}

	// 그룹별 SMS 수신 가능 회원 목록 조회
	@GetMapping("/recipients/groups")
	public ResponseEntity<ApiResponseDto<List<GroupRecipientsDto>>> getGroupRecipients() {
		// 그룹별 수신 가능 회원 수 조회
		List<GroupRecipientsDto> result = manualSmsService.getGroupRecipients();
		return ResponseEntity.ok(ApiResponseDto.ok(result));
	}
}
