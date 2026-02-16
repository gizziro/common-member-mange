package com.gizzi.core.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 필터 체인 에러 응답 유틸리티
// Spring Security 필터 체인 내에서 발생하는 에러를
// ApiResponseDto 형식으로 직렬화하여 응답한다
// GlobalExceptionHandler(@ControllerAdvice)는 컨트롤러 이후에서만 동작하므로
// 필터 레벨 에러는 이 유틸리티를 통해 일관된 JSON 응답을 보장한다
@Component
@RequiredArgsConstructor
public class FilterErrorResponseWriter {

	// Jackson ObjectMapper (Spring Boot 자동 구성)
	private final ObjectMapper objectMapper;

	// ErrorCode 기반 에러 응답 작성
	// HTTP 상태 코드는 ErrorCode.getHttpStatus()에서 자동 추출
	public void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		// HTTP 상태 코드 설정 (ErrorCode에 정의된 값 사용)
		response.setStatus(errorCode.getHttpStatus().value());

		// Content-Type: application/json; charset=UTF-8
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// ApiResponseDto 에러 객체를 JSON으로 직렬화하여 응답 본문에 작성
		ApiResponseDto<Void> body = ApiResponseDto.error(errorCode);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
