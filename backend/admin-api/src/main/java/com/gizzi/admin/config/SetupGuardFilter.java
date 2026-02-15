package com.gizzi.admin.config;

import com.gizzi.core.common.exception.SetupErrorCode;
import com.gizzi.core.domain.setup.service.SystemInitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 시스템 미초기화 상태에서 셋업 외 요청을 차단하는 필터
// SetupGuardFilter는 JwtAuthenticationFilter 앞에 위치하여
// 초기화되지 않은 시스템에서는 /setup/** 외 모든 요청에 403을 반환한다
@Slf4j
@Component
@RequiredArgsConstructor
public class SetupGuardFilter extends OncePerRequestFilter {

	// 시스템 초기화 서비스 (초기화 상태 조회 + 캐시)
	private final SystemInitService systemInitService;

	// 403 SETUP_REQUIRED 응답 JSON 고정 문자열 (ApiResponse 에러 형식)
	private static final String SETUP_REQUIRED_RESPONSE = String.format(
		"{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\",\"description\":\"%s\"}}",
		SetupErrorCode.SETUP_REQUIRED.getCode(),
		SetupErrorCode.SETUP_REQUIRED.getMessage(),
		SetupErrorCode.SETUP_REQUIRED.getDescription()
	);

	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException {
		// 시스템이 이미 초기화되었으면 모든 요청 통과
		if (systemInitService.isInitialized()) {
			filterChain.doFilter(request, response);
			return;
		}

		// 미초기화 상태: 허용된 경로인지 확인
		String requestUri = request.getRequestURI();

		// /setup/** 과 /actuator/** 는 미초기화 상태에서도 허용
		if (requestUri.startsWith("/setup") || requestUri.startsWith("/actuator")) {
			filterChain.doFilter(request, response);
			return;
		}

		// 미초기화 + 비허용 경로 → 403 SETUP_REQUIRED 응답
		log.warn("미초기화 시스템 접근 차단: uri={}, method={}", requestUri, request.getMethod());

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// 고정 JSON 응답 출력
		response.getWriter().write(SETUP_REQUIRED_RESPONSE);
	}
}
