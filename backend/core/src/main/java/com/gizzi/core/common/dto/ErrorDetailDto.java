package com.gizzi.core.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 에러 응답의 상세 정보를 담는 DTO
@Getter
@AllArgsConstructor
public class ErrorDetailDto {

	// 에러 코드 (예: "COM_INTERNAL_ERROR", "AUTH_UNAUTHORIZED")
	private final String code;

	// 에러 메시지 (사용자에게 표시할 내용)
	private final String message;

	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final String description;
}
