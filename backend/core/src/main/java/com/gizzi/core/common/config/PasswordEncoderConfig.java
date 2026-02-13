package com.gizzi.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 비밀번호 인코더 설정
// Spring Security 전체가 아닌 spring-security-crypto 모듈만 사용하여 BCrypt 해싱을 제공한다
@Configuration
public class PasswordEncoderConfig {

	// BCrypt 패스워드 인코더 빈 등록 (cost factor: 기본값 10)
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
