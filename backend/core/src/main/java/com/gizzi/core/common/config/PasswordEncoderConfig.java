package com.gizzi.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 비밀번호 인코더 설정
// BCrypt 해싱을 제공하며 Spring Security 인증 매니저에서도 이 빈을 사용한다
@Configuration
public class PasswordEncoderConfig {

	// BCrypt 패스워드 인코더 빈 등록 (cost factor: 기본값 10)
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
