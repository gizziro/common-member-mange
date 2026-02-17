package com.gizzi.module.page.admin.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.module.dto.InstancePermissionDto;
import com.gizzi.core.module.dto.SetPermissionsRequestDto;
import com.gizzi.core.module.dto.SetUserPermissionsRequestDto;
import com.gizzi.core.module.dto.UserInstancePermissionDto;
import com.gizzi.core.module.service.ModulePermissionService;
import com.gizzi.module.page.dto.CreatePageRequestDto;
import com.gizzi.module.page.dto.PageListResponseDto;
import com.gizzi.module.page.dto.PageResponseDto;
import com.gizzi.module.page.dto.UpdatePageRequestDto;
import com.gizzi.module.page.exception.PageErrorCode;
import com.gizzi.module.page.repository.PageRepository;
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
import java.util.Map;

// 페이지 관리 컨트롤러 (admin-api에서만 활성화)
// app.api-type=admin 일 때만 빈으로 등록된다
// 페이지 CRUD + 공개/비공개 + 페이지별 권한 관리 API를 제공한다
@Slf4j
@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "admin")
public class PageAdminController {

	// 페이지 모듈 코드 상수
	private static final String MODULE_CODE = "page";

	// 페이지 서비스
	private final PageService              pageService;

	// 모듈 인스턴스 권한 서비스
	private final ModulePermissionService  permissionService;

	// 페이지 리포지토리 (moduleInstanceId 조회용)
	private final PageRepository           pageRepository;

	// ─── 페이지 CRUD ───

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

	// ─── 페이지별 권한 관리 API ───

	// 페이지 모듈에서 사용 가능한 권한 정의 목록 조회
	@GetMapping("/permission-definitions")
	public ResponseEntity<ApiResponseDto<List<Map<String, String>>>> getPermissionDefinitions() {
		List<Map<String, String>> permissions = permissionService.getAvailablePermissions(MODULE_CODE)
				.stream()
				.map(perm -> Map.of(
						"id", perm.getId(),
						"resource", perm.getResource(),
						"action", perm.getAction(),
						"name", perm.getName(),
						"flatCode", perm.toFlatPermissionString()
				))
				.toList();
		return ResponseEntity.ok(ApiResponseDto.ok(permissions));
	}

	// 특정 페이지의 그룹별 권한 현황 조회
	@GetMapping("/{id}/permissions")
	public ResponseEntity<ApiResponseDto<List<InstancePermissionDto>>> getPagePermissions(
			@PathVariable String id) {
		// 페이지에서 moduleInstanceId 추출
		String instanceId = getModuleInstanceId(id);
		List<InstancePermissionDto> permissions = permissionService.getGroupPermissions(instanceId);
		return ResponseEntity.ok(ApiResponseDto.ok(permissions));
	}

	// 특정 페이지의 그룹별 권한 일괄 설정
	@PutMapping("/{id}/permissions")
	public ResponseEntity<ApiResponseDto<Void>> setPagePermissions(
			@PathVariable String id,
			@Valid @RequestBody SetPermissionsRequestDto request) {
		// 페이지에서 moduleInstanceId 추출
		String instanceId = getModuleInstanceId(id);
		permissionService.setGroupPermissions(instanceId, request);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 페이지별 사용자 권한 관리 API ───

	// 특정 페이지의 개별 사용자 권한 현황 조회
	@GetMapping("/{id}/user-permissions")
	public ResponseEntity<ApiResponseDto<List<UserInstancePermissionDto>>> getPageUserPermissions(
			@PathVariable String id) {
		// 페이지에서 moduleInstanceId 추출
		String instanceId = getModuleInstanceId(id);
		List<UserInstancePermissionDto> permissions = permissionService.getUserPermissions(instanceId);
		return ResponseEntity.ok(ApiResponseDto.ok(permissions));
	}

	// 특정 페이지의 개별 사용자 권한 일괄 설정
	@PutMapping("/{id}/user-permissions")
	public ResponseEntity<ApiResponseDto<Void>> setPageUserPermissions(
			@PathVariable String id,
			@Valid @RequestBody SetUserPermissionsRequestDto request) {
		// 페이지에서 moduleInstanceId 추출
		String instanceId = getModuleInstanceId(id);
		permissionService.setUserPermissions(instanceId, request);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 헬퍼 메서드 ───

	// 페이지 ID에서 연결된 모듈 인스턴스 ID 추출
	private String getModuleInstanceId(String pageId) {
		return pageRepository.findById(pageId)
				.map(page -> {
					// moduleInstanceId가 없으면 에러
					if (page.getModuleInstanceId() == null) {
						throw new BusinessException(PageErrorCode.PAGE_NOT_FOUND);
					}
					return page.getModuleInstanceId();
				})
				.orElseThrow(() -> new BusinessException(PageErrorCode.PAGE_NOT_FOUND));
	}
}
