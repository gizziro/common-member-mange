package com.gizzi.user.controller.resolve;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.module.SlugResolver;
import com.gizzi.core.module.dto.ResolveResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Slug 기반 동적 라우팅 Resolve API 컨트롤러
// 프론트엔드 URL의 slug를 모듈/인스턴스 정보 + 사용자 권한으로 변환한다
@Slf4j
@RestController
@RequestMapping("/resolve")
@RequiredArgsConstructor
public class ResolveController {

	// Slug 해석 서비스
	private final SlugResolver slugResolver;

	// SINGLE 모듈 slug 해석
	// GET /resolve/{moduleSlug}
	@GetMapping("/{moduleSlug}")
	public ResponseEntity<ApiResponseDto<ResolveResponseDto>> resolveSingle(
			@PathVariable String moduleSlug,
			Authentication authentication) {
		// 인증된 사용자 ID 추출
		String userId = authentication != null ? authentication.getName() : null;
		ResolveResponseDto response = slugResolver.resolveSingle(moduleSlug, userId);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// MULTI 모듈 slug 해석
	// GET /resolve/{moduleSlug}/{instanceSlug}
	@GetMapping("/{moduleSlug}/{instanceSlug}")
	public ResponseEntity<ApiResponseDto<ResolveResponseDto>> resolveMulti(
			@PathVariable String moduleSlug,
			@PathVariable String instanceSlug,
			Authentication authentication) {
		// 인증된 사용자 ID 추출
		String userId = authentication != null ? authentication.getName() : null;
		ResolveResponseDto response = slugResolver.resolveMulti(moduleSlug, instanceSlug, userId);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
