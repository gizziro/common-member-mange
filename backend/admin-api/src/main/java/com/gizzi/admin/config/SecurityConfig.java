package com.gizzi.admin.config;

import com.gizzi.core.common.config.JwtProperties;
import com.gizzi.core.common.security.JwtAuthenticationEntryPoint;
import com.gizzi.core.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

// 관리자 API Spring Security 설정
// 모든 엔드포인트에 인증이 필요하며, 향후 ROLE_ADMIN 제한 추가 예정
// 시스템 미초기화 시 SetupGuardFilter가 /setup/** 외 요청을 차단한다
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

	// JWT 인증 필터
	private final JwtAuthenticationFilter      jwtAuthenticationFilter;

	// 인증 실패 시 401 응답 처리기
	private final JwtAuthenticationEntryPoint   jwtAuthenticationEntryPoint;

	// 시스템 초기 설정 가드 필터 (미초기화 시 비셋업 요청 차단)
	private final SetupGuardFilter             setupGuardFilter;

	// 관리자 그룹 소속 확인 필터 (인증 후 administrator 그룹 멤버십 검증)
	private final AdminGroupFilter             adminGroupFilter;

	// Spring Security 필터 체인 설정
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// CSRF 비활성화 (JWT 무상태 인증이므로 불필요)
			.csrf(AbstractHttpConfigurer::disable)

			// 세션 관리: 무상태
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// 인증 실패 시 커스텀 엔트리포인트 사용
			.exceptionHandling(exception ->
				exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

			// URL 별 접근 권한 설정
			.authorizeHttpRequests(authorize -> authorize
				// 셋업 위자드 엔드포인트 인증 없이 허용
				.requestMatchers("/setup/**").permitAll()
				// 인증 엔드포인트 (로그인, 토큰 갱신) 인증 없이 허용
				.requestMatchers("/auth/login", "/auth/refresh").permitAll()
				// Actuator 헬스체크 허용
				.requestMatchers("/actuator/health", "/actuator/info").permitAll()
				// 관리자 API는 모든 엔드포인트 인증 필수
				.anyRequest().authenticated()
			)

			// SetupGuardFilter를 SecurityContextHolderFilter 직후에 추가 (필터 체인 초반에서 미초기화 조기 차단)
			.addFilterAfter(setupGuardFilter, SecurityContextHolderFilter.class)
			// JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			// AdminGroupFilter를 JwtAuthenticationFilter 이후에 추가 (인증 후 관리자 그룹 검증)
			.addFilterAfter(adminGroupFilter, JwtAuthenticationFilter.class);

		return http.build();
	}
}
