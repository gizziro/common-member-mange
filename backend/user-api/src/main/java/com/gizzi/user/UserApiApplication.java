package com.gizzi.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 사용자 API 애플리케이션 진입점
@SpringBootApplication(scanBasePackages = {"com.gizzi.core", "com.gizzi.user"})
public class UserApiApplication {

	public static void main(String[] args) {
		// 사용자 API 서버 시작
		SpringApplication.run(UserApiApplication.class, args);
	}

}
