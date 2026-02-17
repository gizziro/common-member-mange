package com.gizzi.user.controller.menu;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.menu.dto.MenuResponseDto;
import com.gizzi.core.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 사용자 메뉴 조회 API 컨트롤러
// 모든 사용자에게 보이는 메뉴 트리를 반환한다 (인증 불필요)
// 권한 필터링은 없으며, is_visible 플래그만으로 제어한다
@Slf4j
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

	// 메뉴 서비스
	private final MenuService menuService;

	// 보이는 메뉴 트리 조회 (인증 불필요, 전체 공개)
	@GetMapping("/me")
	public ResponseEntity<ApiResponseDto<List<MenuResponseDto>>> getMyMenu() {
		List<MenuResponseDto> tree = menuService.getVisibleMenuTree();
		return ResponseEntity.ok(ApiResponseDto.ok(tree));
	}
}
