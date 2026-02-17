package com.gizzi.admin.config;

import com.gizzi.core.common.exception.AuthErrorCode;
import com.gizzi.core.common.security.FilterErrorResponseWriter;
import com.gizzi.core.domain.group.service.AdminAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 관리자 그룹 소속 여부를 확인하는 필터
// JwtAuthenticationFilter 이후에 실행되어, 인증된 사용자가 administrator 그룹 소속인지 검증한다
// 비소속 사용자는 403 ACCESS_DENIED 응답으로 차단된다
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminGroupFilter extends OncePerRequestFilter {

	// 관리자 접근 확인 서비스
	private final AdminAccessService       adminAccessService;

	// 필터 에러 응답 유틸리티 (ApiResponseDto JSON 직렬화)
	private final FilterErrorResponseWriter filterErrorResponseWriter;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException {
		// 인증 정보가 없으면 (미인증 요청) 다음 필터로 위임
		// → permitAll 경로이거나 JwtAuthenticationEntryPoint가 처리
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			filterChain.doFilter(request, response);
			return;
		}

		// SecurityContext에서 사용자 PK 추출 (JwtAuthenticationFilter가 설정한 principal)
		String userPk = authentication.getName();

		// "anonymousUser"인 경우 (Spring Security 기본 익명 인증) 스킵
		if ("anonymousUser".equals(userPk)) {
			filterChain.doFilter(request, response);
			return;
		}

		// administrator 그룹 소속 여부 확인
		if (!adminAccessService.isAdminMember(userPk)) {
			// 비관리자 사용자의 관리 API 접근 차단
			log.warn("관리자 그룹 미소속 사용자 접근 차단: userPk={}, uri={}", userPk, request.getRequestURI());
			filterErrorResponseWriter.writeError(response, AuthErrorCode.ACCESS_DENIED);
			return;
		}

		// 관리자 그룹 소속 확인 완료 → 다음 필터로 진행
		filterChain.doFilter(request, response);
	}

	// 필터 스킵 경로 설정
	// /setup, /auth/login, /auth/refresh, /actuator 경로는 그룹 확인을 건너뛴다
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri.startsWith("/setup")
			|| uri.equals("/auth/login")
			|| uri.equals("/auth/refresh")
			|| uri.startsWith("/actuator");
	}
}
