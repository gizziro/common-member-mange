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

// 관리자 API Spring Security 설정
// 모든 엔드포인트에 인증이 필요하며, 향후 ROLE_ADMIN 제한 추가 예정
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

			// 세션 관리: 무상태
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// 인증 실패 시 커스텀 엔트리포인트 사용
			.exceptionHandling(exception ->
				exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

			// URL 별 접근 권한 설정
			.authorizeHttpRequests(authorize -> authorize
				// Actuator 헬스체크만 허용
				.requestMatchers("/actuator/health", "/actuator/info").permitAll()
				// 관리자 API는 모든 엔드포인트 인증 필수
				.anyRequest().authenticated()
			)

			// JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
