package com.gizzi.core.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 에러 응답의 상세 정보를 담는 DTO
@Getter
@AllArgsConstructor
public class ErrorDetail {

	// 에러 코드 (예: "COM_001", "AUTH_001")
	private final String code;

	// 에러 메시지 (사용자에게 표시할 내용)
	private final String message;
}
