package com.gizzi.module.board.api.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.module.board.dto.category.CategoryResponseDto;
import com.gizzi.module.board.dto.settings.BoardSettingsResponseDto;
import com.gizzi.module.board.dto.tag.TagResponseDto;
import com.gizzi.module.board.service.BoardCategoryService;
import com.gizzi.module.board.service.BoardInstanceService;
import com.gizzi.module.board.service.BoardTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 사용자 게시판 설정/카테고리/태그 조회 컨트롤러 (user-api에서만 활성화)
// 프론트엔드에서 게시판 설정을 기반으로 UI를 동적 구성할 수 있도록 조회 API를 제공한다
@Slf4j
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "user")
public class BoardUserSettingsController {

	// 게시판 인스턴스 서비스 (설정 조회 위임)
	private final BoardInstanceService instanceService;

	// 카테고리 서비스
	private final BoardCategoryService categoryService;

	// 태그 서비스
	private final BoardTagService      tagService;

	// ─── 게시판 설정 조회 ───

	// 게시판 설정 조회 (프론트엔드 UI 동적 구성용)
	@GetMapping("/{id}/settings")
	public ResponseEntity<ApiResponseDto<BoardSettingsResponseDto>> getSettings(
			@PathVariable String id) {
		// 설정 조회 위임
		BoardSettingsResponseDto settings = instanceService.getSettings(id);
		return ResponseEntity.ok(ApiResponseDto.ok(settings));
	}

	// ─── 카테고리 목록 조회 ───

	// 게시판의 카테고리 목록 조회 (활성화된 카테고리만 사용자에게 노출)
	@GetMapping("/{id}/categories")
	public ResponseEntity<ApiResponseDto<List<CategoryResponseDto>>> getCategories(
			@PathVariable String id) {
		// 카테고리 목록 조회 위임
		List<CategoryResponseDto> categories = categoryService.getCategories(id);
		return ResponseEntity.ok(ApiResponseDto.ok(categories));
	}

	// ─── 태그 목록 조회 ───

	// 게시판의 사용 중인 태그 목록 조회 (postCount > 0, 인기순)
	@GetMapping("/{id}/tags")
	public ResponseEntity<ApiResponseDto<List<TagResponseDto>>> getTags(
			@PathVariable String id) {
		// 태그 목록 조회 위임
		List<TagResponseDto> tags = tagService.getTagList(id);
		return ResponseEntity.ok(ApiResponseDto.ok(tags));
	}
}
