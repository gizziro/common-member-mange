package com.gizzi.module.board.admin.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.module.board.dto.category.CategoryResponseDto;
import com.gizzi.module.board.dto.category.CreateCategoryRequestDto;
import com.gizzi.module.board.dto.category.UpdateCategoryRequestDto;
import com.gizzi.module.board.service.BoardCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 게시판 카테고리 관리 컨트롤러 (admin-api에서만 활성화)
// app.api-type=admin 일 때만 빈으로 등록된다
// 게시판 인스턴스 내 카테고리 CRUD API를 제공한다
@Slf4j
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "admin")
public class BoardAdminCategoryController {

	// 카테고리 서비스
	private final BoardCategoryService categoryService;

	// ─── 카테고리 CRUD ───

	// 카테고리 목록 조회 (정렬 순서 오름차순)
	@GetMapping("/{id}/categories")
	public ResponseEntity<ApiResponseDto<List<CategoryResponseDto>>> getCategories(
			@PathVariable String id) {
		// 게시판 인스턴스의 전체 카테고리 목록 조회
		List<CategoryResponseDto> categories = categoryService.getCategories(id);
		return ResponseEntity.ok(ApiResponseDto.ok(categories));
	}

	// 카테고리 생성
	@PostMapping("/{id}/categories")
	public ResponseEntity<ApiResponseDto<CategoryResponseDto>> createCategory(
			@PathVariable String id,
			@Valid @RequestBody CreateCategoryRequestDto request) {
		// 게시판 인스턴스에 카테고리 생성 (슬러그 중복 확인 포함)
		CategoryResponseDto response = categoryService.createCategory(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(response));
	}

	// 카테고리 수정
	@PutMapping("/{id}/categories/{categoryId}")
	public ResponseEntity<ApiResponseDto<CategoryResponseDto>> updateCategory(
			@PathVariable String id,
			@PathVariable String categoryId,
			@Valid @RequestBody UpdateCategoryRequestDto request) {
		// 카테고리 정보 수정 (슬러그 변경 시 중복 확인)
		CategoryResponseDto response = categoryService.updateCategory(id, categoryId, request);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 카테고리 삭제
	@DeleteMapping("/{id}/categories/{categoryId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteCategory(
			@PathVariable String id,
			@PathVariable String categoryId) {
		// 카테고리 삭제 (해당 카테고리의 게시글은 categoryId가 null로 변경됨)
		categoryService.deleteCategory(categoryId);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}
}
