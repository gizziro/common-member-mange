package com.gizzi.core.domain.menu.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.MenuErrorCode;
import com.gizzi.core.domain.menu.dto.CreateMenuRequestDto;
import com.gizzi.core.domain.menu.dto.MenuResponseDto;
import com.gizzi.core.domain.menu.dto.UpdateMenuRequestDto;
import com.gizzi.core.domain.menu.entity.MenuEntity;
import com.gizzi.core.domain.menu.entity.MenuType;
import com.gizzi.core.domain.menu.repository.MenuRepository;
import com.gizzi.core.module.PermissionChecker;
import com.gizzi.core.module.entity.ModuleEntity;
import com.gizzi.core.module.entity.ModuleInstanceEntity;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.core.module.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 메뉴 관리 서비스
// 메뉴 CRUD, 트리 조회, 정렬, 사용자 권한 기반 필터링을 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

	// 최대 메뉴 깊이 (3단계까지 허용)
	private static final int MAX_DEPTH = 3;

	// 메뉴 리포지토리
	private final MenuRepository           menuRepository;

	// 모듈 인스턴스 리포지토리 (MODULE 타입 URL 생성용)
	private final ModuleInstanceRepository  instanceRepository;

	// 모듈 리포지토리 (slug 조회용)
	private final ModuleRepository          moduleRepository;

	// 권한 체크 유틸리티 (사용자 메뉴 가시성 판단)
	private final PermissionChecker         permissionChecker;

	// 메뉴 항목 생성
	@Transactional
	public MenuResponseDto createMenu(CreateMenuRequestDto request) {
		// 메뉴 유형 파싱
		MenuType menuType = parseMenuType(request.getMenuType());

		// MODULE 타입 유효성 검증
		if (menuType == MenuType.MODULE && (request.getModuleInstanceId() == null
				|| request.getModuleInstanceId().isBlank())) {
			throw new BusinessException(MenuErrorCode.MENU_MODULE_INSTANCE_REQUIRED);
		}

		// 부모 메뉴 존재 여부 검증
		if (request.getParentId() != null && !request.getParentId().isBlank()) {
			// 부모 메뉴 존재 확인
			if (!menuRepository.existsById(request.getParentId())) {
				throw new BusinessException(MenuErrorCode.MENU_PARENT_NOT_FOUND);
			}
			// 깊이 제한 확인
			int parentDepth = calculateDepth(request.getParentId());
			if (parentDepth >= MAX_DEPTH) {
				throw new BusinessException(MenuErrorCode.MENU_MAX_DEPTH_EXCEEDED);
			}
		}

		// 엔티티 생성 및 저장
		MenuEntity entity = MenuEntity.create(
				request.getName(),
				request.getIcon(),
				menuType,
				menuType == MenuType.MODULE ? request.getModuleInstanceId() : null,
				menuType == MenuType.LINK ? request.getCustomUrl() : null,
				menuType == MenuType.LINK ? request.getRequiredRole() : null,
				request.getParentId() != null && !request.getParentId().isBlank()
						? request.getParentId() : null,
				request.getSortOrder()
		);

		menuRepository.save(entity);
		log.info("메뉴 생성: {} ({})", entity.getName(), entity.getMenuType());

		return toResponseDto(entity, List.of());
	}

	// 메뉴 항목 수정
	@Transactional
	public MenuResponseDto updateMenu(String id, UpdateMenuRequestDto request) {
		// 메뉴 조회
		MenuEntity entity = menuRepository.findById(id)
				.orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

		// 메뉴 유형 파싱
		MenuType menuType = parseMenuType(request.getMenuType());

		// MODULE 타입 유효성 검증
		if (menuType == MenuType.MODULE && (request.getModuleInstanceId() == null
				|| request.getModuleInstanceId().isBlank())) {
			throw new BusinessException(MenuErrorCode.MENU_MODULE_INSTANCE_REQUIRED);
		}

		// 정보 수정
		entity.updateInfo(
				request.getName(),
				request.getIcon(),
				menuType,
				menuType == MenuType.MODULE ? request.getModuleInstanceId() : null,
				menuType == MenuType.LINK ? request.getCustomUrl() : null,
				menuType == MenuType.LINK ? request.getRequiredRole() : null
		);

		menuRepository.save(entity);
		log.info("메뉴 수정: {} ({})", entity.getName(), entity.getId());

		return toResponseDto(entity, List.of());
	}

	// 메뉴 항목 삭제 (CASCADE로 하위 메뉴도 삭제)
	@Transactional
	public void deleteMenu(String id) {
		// 메뉴 존재 확인
		MenuEntity entity = menuRepository.findById(id)
				.orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

		menuRepository.delete(entity);
		log.info("메뉴 삭제: {} ({})", entity.getName(), entity.getId());
	}

	// 정렬 순서 및 부모 변경
	@Transactional
	public void updateOrder(String id, Integer sortOrder, String parentId) {
		// 메뉴 조회
		MenuEntity entity = menuRepository.findById(id)
				.orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

		// 순환 참조 검증 (자기 자신을 부모로 지정 불가)
		if (parentId != null && parentId.equals(id)) {
			throw new BusinessException(MenuErrorCode.MENU_CIRCULAR_REFERENCE);
		}

		// 부모 메뉴 존재 확인
		if (parentId != null && !parentId.isBlank()) {
			if (!menuRepository.existsById(parentId)) {
				throw new BusinessException(MenuErrorCode.MENU_PARENT_NOT_FOUND);
			}
			// 자식을 부모로 지정하는 순환 참조 확인
			if (isDescendant(id, parentId)) {
				throw new BusinessException(MenuErrorCode.MENU_CIRCULAR_REFERENCE);
			}
		}

		entity.updateOrder(sortOrder, parentId != null && !parentId.isBlank() ? parentId : null);
		menuRepository.save(entity);
	}

	// 가시성 토글
	@Transactional
	public void toggleVisibility(String id) {
		// 메뉴 조회
		MenuEntity entity = menuRepository.findById(id)
				.orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

		entity.toggleVisibility();
		menuRepository.save(entity);
	}

	// 전체 메뉴 트리 조회 (관리자용 — 모든 메뉴 포함)
	public List<MenuResponseDto> getAllMenuTree() {
		// 전체 메뉴 로드
		List<MenuEntity> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
		return buildTree(allMenus, null, false, null);
	}

	// 사용자 권한 기반 메뉴 트리 조회
	// 가시성이 true인 메뉴만 로드하고, 권한에 따라 필터링한다
	public List<MenuResponseDto> getVisibleMenuTree(String userId) {
		// is_visible = true인 메뉴만 로드
		List<MenuEntity> visibleMenus = menuRepository.findByIsVisibleTrueOrderBySortOrderAsc();
		return buildTree(visibleMenus, null, true, userId);
	}

	// 메뉴 트리를 재귀적으로 구성
	// filterByPermission=true이면 사용자 권한 기반 필터링 적용
	private List<MenuResponseDto> buildTree(List<MenuEntity> allMenus, String parentId,
	                                         boolean filterByPermission, String userId) {
		List<MenuResponseDto> result = new ArrayList<>();

		// 현재 부모에 속하는 메뉴 필터링
		List<MenuEntity> children = allMenus.stream()
				.filter(m -> parentId == null
						? m.getParentId() == null
						: parentId.equals(m.getParentId()))
				.collect(Collectors.toList());

		for (MenuEntity menu : children) {
			// 하위 메뉴 재귀 구성
			List<MenuResponseDto> childDtos = buildTree(allMenus, menu.getId(),
					filterByPermission, userId);

			if (filterByPermission) {
				// 권한 기반 필터링 적용
				MenuResponseDto dto = buildFilteredMenuDto(menu, childDtos, userId);
				if (dto != null) {
					result.add(dto);
				}
			} else {
				// 관리자용 — 전체 표시
				result.add(toResponseDto(menu, childDtos));
			}
		}

		return result;
	}

	// 사용자 권한에 따라 메뉴 표시 여부 결정
	// MODULE: 인스턴스 권한이 하나라도 있으면 표시
	// LINK: requiredRole이 null이면 전체 공개 (현재는 역할 체크 미구현, 전체 공개)
	// SEPARATOR: 자식 중 하나라도 보이면 표시
	private MenuResponseDto buildFilteredMenuDto(MenuEntity menu,
	                                              List<MenuResponseDto> filteredChildren,
	                                              String userId) {
		switch (menu.getMenuType()) {
			case MODULE:
				// 모듈 인스턴스 권한 확인
				if (menu.getModuleInstanceId() == null) {
					return null;
				}
				Map<String, List<String>> permissions =
						permissionChecker.getPermissionMap(userId, menu.getModuleInstanceId());
				// 권한이 하나라도 있으면 표시
				if (permissions.isEmpty()) {
					return null;
				}
				return toResponseDtoWithPermissions(menu, filteredChildren, permissions);

			case LINK:
				// LINK 타입: requiredRole이 null이면 전체 공개
				// TODO: 향후 역할 기반 필터링 구현
				if (menu.getRequiredRole() != null) {
					return null;
				}
				return toResponseDto(menu, filteredChildren);

			case SEPARATOR:
				// SEPARATOR: 자식이 하나라도 보이면 표시
				if (filteredChildren.isEmpty()) {
					return null;
				}
				return toResponseDto(menu, filteredChildren);

			default:
				return null;
		}
	}

	// MenuEntity → 기본 응답 DTO 변환
	private MenuResponseDto toResponseDto(MenuEntity entity, List<MenuResponseDto> children) {
		return MenuResponseDto.builder()
				.id(entity.getId())
				.name(entity.getName())
				.icon(entity.getIcon())
				.menuType(entity.getMenuType().name())
				.url(buildUrl(entity))
				.moduleInstanceId(entity.getModuleInstanceId())
				.customUrl(entity.getCustomUrl())
				.requiredRole(entity.getRequiredRole())
				.sortOrder(entity.getSortOrder())
				.isVisible(entity.getIsVisible())
				.children(children.isEmpty() ? null : children)
				.build();
	}

	// MenuEntity → 권한 포함 응답 DTO 변환 (MODULE 타입 사용자 메뉴용)
	private MenuResponseDto toResponseDtoWithPermissions(MenuEntity entity,
	                                                      List<MenuResponseDto> children,
	                                                      Map<String, List<String>> permissions) {
		return MenuResponseDto.builder()
				.id(entity.getId())
				.name(entity.getName())
				.icon(entity.getIcon())
				.menuType(entity.getMenuType().name())
				.url(buildUrl(entity))
				.sortOrder(entity.getSortOrder())
				.permissions(permissions)
				.children(children.isEmpty() ? null : children)
				.build();
	}

	// 메뉴 URL 생성
	// MODULE: /{module-slug}/{instance-slug} (MULTI) 또는 /{module-slug} (SINGLE)
	// LINK: customUrl 반환
	// SEPARATOR: null
	private String buildUrl(MenuEntity entity) {
		switch (entity.getMenuType()) {
			case MODULE:
				return buildModuleUrl(entity.getModuleInstanceId());
			case LINK:
				return entity.getCustomUrl();
			default:
				return null;
		}
	}

	// 모듈 인스턴스 ID로 URL 생성
	private String buildModuleUrl(String instanceId) {
		if (instanceId == null) {
			return null;
		}
		// 인스턴스 조회
		return instanceRepository.findById(instanceId)
				.map(instance -> {
					// 모듈 조회하여 slug 확보
					return moduleRepository.findByCode(instance.getModuleCode())
							.map(module -> {
								// SINGLE 모듈: /{module-slug}
								if ("SINGLE".equals(module.getType().name())) {
									return "/" + module.getSlug();
								}
								// MULTI 모듈: /{module-slug}/{instance-slug}
								return "/" + module.getSlug() + "/" + instance.getSlug();
							})
							.orElse(null);
				})
				.orElse(null);
	}

	// 메뉴 유형 문자열 파싱
	private MenuType parseMenuType(String menuType) {
		try {
			return MenuType.valueOf(menuType.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(MenuErrorCode.MENU_INVALID_TYPE);
		}
	}

	// 특정 메뉴의 깊이 계산 (0-based: 최상위=0)
	private int calculateDepth(String menuId) {
		int depth = 0;
		String currentId = menuId;
		// 무한 루프 방지용 방문 집합
		Set<String> visited = new HashSet<>();

		while (currentId != null) {
			if (visited.contains(currentId)) {
				break;
			}
			visited.add(currentId);

			String parentId = menuRepository.findById(currentId)
					.map(MenuEntity::getParentId)
					.orElse(null);

			if (parentId == null) {
				break;
			}

			depth++;
			currentId = parentId;
		}

		return depth;
	}

	// targetId가 sourceId의 하위(자손)인지 확인 (순환 참조 검증용)
	private boolean isDescendant(String sourceId, String targetId) {
		// sourceId의 모든 자손을 BFS로 탐색
		Set<String> visited = new HashSet<>();
		List<String> queue = new ArrayList<>();
		queue.add(sourceId);

		while (!queue.isEmpty()) {
			String current = queue.remove(0);
			if (visited.contains(current)) {
				continue;
			}
			visited.add(current);

			// current의 자식 메뉴 조회
			List<MenuEntity> childMenus = menuRepository.findByParentIdOrderBySortOrderAsc(current);
			for (MenuEntity child : childMenus) {
				if (child.getId().equals(targetId)) {
					return true;
				}
				queue.add(child.getId());
			}
		}

		return false;
	}
}
