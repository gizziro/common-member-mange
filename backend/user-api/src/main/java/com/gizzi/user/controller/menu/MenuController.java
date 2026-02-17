package com.gizzi.user.controller.menu;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.menu.dto.MenuResponseDto;
import com.gizzi.core.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 사용자 메뉴 조회 API 컨트롤러
// 현재 사용자의 권한에 따라 필터링된 메뉴 트리를 반환한다
@Slf4j
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

	// 메뉴 서비스
	private final MenuService menuService;

	// 현재 사용자 권한 기반 메뉴 트리 조회
	@GetMapping("/me")
	public ResponseEntity<ApiResponseDto<List<MenuResponseDto>>> getMyMenu(
			Authentication authentication) {
		// 인증된 사용자 ID 추출 (JWT의 sub 클레임 = userPk)
		String userId = authentication != null ? authentication.getName() : null;
		List<MenuResponseDto> tree = menuService.getVisibleMenuTree(userId);
		return ResponseEntity.ok(ApiResponseDto.ok(tree));
	}
}
