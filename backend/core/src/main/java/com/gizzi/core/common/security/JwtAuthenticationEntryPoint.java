package com.gizzi.core.common.security;

import com.gizzi.core.common.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증되지 않은 요청에 대한 401 응답 처리기
// Spring Security에서 인증 실패 시 ApiResponse JSON 형식으로 응답한다
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	// 401 응답 JSON 고정 문자열 (ApiResponse 에러 형식)
	private static final String UNAUTHORIZED_RESPONSE = String.format(
		"{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\",\"description\":\"%s\"}}",
		AuthErrorCode.UNAUTHORIZED.getCode(),
		AuthErrorCode.UNAUTHORIZED.getMessage(),
		AuthErrorCode.UNAUTHORIZED.getDescription()
	);

	@Override
	public void commence(HttpServletRequest request,
	                     HttpServletResponse response,
	                     AuthenticationException authException) throws IOException {
		// 인증 실패 로그 기록
		log.debug("인증 실패: uri={}, message={}", request.getRequestURI(), authException.getMessage());

		// 401 Unauthorized 응답 설정
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// 고정 JSON 응답 출력
		response.getWriter().write(UNAUTHORIZED_RESPONSE);
	}
}
