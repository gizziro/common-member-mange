package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 시스템 전역 에러 코드 정의
@Getter
@AllArgsConstructor
public enum ErrorCode {

	// === 공통 에러 (COM) ===
	INTERNAL_SERVER_ERROR ("COM_INTERNAL_ERROR",    "서버 내부 오류",         HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_INPUT         ("COM_INVALID_INPUT",     "유효하지 않은 입력값",   HttpStatus.BAD_REQUEST),
	RESOURCE_NOT_FOUND    ("COM_RESOURCE_NOT_FOUND", "리소스를 찾을 수 없음", HttpStatus.NOT_FOUND),
	METHOD_NOT_ALLOWED    ("COM_METHOD_NOT_ALLOWED", "허용되지 않은 메서드",  HttpStatus.METHOD_NOT_ALLOWED),

	// === 인증 에러 (AUTH) ===
	UNAUTHORIZED          ("AUTH_UNAUTHORIZED",     "인증이 필요합니다",     HttpStatus.UNAUTHORIZED),
	ACCESS_DENIED         ("AUTH_ACCESS_DENIED",    "접근 권한이 없습니다",  HttpStatus.FORBIDDEN),

	// === 사용자 에러 (USER) ===
	DUPLICATE_USER_ID     ("USER_DUPLICATE_ID",    "이미 사용 중인 아이디입니다",  HttpStatus.CONFLICT),
	DUPLICATE_EMAIL       ("USER_DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다",  HttpStatus.CONFLICT),
	USER_NOT_FOUND        ("USER_NOT_FOUND",       "사용자를 찾을 수 없습니다",    HttpStatus.NOT_FOUND);

	// 에러 코드 문자열 (예: "COM_INTERNAL_ERROR")
	private final String code;

	// 기본 에러 메시지
	private final String message;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
