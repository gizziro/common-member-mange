package com.gizzi.user.controller.auth;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.domain.auth.dto.LoginResponseDto;
import com.gizzi.core.domain.auth.dto.OAuth2LinkConfirmRequestDto;
import com.gizzi.core.domain.auth.dto.OAuth2LoginResultDto;
import com.gizzi.core.domain.auth.dto.OAuth2ProviderDto;
import com.gizzi.core.domain.auth.dto.SetPasswordRequestDto;
import com.gizzi.core.domain.auth.dto.UserIdentityResponseDto;
import com.gizzi.core.domain.auth.service.OAuth2Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// 소셜 로그인 API 컨트롤러 (사용자용)
// Provider 목록 조회, Authorization URL 생성, 콜백 처리, 연동 확인/관리를 담당한다
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth2")
public class OAuth2Controller {

	// OAuth2 서비스
	private final OAuth2Service oauth2Service;

	// 활성 소셜 Provider 목록 조회 (로그인 페이지 표시용)
	@GetMapping("/providers")
	public ResponseEntity<ApiResponseDto<List<OAuth2ProviderDto>>> getProviders() {
		// 활성화된 소셜 Provider 목록 반환
		List<OAuth2ProviderDto> providers = oauth2Service.getEnabledProviders();
		return ResponseEntity.ok(ApiResponseDto.ok(providers));
	}

	// Authorization URL 생성 → JSON 응답으로 URL 반환
	@GetMapping("/authorize/{provider}")
	public ResponseEntity<ApiResponseDto<String>> authorize(@PathVariable("provider") String provider) {
		// Authorization URL 생성 (state 포함)
		String authorizationUrl = oauth2Service.getAuthorizationUrl(provider);
		return ResponseEntity.ok(ApiResponseDto.ok(authorizationUrl));
	}

	// OAuth2 콜백 처리 (통합: 로그인 + 마이페이지 연동)
	// state 값의 모드에 따라 분기:
	//   login 모드 → 소셜 로그인 (토큰 발급 or 연동 대기)
	//   link 모드  → 마이페이지 소셜 연동 추가
	// 에러 발생 시 JSON 대신 프론트엔드 에러 페이지로 리다이렉트
	@GetMapping("/callback/{provider}")
	public ResponseEntity<Void> callback(
		@PathVariable("provider") String provider,
		@RequestParam("code") String code,
		@RequestParam("state") String state,
		HttpServletRequest request) {

		// state 모드 확인 (Redis 값 소비하지 않고 모드만 판별)
		String mode = oauth2Service.peekStateMode(state);

		// link 모드: 마이페이지 소셜 연동 추가
		if ("link".equals(mode)) {
			try {
				// 연동 콜백 처리 (기존 사용자에 연동 추가)
				oauth2Service.processLinkCallback(provider, code, state);

				// 프론트엔드 연동 성공 페이지로 리다이렉트
				String redirectUrl = "http://localhost:3020/auth/oauth2/link-success"
					+ "?provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8);

				log.info("OAuth2 연동 콜백 리다이렉트: provider={}", provider);

				return ResponseEntity.status(302)
					.location(URI.create(redirectUrl))
					.build();
			} catch (BusinessException e) {
				// 연동 에러 시 프론트엔드 에러 페이지로 리다이렉트 (JSON 대신)
				log.warn("OAuth2 연동 콜백 에러: provider={}, code={}, message={}",
					provider, e.getErrorCode().getCode(), e.getMessage());

				return buildErrorRedirect("link", e.getErrorCode().getCode(), e.getMessage());
			}
		}

		// login 모드: 소셜 로그인 (기본)
		try {
			// 클라이언트 정보 추출
			String ipAddress = request.getRemoteAddr();
			String userAgent = request.getHeader("User-Agent");

			// 콜백 처리 → 토큰 교환 → 사용자 조회/생성 → 결과 분기
			OAuth2LoginResultDto result = oauth2Service.processCallback(provider, code, state, ipAddress, userAgent);

			String redirectUrl;

			if ("SUCCESS".equals(result.getType())) {
				// 성공: 토큰과 함께 성공 페이지로 리다이렉트
				redirectUrl = "http://localhost:3020/auth/oauth2/success"
					+ "?token=" + URLEncoder.encode(result.getAccessToken(), StandardCharsets.UTF_8)
					+ "&refresh=" + URLEncoder.encode(result.getRefreshToken(), StandardCharsets.UTF_8)
					+ "&username=" + URLEncoder.encode(result.getUsername(), StandardCharsets.UTF_8);

				log.info("OAuth2 콜백 성공 리다이렉트: provider={}", provider);
			} else {
				// 연동 대기: 연동 확인 페이지로 리다이렉트
				redirectUrl = "http://localhost:3020/auth/oauth2/link-confirm"
					+ "?pendingId=" + URLEncoder.encode(result.getPendingId(), StandardCharsets.UTF_8)
					+ "&email=" + URLEncoder.encode(result.getEmail(), StandardCharsets.UTF_8)
					+ "&provider=" + URLEncoder.encode(result.getProviderCode(), StandardCharsets.UTF_8)
					+ "&providerName=" + URLEncoder.encode(result.getProviderName(), StandardCharsets.UTF_8);

				log.info("OAuth2 콜백 연동 대기 리다이렉트: provider={}, email={}", provider, result.getEmail());
			}

			return ResponseEntity.status(302)
				.location(URI.create(redirectUrl))
				.build();
		} catch (BusinessException e) {
			// 로그인 에러 시 프론트엔드 에러 페이지로 리다이렉트 (JSON 대신)
			log.warn("OAuth2 로그인 콜백 에러: provider={}, code={}, message={}",
				provider, e.getErrorCode().getCode(), e.getMessage());

			return buildErrorRedirect("login", e.getErrorCode().getCode(), e.getMessage());
		}
	}

	// OAuth2 콜백 에러 시 프론트엔드 에러 페이지로 리다이렉트 URL 생성
	// mode: "login" 또는 "link" (에러 발생 맥락)
	private ResponseEntity<Void> buildErrorRedirect(String mode, String errorCode, String message) {
		// 프론트엔드 에러 페이지로 리다이렉트
		String redirectUrl = "http://localhost:3020/auth/oauth2/error"
			+ "?mode=" + URLEncoder.encode(mode, StandardCharsets.UTF_8)
			+ "&code=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8)
			+ "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

		return ResponseEntity.status(302)
			.location(URI.create(redirectUrl))
			.build();
	}

	// 소셜 연동 확인 API (로컬 ID/PW 검증 후 연동 완료 + JWT 발급)
	@PostMapping("/link-confirm")
	public ResponseEntity<ApiResponseDto<LoginResponseDto>> confirmLink(
		@Valid @RequestBody OAuth2LinkConfirmRequestDto request,
		HttpServletRequest httpRequest) {

		// 클라이언트 IP, User-Agent 추출
		String ipAddress = httpRequest.getRemoteAddr();
		String userAgent = httpRequest.getHeader("User-Agent");

		// 연동 확인 처리 → JWT 발급
		LoginResponseDto response = oauth2Service.confirmLink(
			request.getPendingId(), request.getUserId(), request.getPassword(),
			ipAddress, userAgent);

		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// ===========================
	// 마이페이지 소셜 연동 관리 (인증 필요)
	// ===========================

	// 현재 사용자의 소셜 연동 목록 조회
	@GetMapping("/identities")
	public ResponseEntity<ApiResponseDto<List<UserIdentityResponseDto>>> getIdentities(
		Authentication authentication) {

		// 현재 사용자 PK
		String userPk = authentication.getName();

		// 소셜 연동 목록 조회
		List<UserIdentityResponseDto> identities = oauth2Service.getUserIdentities(userPk);

		return ResponseEntity.ok(ApiResponseDto.ok(identities));
	}

	// 마이페이지 소셜 연동용 Authorization URL 생성 (인증 필요)
	// 정책: 소셜 전용 사용자는 비밀번호 설정 후에만 연동 추가 가능
	@GetMapping("/link/{provider}")
	public ResponseEntity<ApiResponseDto<String>> linkProvider(
		@PathVariable("provider") String provider,
		Authentication authentication) {

		// 현재 사용자 PK
		String userPk = authentication.getName();

		// 연동용 Authorization URL 생성 (state에 userPk 포함)
		String authorizationUrl = oauth2Service.getLinkAuthorizationUrl(provider, userPk);

		return ResponseEntity.ok(ApiResponseDto.ok(authorizationUrl));
	}

	// 소셜 연동 해제 (인증 필요)
	// 정책: 소셜 전용 사용자는 연동 해제 불가 (비밀번호 설정 필요)
	@DeleteMapping("/identities/{identityId}")
	public ResponseEntity<ApiResponseDto<Void>> unlinkProvider(
		@PathVariable("identityId") String identityId,
		Authentication authentication) {

		// 현재 사용자 PK
		String userPk = authentication.getName();

		// 연동 해제 처리
		oauth2Service.unlinkProvider(userPk, identityId);

		return ResponseEntity.ok(ApiResponseDto.ok(null));
	}

	// 소셜 전용 사용자의 로컬 자격증명 설정 (인증 필요)
	// 로컬 ID + 비밀번호 설정 후 소셜 연동 추가/해제가 가능해진다
	@PostMapping("/set-password")
	public ResponseEntity<ApiResponseDto<Void>> setPassword(
		@Valid @RequestBody SetPasswordRequestDto request,
		Authentication authentication) {

		// 현재 사용자 PK
		String userPk = authentication.getName();

		// 로컬 자격증명 설정
		oauth2Service.setPassword(userPk, request);

		return ResponseEntity.ok(ApiResponseDto.ok(null));
	}
}
