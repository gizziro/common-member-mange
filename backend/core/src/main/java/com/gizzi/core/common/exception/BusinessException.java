package com.gizzi.core.common.exception;

import lombok.Getter;

// 비즈니스 로직에서 발생하는 예외의 기본 클래스
// 모든 비즈니스 예외는 이 클래스를 상속받아 ErrorCode를 지정해야 함
@Getter
public class BusinessException extends RuntimeException {

	// 이 예외에 해당하는 에러 코드
	private final ErrorCode errorCode;

	// ErrorCode의 기본 메시지를 사용하는 생성자
	public BusinessException(ErrorCode errorCode) {
		// RuntimeException에 기본 메시지 전달
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	// 커스텀 메시지를 사용하는 생성자
	public BusinessException(ErrorCode errorCode, String message) {
		// RuntimeException에 커스텀 메시지 전달
		super(message);
		this.errorCode = errorCode;
	}
}
