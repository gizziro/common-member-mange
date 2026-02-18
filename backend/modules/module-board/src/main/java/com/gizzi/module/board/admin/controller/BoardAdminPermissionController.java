package com.gizzi.module.board.admin.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.module.dto.InstancePermissionDto;
import com.gizzi.core.module.dto.SetPermissionsRequestDto;
import com.gizzi.core.module.dto.SetUserPermissionsRequestDto;
import com.gizzi.core.module.dto.UserInstancePermissionDto;
import com.gizzi.core.module.service.ModulePermissionService;
import com.gizzi.module.board.dto.admin.AddBoardAdminRequestDto;
import com.gizzi.module.board.dto.admin.BoardAdminResponseDto;
import com.gizzi.module.board.service.BoardAdminService;
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
import java.util.Map;

// 게시판 권한 및 부관리자 관리 컨트롤러 (admin-api에서만 활성화)
// app.api-type=admin 일 때만 빈으로 등록된다
// 게시판 모듈 권한 정의 조회 + 인스턴스별 그룹/사용자 권한 관리 + 부관리자 관리 API를 제공한다
@Slf4j
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "admin")
public class BoardAdminPermissionController {

	// 게시판 모듈 코드 상수
	private static final String MODULE_CODE = "board";

	// 모듈 인스턴스 권한 서비스 (core 모듈)
	private final ModulePermissionService permissionService;

	// 게시판 부관리자 서비스
	private final BoardAdminService       adminService;

	// ─── 권한 정의 ───

	// 게시판 모듈에서 사용 가능한 권한 정의 목록 조회
	@GetMapping("/permission-definitions")
	public ResponseEntity<ApiResponseDto<List<Map<String, String>>>> getPermissionDefinitions() {
		// 게시판 모듈의 전체 권한 정의 (resource + action + name + flatCode)
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

	// ─── 그룹별 권한 관리 ───

	// 게시판 인스턴스의 그룹별 권한 현황 조회
	@GetMapping("/{id}/permissions")
	public ResponseEntity<ApiResponseDto<List<InstancePermissionDto>>> getPermissions(
			@PathVariable String id) {
		// boardId가 곧 moduleInstanceId
		List<InstancePermissionDto> permissions = permissionService.getGroupPermissions(id);
		return ResponseEntity.ok(ApiResponseDto.ok(permissions));
	}

	// 게시판 인스턴스의 그룹별 권한 일괄 설정
	@PutMapping("/{id}/permissions")
	public ResponseEntity<ApiResponseDto<Void>> setPermissions(
			@PathVariable String id,
			@Valid @RequestBody SetPermissionsRequestDto request) {
		// boardId가 곧 moduleInstanceId
		permissionService.setGroupPermissions(id, request);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 사용자별 권한 관리 ───

	// 게시판 인스턴스의 개별 사용자 권한 현황 조회
	@GetMapping("/{id}/user-permissions")
	public ResponseEntity<ApiResponseDto<List<UserInstancePermissionDto>>> getUserPermissions(
			@PathVariable String id) {
		// boardId가 곧 moduleInstanceId
		List<UserInstancePermissionDto> permissions = permissionService.getUserPermissions(id);
		return ResponseEntity.ok(ApiResponseDto.ok(permissions));
	}

	// 게시판 인스턴스의 개별 사용자 권한 일괄 설정
	@PutMapping("/{id}/user-permissions")
	public ResponseEntity<ApiResponseDto<Void>> setUserPermissions(
			@PathVariable String id,
			@Valid @RequestBody SetUserPermissionsRequestDto request) {
		// boardId가 곧 moduleInstanceId
		permissionService.setUserPermissions(id, request);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 부관리자 관리 ───

	// 게시판 인스턴스의 부관리자 목록 조회
	@GetMapping("/{id}/admins")
	public ResponseEntity<ApiResponseDto<List<BoardAdminResponseDto>>> getAdmins(
			@PathVariable String id) {
		// 게시판 부관리자 목록 (사용자명 포함)
		List<BoardAdminResponseDto> admins = adminService.getAdmins(id);
		return ResponseEntity.ok(ApiResponseDto.ok(admins));
	}

	// 게시판 부관리자 추가
	@PostMapping("/{id}/admins")
	public ResponseEntity<ApiResponseDto<BoardAdminResponseDto>> addAdmin(
			@PathVariable String id,
			@Valid @RequestBody AddBoardAdminRequestDto request) {
		// 부관리자 추가 (사용자 존재 확인 + 중복 확인 포함)
		BoardAdminResponseDto response = adminService.addAdmin(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(response));
	}

	// 게시판 부관리자 삭제
	@DeleteMapping("/{id}/admins/{userId}")
	public ResponseEntity<ApiResponseDto<Void>> removeAdmin(
			@PathVariable String id,
			@PathVariable String userId) {
		// 부관리자 삭제 (존재 확인 후 삭제)
		adminService.removeAdmin(id, userId);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}
}
