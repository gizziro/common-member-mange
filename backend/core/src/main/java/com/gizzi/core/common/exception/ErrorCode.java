package com.gizzi.core.common.exception;

import org.springframework.http.HttpStatus;

// 시스템 전역 에러 코드 인터페이스
// 모듈별로 이 인터페이스를 구현하는 enum을 생성하여 에러 코드를 분산 관리한다
// 예: CommonErrorCode (공통), AuthErrorCode (인증), UserErrorCode (사용자)
public interface ErrorCode {

	// 에러 코드 문자열 (예: "COM_INTERNAL_ERROR", "AUTH_UNAUTHORIZED")
	String getCode();

	// 사용자에게 표시할 에러 메시지
	String getMessage();

	// 개발자용 상세 설명 (로깅/디버깅 용도)
	String getDescription();

	// HTTP 상태 코드
	HttpStatus getHttpStatus();
}
