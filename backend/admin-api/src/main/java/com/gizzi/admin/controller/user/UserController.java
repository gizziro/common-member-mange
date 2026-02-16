package com.gizzi.admin.controller.user;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.dto.PageResponseDto;
import com.gizzi.core.domain.auth.dto.UserIdentityResponseDto;
import com.gizzi.core.domain.auth.repository.UserIdentityRepository;
import com.gizzi.core.domain.group.dto.GroupResponseDto;
import com.gizzi.core.domain.group.service.GroupService;
import com.gizzi.core.domain.user.dto.ChangePasswordRequestDto;
import com.gizzi.core.domain.user.dto.UpdateUserRequestDto;
import com.gizzi.core.domain.user.dto.UserListResponseDto;
import com.gizzi.core.domain.user.dto.UserResponseDto;
import com.gizzi.core.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 관리자 사용자 관리 API 컨트롤러
// 사용자 목록 조회, 상세 조회, 수정, 잠금 해제, 삭제 기능을 제공한다
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	// 사용자 서비스
	private final UserService             userService;

	// 그룹 서비스 (소속 그룹 조회용)
	private final GroupService            groupService;

	// 소셜 연동 리포지토리 (소셜 계정 연동 조회용)
	private final UserIdentityRepository  userIdentityRepository;

	// 사용자 목록 조회 API (페이지네이션)
	@GetMapping
	public ResponseEntity<ApiResponseDto<PageResponseDto<UserListResponseDto>>> getUsers(
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		// 페이지네이션된 사용자 목록 조회
		Page<UserListResponseDto> page = userService.getUsers(pageable);

		// PageResponseDto로 래핑하여 응답
		PageResponseDto<UserListResponseDto> response = PageResponseDto.from(page, dto -> dto);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 사용자 상세 조회 API
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponseDto<UserResponseDto>> getUser(
			@PathVariable String id) {
		// 사용자 단건 조회
		UserResponseDto response = userService.getUser(id);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 사용자 정보 수정 API
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDto<UserResponseDto>> updateUser(
			@PathVariable String id,
			@Valid @RequestBody UpdateUserRequestDto request) {
		// 사용자 정보 수정
		UserResponseDto response = userService.updateUser(id, request);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 사용자 잠금 해제 API
	@PutMapping("/{id}/unlock")
	public ResponseEntity<ApiResponseDto<UserResponseDto>> unlockUser(
			@PathVariable String id) {
		// 계정 잠금 해제
		UserResponseDto response = userService.unlockUser(id);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 관리자 비밀번호 변경 API
	@PutMapping("/{id}/password")
	public ResponseEntity<ApiResponseDto<Void>> changePassword(
			@PathVariable String id,
			@Valid @RequestBody ChangePasswordRequestDto request) {
		// 비밀번호 변경 처리
		userService.changePassword(id, request);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(null));
	}

	// 사용자 삭제 API (자기 자신 삭제 불가)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(
			@PathVariable String id,
			Authentication authentication) {
		// 현재 로그인한 사용자 PK를 전달하여 자기 자신 삭제 방지
		String currentUserPk = authentication.getName();
		userService.deleteUser(id, currentUserPk);

		// 204 No Content 응답 반환
		return ResponseEntity.noContent().build();
	}

	// 사용자 소속 그룹 조회 API
	@GetMapping("/{id}/groups")
	public ResponseEntity<ApiResponseDto<List<GroupResponseDto>>> getUserGroups(
			@PathVariable String id) {
		// 사용자 소속 그룹 목록 조회
		List<GroupResponseDto> response = groupService.getUserGroups(id);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 사용자 소셜 연동 조회 API
	@GetMapping("/{id}/identities")
	public ResponseEntity<ApiResponseDto<List<UserIdentityResponseDto>>> getUserIdentities(
			@PathVariable String id) {
		// 사용자의 소셜 연동 목록 조회 → DTO 변환
		List<UserIdentityResponseDto> response = userIdentityRepository.findByUserId(id)
			.stream()
			.map(UserIdentityResponseDto::from)
			.toList();

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
