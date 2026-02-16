package com.gizzi.admin.controller.auth;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.exception.AuthErrorCode;
import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.domain.auth.dto.LoginRequestDto;
import com.gizzi.core.domain.auth.dto.LoginResponseDto;
import com.gizzi.core.domain.auth.dto.TokenRefreshRequestDto;
import com.gizzi.core.domain.auth.dto.TokenRefreshResponseDto;
import com.gizzi.core.domain.auth.dto.UserMeResponseDto;
import com.gizzi.core.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 인증 API 컨트롤러 (로그인, 로그아웃, 토큰 갱신, 사용자 정보)
// 회원가입/중복체크는 제외 (관리자 가입은 /setup/initialize로 처리)
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	// 인증 서비스
	private final AuthService authService;

	// 관리자 로그인 API
	@PostMapping("/login")
	public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
			@Valid @RequestBody LoginRequestDto request,
			HttpServletRequest httpRequest) {
		// 클라이언트 IP 주소 추출
		String ipAddress = httpRequest.getRemoteAddr();
		// 클라이언트 User-Agent 추출
		String userAgent = httpRequest.getHeader("User-Agent");

		// 로그인 서비스 호출
		LoginResponseDto response = authService.login(request, ipAddress, userAgent);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 관리자 로그아웃 API
	@PostMapping("/logout")
	public ResponseEntity<ApiResponseDto<Void>> logout(
			@RequestHeader("Authorization") String authHeader) {
		// Authorization 헤더에서 토큰 추출
		String token = authService.extractToken(authHeader);

		// 토큰이 없으면 잘못된 요청
		if (token == null) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		// 로그아웃 서비스 호출
		authService.logout(token);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// 토큰 갱신 API
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponseDto<TokenRefreshResponseDto>> refresh(
			@Valid @RequestBody TokenRefreshRequestDto request) {
		// 토큰 갱신 서비스 호출
		TokenRefreshResponseDto response = authService.refresh(request);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 현재 사용자 정보 조회 API (토큰 유효성 확인 + grace period 적용)
	@GetMapping("/me")
	public ResponseEntity<ApiResponseDto<UserMeResponseDto>> me(
			@RequestHeader(value = "Authorization", required = false) String authHeader) {
		// Authorization 헤더에서 토큰 추출
		String token = authService.extractToken(authHeader);

		// 토큰이 없으면 인증 필요 에러
		if (token == null) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
		}

		// 사용자 정보 조회 서비스 호출 (grace period 적용)
		UserMeResponseDto response = authService.me(token);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
