package com.gizzi.user.controller.group;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.group.dto.GroupResponseDto;
import com.gizzi.core.domain.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 사용자 그룹 조회 API 컨트롤러
// 현재 로그인한 사용자의 소속 그룹 목록을 조회한다
@Slf4j
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

	// 그룹 서비스
	private final GroupService groupService;

	// 내 소속 그룹 목록 조회 API
	@GetMapping("/me")
	public ResponseEntity<ApiResponseDto<List<GroupResponseDto>>> getMyGroups(
			Authentication authentication) {
		// 인증된 사용자 PK로 소속 그룹 조회
		String userPk = authentication.getName();

		// 사용자 소속 그룹 목록 서비스 호출
		List<GroupResponseDto> response = groupService.getUserGroups(userPk);

		// 200 OK 응답 반환
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
