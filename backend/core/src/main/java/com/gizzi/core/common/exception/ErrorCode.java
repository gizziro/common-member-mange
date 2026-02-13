package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 시스템 전역 에러 코드 정의
@Getter
@AllArgsConstructor
public enum ErrorCode {

	// === 공통 에러 (COM) ===
	INTERNAL_SERVER_ERROR ("COM_001", "서버 내부 오류",         HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_INPUT         ("COM_002", "유효하지 않은 입력값",   HttpStatus.BAD_REQUEST),
	RESOURCE_NOT_FOUND    ("COM_003", "리소스를 찾을 수 없음",  HttpStatus.NOT_FOUND),
	METHOD_NOT_ALLOWED    ("COM_004", "허용되지 않은 메서드",   HttpStatus.METHOD_NOT_ALLOWED),

	// === 인증 에러 (AUTH) ===
	UNAUTHORIZED          ("AUTH_001", "인증이 필요합니다",     HttpStatus.UNAUTHORIZED),
	ACCESS_DENIED         ("AUTH_002", "접근 권한이 없습니다",  HttpStatus.FORBIDDEN);

	// 에러 코드 문자열 (예: "COM_001")
	private final String code;

	// 기본 에러 메시지
	private final String message;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
