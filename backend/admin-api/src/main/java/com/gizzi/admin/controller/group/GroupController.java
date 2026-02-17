package com.gizzi.admin.controller.group;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.group.dto.AddGroupMemberRequestDto;
import com.gizzi.core.domain.group.dto.CreateGroupRequestDto;
import com.gizzi.core.domain.group.dto.GroupMemberResponseDto;
import com.gizzi.core.domain.group.dto.GroupResponseDto;
import com.gizzi.core.domain.group.dto.UpdateGroupRequestDto;
import com.gizzi.core.domain.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 관리자 그룹 관리 API 컨트롤러
// 그룹 CRUD + 멤버 관리 기능을 제공한다
@Slf4j
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

	// 그룹 서비스
	private final GroupService groupService;

	// 그룹 생성 API
	@PostMapping
	public ResponseEntity<ApiResponseDto<GroupResponseDto>> createGroup(
			@Valid @RequestBody CreateGroupRequestDto request,
			Authentication authentication) {
		// 인증된 사용자 PK를 소유자로 설정
		String ownerUserId = authentication.getName();

		// 그룹 생성 서비스 호출
		GroupResponseDto response = groupService.createGroup(request, ownerUserId);

		// 201 Created 응답 반환
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponseDto.ok(response));
	}

	// 전체 그룹 목록 조회 API
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<GroupResponseDto>>> getAllGroups() {
		// 전체 그룹 목록 서비스 호출
		List<GroupResponseDto> response = groupService.getAllGroups();

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 그룹 상세 조회 API
	@GetMapping("/{groupId}")
	public ResponseEntity<ApiResponseDto<GroupResponseDto>> getGroup(
			@PathVariable String groupId) {
		// 그룹 단건 조회 서비스 호출
		GroupResponseDto response = groupService.getGroup(groupId);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 그룹 수정 API
	@PutMapping("/{groupId}")
	public ResponseEntity<ApiResponseDto<GroupResponseDto>> updateGroup(
			@PathVariable String groupId,
			@Valid @RequestBody UpdateGroupRequestDto request) {
		// 그룹 수정 서비스 호출
		GroupResponseDto response = groupService.updateGroup(groupId, request);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 그룹 삭제 API
	@DeleteMapping("/{groupId}")
	public ResponseEntity<Void> deleteGroup(@PathVariable String groupId) {
		// 그룹 삭제 서비스 호출
		groupService.deleteGroup(groupId);

		// 204 No Content 응답 반환
		return ResponseEntity.noContent().build();
	}

	// 그룹 멤버 목록 조회 API
	@GetMapping("/{groupId}/members")
	public ResponseEntity<ApiResponseDto<List<GroupMemberResponseDto>>> getGroupMembers(
			@PathVariable String groupId) {
		// 그룹 멤버 목록 서비스 호출
		List<GroupMemberResponseDto> response = groupService.getGroupMembers(groupId);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 그룹 멤버 추가 API (로그인 ID 기반)
	@PostMapping("/{groupId}/members")
	public ResponseEntity<ApiResponseDto<Void>> addMember(
			@PathVariable String groupId,
			@Valid @RequestBody AddGroupMemberRequestDto request) {
		// 멤버 추가 서비스 호출 (로그인 ID 전달)
		groupService.addMember(groupId, request.getUserId());

		// 201 Created 응답 반환
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponseDto.ok());
	}

	// 그룹 멤버 제거 API (로그인 ID 기반)
	@DeleteMapping("/{groupId}/members/{loginId}")
	public ResponseEntity<Void> removeMember(
			@PathVariable String groupId,
			@PathVariable String loginId) {
		// 멤버 제거 서비스 호출 (로그인 ID 전달)
		groupService.removeMember(groupId, loginId);

		// 204 No Content 응답 반환
		return ResponseEntity.noContent().build();
	}
}
