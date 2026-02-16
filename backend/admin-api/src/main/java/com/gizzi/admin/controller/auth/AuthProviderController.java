package com.gizzi.admin.controller.auth;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.auth.dto.AuthProviderResponseDto;
import com.gizzi.core.domain.auth.dto.UpdateAuthProviderRequestDto;
import com.gizzi.core.domain.auth.service.AuthProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 인증 제공자 관리 API 컨트롤러 (관리자 전용)
// Provider 목록 조회, 상세 조회, 설정 수정을 담당한다
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/providers")
public class AuthProviderController {

	// 인증 제공자 관리 서비스
	private final AuthProviderService authProviderService;

	// 전체 Provider 목록 조회
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<AuthProviderResponseDto>>> getProviders() {
		// 모든 인증 제공자 목록 반환
		List<AuthProviderResponseDto> providers = authProviderService.getAllProviders();
		return ResponseEntity.ok(ApiResponseDto.ok(providers));
	}

	// Provider 상세 조회
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponseDto<AuthProviderResponseDto>> getProvider(
		@PathVariable("id") String id) {
		// PK로 상세 정보 조회
		AuthProviderResponseDto provider = authProviderService.getProvider(id);
		return ResponseEntity.ok(ApiResponseDto.ok(provider));
	}

	// Provider 설정 수정
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDto<AuthProviderResponseDto>> updateProvider(
		@PathVariable("id") String id,
		@Valid @RequestBody UpdateAuthProviderRequestDto request) {
		// 설정 수정 후 응답 반환
		AuthProviderResponseDto provider = authProviderService.updateProvider(id, request);
		return ResponseEntity.ok(ApiResponseDto.ok(provider));
	}
}
