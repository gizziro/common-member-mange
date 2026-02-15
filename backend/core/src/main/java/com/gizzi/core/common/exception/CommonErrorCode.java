package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 시스템 공통 에러 코드 (COM_*)
// 서버 오류, 입력 검증, 404, 405 등 모든 모듈에 공통으로 적용되는 에러
@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	// 서버 내부 오류 (예상치 못한 예외)
	INTERNAL_SERVER_ERROR("COM_INTERNAL_ERROR",    "서버 내부 오류",         "예상치 못한 서버 오류가 발생함",        HttpStatus.INTERNAL_SERVER_ERROR),

	// Bean Validation 검증 실패
	INVALID_INPUT        ("COM_INVALID_INPUT",     "유효하지 않은 입력값",   "Bean Validation 검증 실패",            HttpStatus.BAD_REQUEST),

	// 요청한 리소스가 존재하지 않음
	RESOURCE_NOT_FOUND   ("COM_RESOURCE_NOT_FOUND", "리소스를 찾을 수 없음", "요청한 리소스가 존재하지 않음",         HttpStatus.NOT_FOUND),

	// 지원하지 않는 HTTP 메서드
	METHOD_NOT_ALLOWED   ("COM_METHOD_NOT_ALLOWED", "허용되지 않은 메서드",  "HTTP 메서드가 지원되지 않음",           HttpStatus.METHOD_NOT_ALLOWED);

	// 에러 코드 문자열 (예: "COM_INTERNAL_ERROR")
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
