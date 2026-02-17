package com.gizzi.module.page.api.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.module.page.dto.PageListResponseDto;
import com.gizzi.module.page.dto.PageResponseDto;
import com.gizzi.module.page.service.PageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 사용자 페이지 조회 컨트롤러 (user-api에서만 활성화)
// 공개된 페이지 + 권한 기반 필터링으로 페이지를 조회한다
// /pages/** 는 SecurityConfig에서 permitAll이므로 비로그인 사용자도 접근 가능
// Authentication이 null이면 비로그인, 있으면 userId로 권한 체크
@Slf4j
@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "user")
public class PageUserController {

	// 페이지 서비스
	private final PageService pageService;

	// slug로 공개 페이지 조회 (권한 체크 포함)
	@GetMapping("/{slug}")
	public ResponseEntity<ApiResponseDto<PageResponseDto>> getPageBySlug(
			@PathVariable String slug,
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인 시 null)
		String userId = extractUserId(authentication);
		PageResponseDto response = pageService.getPublishedPage(slug, userId);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 공개 페이지 목록 조회 (권한 필터링 포함)
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<PageListResponseDto>>> getPublishedPages(
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인 시 null)
		String userId = extractUserId(authentication);
		List<PageListResponseDto> pages = pageService.getPublishedPages(userId);
		return ResponseEntity.ok(ApiResponseDto.ok(pages));
	}

	// Authentication에서 userId 추출 (null 안전)
	private String extractUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		return authentication.getName();
	}
}
