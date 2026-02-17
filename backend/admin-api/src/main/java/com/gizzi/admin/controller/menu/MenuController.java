package com.gizzi.admin.controller.menu;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.menu.dto.CreateMenuRequestDto;
import com.gizzi.core.domain.menu.dto.MenuResponseDto;
import com.gizzi.core.domain.menu.dto.UpdateMenuRequestDto;
import com.gizzi.core.domain.menu.service.MenuService;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.core.module.repository.ModuleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// 관리자 메뉴 관리 API 컨트롤러
// 메뉴 CRUD + 정렬 + 가시성 관리
// 권한 관리는 각 모듈 admin 컨트롤러에서 담당한다
@Slf4j
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

	// 메뉴 서비스
	private final MenuService              menuService;

	// 모듈 리포지토리
	private final ModuleRepository         moduleRepository;

	// 모듈 인스턴스 리포지토리
	private final ModuleInstanceRepository instanceRepository;

	// 메뉴 항목 생성
	@PostMapping
	public ResponseEntity<ApiResponseDto<MenuResponseDto>> createMenu(
			@Valid @RequestBody CreateMenuRequestDto request) {
		MenuResponseDto response = menuService.createMenu(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(response));
	}

	// 전체 메뉴 트리 조회
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<MenuResponseDto>>> getAllMenuTree() {
		List<MenuResponseDto> tree = menuService.getAllMenuTree();
		return ResponseEntity.ok(ApiResponseDto.ok(tree));
	}

	// 메뉴 항목 수정
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDto<MenuResponseDto>> updateMenu(
			@PathVariable String id,
			@Valid @RequestBody UpdateMenuRequestDto request) {
		MenuResponseDto response = menuService.updateMenu(id, request);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 메뉴 항목 삭제
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponseDto<Void>> deleteMenu(@PathVariable String id) {
		menuService.deleteMenu(id);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// 정렬 순서 변경
	@PatchMapping("/{id}/order")
	public ResponseEntity<ApiResponseDto<Void>> updateOrder(
			@PathVariable String id,
			@RequestParam Integer sortOrder,
			@RequestParam(required = false) String parentId) {
		menuService.updateOrder(id, sortOrder, parentId);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// 가시성 토글
	@PatchMapping("/{id}/toggle")
	public ResponseEntity<ApiResponseDto<Void>> toggleVisibility(@PathVariable String id) {
		menuService.toggleVisibility(id);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 모듈/인스턴스 목록 조회 ───

	// 등록된 전체 모듈 목록 조회 (메뉴 생성 시 모듈 선택 드롭다운용)
	@GetMapping("/modules")
	public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> getModules() {
		List<Map<String, Object>> modules = moduleRepository.findAll().stream()
				.map(m -> Map.<String, Object>of(
						"code", m.getCode(),
						"name", m.getName(),
						"slug", m.getSlug(),
						"type", m.getType().name()
				))
				.toList();
		return ResponseEntity.ok(ApiResponseDto.ok(modules));
	}

	// 특정 모듈의 인스턴스 목록 조회 (모듈 선택 후 인스턴스 드롭다운용)
	@GetMapping("/modules/{moduleCode}/instances")
	public ResponseEntity<ApiResponseDto<List<Map<String, String>>>> getModuleInstances(
			@PathVariable String moduleCode) {
		List<Map<String, String>> list = instanceRepository.findByModuleCode(moduleCode).stream()
				.map(inst -> Map.of(
						"instanceId", inst.getInstanceId(),
						"instanceName", inst.getInstanceName(),
						"slug", inst.getSlug(),
						"moduleCode", inst.getModuleCode()
				))
				.toList();
		return ResponseEntity.ok(ApiResponseDto.ok(list));
	}
}
