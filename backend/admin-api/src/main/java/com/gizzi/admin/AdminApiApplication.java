package com.gizzi.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 관리자 API 애플리케이션 진입점
@SpringBootApplication(scanBasePackages = {"com.gizzi.core", "com.gizzi.admin"})
public class AdminApiApplication {

	public static void main(String[] args) {
		// 관리자 API 서버 시작
		SpringApplication.run(AdminApiApplication.class, args);
	}

}
