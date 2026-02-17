package com.gizzi.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// 관리자 API 애플리케이션 진입점
// JWT 기반 인증을 사용하므로 Spring Security 기본 UserDetailsService 자동 설정을 제외한다
// com.gizzi.module: 기능 모듈(module-board 등)의 컴포넌트 스캔
@SpringBootApplication(
	scanBasePackages = {"com.gizzi.core", "com.gizzi.admin", "com.gizzi.module"},
	exclude = {UserDetailsServiceAutoConfiguration.class}
)
public class AdminApiApplication {

	public static void main(String[] args) {
		// 관리자 API 서버 시작
		SpringApplication.run(AdminApiApplication.class, args);
	}

}
