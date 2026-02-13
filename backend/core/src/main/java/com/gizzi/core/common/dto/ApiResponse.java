package com.gizzi.core.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gizzi.core.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 모든 API 응답을 감싸는 공통 응답 객체
// null 값 필드는 JSON 직렬화 시 제외
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	// 요청 성공 여부
	private final boolean success;

	// 응답 데이터 (성공 시)
	private final T data;

	// 에러 상세 (실패 시)
	private final ErrorDetail error;

	// 데이터가 있는 정상 응답 생성
	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null);
	}

	// 데이터 없는 정상 응답 생성
	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null);
	}

	// ErrorCode 기반 에러 응답 생성 (기본 메시지 사용)
	public static ApiResponse<Void> error(ErrorCode errorCode) {
		// ErrorCode에 정의된 기본 메시지 사용
		ErrorDetail detail = new ErrorDetail(errorCode.getCode(), errorCode.getMessage());
		return new ApiResponse<>(false, null, detail);
	}

	// ErrorCode 기반 에러 응답 생성 (커스텀 메시지 사용)
	public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
		// 전달받은 커스텀 메시지로 에러 상세 생성
		ErrorDetail detail = new ErrorDetail(errorCode.getCode(), message);
		return new ApiResponse<>(false, null, detail);
	}
}
