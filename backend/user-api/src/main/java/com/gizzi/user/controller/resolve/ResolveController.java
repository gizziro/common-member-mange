package com.gizzi.user.controller.resolve;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.exception.BusinessException;
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
// 모듈 slug 해석 실패 시 메뉴 별칭(alias) fallback을 수행한다
@Slf4j
@RestController
@RequestMapping("/resolve")
@RequiredArgsConstructor
public class ResolveController {

	// Slug 해석 서비스
	private final SlugResolver slugResolver;

	// 1-segment slug 해석
	// 먼저 SINGLE 모듈로 시도 → 실패 시 alias fallback
	// GET /resolve/{slug}
	@GetMapping("/{slug}")
	public ResponseEntity<ApiResponseDto<ResolveResponseDto>> resolveSingle(
			@PathVariable String slug,
			Authentication authentication) {
		// 인증된 사용자 ID 추출
		String userId = authentication != null ? authentication.getName() : null;

		try {
			// SINGLE 모듈 slug 해석 시도
			ResolveResponseDto response = slugResolver.resolveSingle(slug, userId);
			return ResponseEntity.ok(ApiResponseDto.ok(response));
		} catch (BusinessException e) {
			// 모듈 해석 실패 → 별칭(alias) fallback
			ResolveResponseDto alias = slugResolver.resolveByAlias(slug, null, userId);
			if (alias != null) {
				return ResponseEntity.ok(ApiResponseDto.ok(alias));
			}
			// 별칭도 없으면 원래 예외 전파
			throw e;
		}
	}

	// 2-segment slug 해석
	// 먼저 MULTI 모듈로 시도 → 실패 시 alias fallback (slug1=alias, slug2=subPath)
	// GET /resolve/{slug1}/{slug2}
	@GetMapping("/{slug1}/{slug2}")
	public ResponseEntity<ApiResponseDto<ResolveResponseDto>> resolveMulti(
			@PathVariable String slug1,
			@PathVariable String slug2,
			Authentication authentication) {
		// 인증된 사용자 ID 추출
		String userId = authentication != null ? authentication.getName() : null;

		try {
			// MULTI 모듈 slug 해석 시도
			ResolveResponseDto response = slugResolver.resolveMulti(slug1, slug2, userId);
			return ResponseEntity.ok(ApiResponseDto.ok(response));
		} catch (BusinessException e) {
			// 모듈 해석 실패 → 별칭(alias) fallback (slug1=alias, slug2=additionalSubPath)
			ResolveResponseDto alias = slugResolver.resolveByAlias(slug1, slug2, userId);
			if (alias != null) {
				return ResponseEntity.ok(ApiResponseDto.ok(alias));
			}
			// 별칭도 없으면 원래 예외 전파
			throw e;
		}
	}
}
