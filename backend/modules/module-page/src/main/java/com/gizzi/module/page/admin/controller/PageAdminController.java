package com.gizzi.module.page.admin.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.module.page.dto.CreatePageRequestDto;
import com.gizzi.module.page.dto.PageListResponseDto;
import com.gizzi.module.page.dto.PageResponseDto;
import com.gizzi.module.page.dto.UpdatePageRequestDto;
import com.gizzi.module.page.service.PageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 페이지 관리 컨트롤러 (admin-api에서만 활성화)
// app.api-type=admin 일 때만 빈으로 등록된다
@Slf4j
@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "admin")
public class PageAdminController {

	// 페이지 서비스
	private final PageService pageService;

	// 페이지 생성
	@PostMapping
	public ResponseEntity<ApiResponseDto<PageResponseDto>> createPage(
			@Valid @RequestBody CreatePageRequestDto request,
			Authentication authentication) {
		// 인증된 사용자 ID를 생성자로 설정
		String createdBy = authentication.getName();
		PageResponseDto response = pageService.createPage(request, createdBy);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(response));
	}

	// 페이지 목록 조회
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<PageListResponseDto>>> getPages() {
		List<PageListResponseDto> pages = pageService.getPages();
		return ResponseEntity.ok(ApiResponseDto.ok(pages));
	}

	// 페이지 상세 조회
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponseDto<PageResponseDto>> getPage(@PathVariable String id) {
		PageResponseDto response = pageService.getPage(id);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 페이지 수정
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDto<PageResponseDto>> updatePage(
			@PathVariable String id,
			@Valid @RequestBody UpdatePageRequestDto request,
			Authentication authentication) {
		String updatedBy = authentication.getName();
		PageResponseDto response = pageService.updatePage(id, request, updatedBy);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 페이지 삭제
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponseDto<Void>> deletePage(@PathVariable String id) {
		pageService.deletePage(id);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// 공개/비공개 토글
	@PatchMapping("/{id}/publish")
	public ResponseEntity<ApiResponseDto<PageResponseDto>> togglePublish(@PathVariable String id) {
		PageResponseDto response = pageService.togglePublish(id);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
