package com.gizzi.admin.controller.audit;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.dto.PageResponseDto;
import com.gizzi.core.domain.audit.dto.AuditLogResponseDto;
import com.gizzi.core.domain.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

// 감사 로그 관리 컨트롤러 (관리자 전용)
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/audit-logs")
public class AuditLogController {

	// 감사 로그 서비스
	private final AuditLogService auditLogService;

	// 감사 로그 목록 조회 (복합 필터 + 페이지네이션)
	@GetMapping
	public ResponseEntity<ApiResponseDto<PageResponseDto<AuditLogResponseDto>>> getAuditLogs(
			@RequestParam(required = false) String actorUserId,
			@RequestParam(required = false) String actionType,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		// 서비스에 필터 조건 전달하여 조회
		Page<AuditLogResponseDto> page = auditLogService.getAuditLogs(
			actorUserId, actionType, startTime, endTime, pageable);

		// Page → PageResponseDto 변환
		PageResponseDto<AuditLogResponseDto> response = PageResponseDto.from(page, dto -> dto);

		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
