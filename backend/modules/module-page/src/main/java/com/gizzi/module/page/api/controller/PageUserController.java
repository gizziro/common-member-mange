package com.gizzi.module.page.api.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.module.page.dto.PageListResponseDto;
import com.gizzi.module.page.dto.PageResponseDto;
import com.gizzi.module.page.service.PageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 사용자 페이지 조회 컨트롤러 (user-api에서만 활성화)
// 공개된 페이지만 조회 가능
@Slf4j
@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "user")
public class PageUserController {

	// 페이지 서비스
	private final PageService pageService;

	// slug로 공개 페이지 조회
	@GetMapping("/{slug}")
	public ResponseEntity<ApiResponseDto<PageResponseDto>> getPageBySlug(
			@PathVariable String slug) {
		PageResponseDto response = pageService.getPageBySlug(slug);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 공개 페이지 목록 조회
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<PageListResponseDto>>> getPublishedPages() {
		List<PageListResponseDto> pages = pageService.getPublishedPages();
		return ResponseEntity.ok(ApiResponseDto.ok(pages));
	}
}
