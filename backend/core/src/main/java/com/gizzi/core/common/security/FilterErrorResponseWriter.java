package com.gizzi.core.common.security;

import com.gizzi.core.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 필터 체인 에러 응답 유틸리티
// Spring Security 필터 체인 내에서 발생하는 에러를
// ApiResponseDto 동일 형식의 JSON으로 응답한다
// GlobalExceptionHandler(@ControllerAdvice)는 컨트롤러 이후에서만 동작하므로
// 필터 레벨 에러는 이 유틸리티를 통해 일관된 JSON 응답을 보장한다
@Component
public class FilterErrorResponseWriter
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ JSON 템플릿 ]
	//----------------------------------------------------------------------------------------------------------------------
	// ApiResponseDto의 @JsonInclude(NON_NULL) 동작과 동일: success=false, data 필드 제외
	private static final String ERROR_JSON_TEMPLATE =
		"{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\",\"description\":\"%s\"}}";

	//======================================================================================================================
	// ErrorCode 기반 에러 응답 작성
	// HTTP 상태 코드는 ErrorCode.getHttpStatus()에서 자동 추출
	//======================================================================================================================
	public void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException
	{
		// HTTP 상태 코드 설정 (ErrorCode에 정의된 값 사용)
		response.setStatus(errorCode.getHttpStatus().value());

		// Content-Type: application/json; charset=UTF-8
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// ErrorCode의 code, message, description을 JSON 템플릿에 삽입
		String json = String.format(ERROR_JSON_TEMPLATE,
			escapeJson(errorCode.getCode()),
			escapeJson(errorCode.getMessage()),
			escapeJson(errorCode.getDescription()));

		// 응답 본문에 JSON 문자열 작성
		response.getWriter().write(json);
	}

	//----------------------------------------------------------------------------------------------------------------------
	// JSON 문자열 내 특수문자 이스케이프 (인젝션 방지)
	//----------------------------------------------------------------------------------------------------------------------
	private static String escapeJson(String value)
	{
		// null 값은 빈 문자열로 처리
		if (value == null) return "";

		// 백슬래시, 따옴표, 줄바꿈, 캐리지리턴, 탭 이스케이프
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}
}
