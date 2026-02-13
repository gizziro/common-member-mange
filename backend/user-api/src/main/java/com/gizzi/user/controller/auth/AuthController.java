package com.gizzi.user.controller.auth;

import com.gizzi.core.common.dto.ApiResponse;
import com.gizzi.core.domain.user.dto.SignupRequest;
import com.gizzi.core.domain.user.dto.SignupResponse;
import com.gizzi.core.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 관련 API 컨트롤러 (회원가입, 로그인 등)
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	// 사용자 서비스
	private final UserService userService;

	// 로컬 회원가입 API
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
			@Valid @RequestBody SignupRequest request) {
		// 회원가입 서비스 호출
		SignupResponse response = userService.signup(request);

		// 201 Created 응답 반환
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(response));
	}
}
