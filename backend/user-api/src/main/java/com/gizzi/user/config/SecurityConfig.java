package com.gizzi.user.config;

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

// 사용자 API Spring Security 설정
// JWT 기반 무상태(Stateless) 인증을 사용하며 CSRF를 비활성화한다
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

	// JWT 인증 필터
	private final JwtAuthenticationFilter      jwtAuthenticationFilter;

	// 인증 실패 시 401 응답 처리기
	private final JwtAuthenticationEntryPoint   jwtAuthenticationEntryPoint;

	// Spring Security 필터 체인 설정
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// CSRF 비활성화 (JWT 무상태 인증이므로 불필요)
			.csrf(AbstractHttpConfigurer::disable)

			// 세션 관리: 무상태 (서버에서 세션을 생성하지 않음)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// 인증 실패 시 커스텀 엔트리포인트 사용
			.exceptionHandling(exception ->
				exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

			// URL 별 접근 권한 설정
			.authorizeHttpRequests(authorize -> authorize
				// 인증 관련 엔드포인트는 모두 허용
				.requestMatchers("/auth/signup", "/auth/login", "/auth/refresh", "/auth/me").permitAll()
				// 소셜 로그인 공개 엔드포인트 (인증 불필요)
				.requestMatchers(
					"/auth/oauth2/providers",       // Provider 목록 조회 (로그인 페이지)
					"/auth/oauth2/authorize/**",    // Authorization URL 생성 (로그인 페이지)
					"/auth/oauth2/callback/**",     // OAuth2 콜백 (Provider 리다이렉트)
					"/auth/oauth2/link-confirm"     // 연동 확인 (pendingId 기반)
				).permitAll()
				// /auth/oauth2/identities, /auth/oauth2/link/*, /auth/oauth2/set-password 등은
				// anyRequest().authenticated()에 의해 인증 필요
				// 아이디/이메일 중복 확인 엔드포인트는 모두 허용
				.requestMatchers("/auth/check/**").permitAll()
				// 공개 페이지 엔드포인트 (인증 선택)
				.requestMatchers("/pages/**").permitAll()
				// Slug 기반 동적 라우팅 (비인증 사용자는 권한 빈 맵으로 응답)
				.requestMatchers("/resolve/**").permitAll()
				// Actuator 헬스체크는 모두 허용
				.requestMatchers("/actuator/health", "/actuator/info").permitAll()
				// 그 외 모든 요청은 인증 필요 (메뉴, resolve 등)
				.anyRequest().authenticated()
			)

			// JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
