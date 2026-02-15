package com.gizzi.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// 사용자 API 애플리케이션 진입점
// JWT 기반 인증을 사용하므로 Spring Security 기본 UserDetailsService 자동 설정을 제외한다
@SpringBootApplication(
	scanBasePackages = {"com.gizzi.core", "com.gizzi.user"},
	exclude = {UserDetailsServiceAutoConfiguration.class}
)
public class UserApiApplication {

	public static void main(String[] args) {
		// 사용자 API 서버 시작
		SpringApplication.run(UserApiApplication.class, args);
	}

}
