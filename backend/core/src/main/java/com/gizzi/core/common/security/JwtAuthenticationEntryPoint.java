package com.gizzi.core.common.security;

import com.gizzi.core.common.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증되지 않은 요청에 대한 401 응답 처리기
// Spring Security에서 인증 실패 시 ApiResponse JSON 형식으로 응답한다
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final FilterErrorResponseWriter filterErrorResponseWriter;		// 필터 에러 응답 유틸리티 (ApiResponseDto JSON 직렬화)

	//======================================================================================================================
	// 인증 실패 시 401 Unauthorized 응답 반환
	//======================================================================================================================
	@Override
	public void commence(HttpServletRequest request,
	                     HttpServletResponse response,
	                     AuthenticationException authException) throws IOException
	{
		// 인증 실패 로그 기록
		log.debug("인증 실패: uri={}, message={}", request.getRequestURI(), authException.getMessage());

		// 401 Unauthorized — ApiResponseDto 형식으로 응답
		filterErrorResponseWriter.writeError(response, AuthErrorCode.UNAUTHORIZED);
	}
}
