package com.gizzi.core.domain.menu.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.MenuErrorCode;
import com.gizzi.core.domain.menu.dto.CreateMenuRequestDto;
import com.gizzi.core.domain.menu.dto.MenuResponseDto;
import com.gizzi.core.domain.menu.dto.UpdateMenuRequestDto;
import com.gizzi.core.domain.menu.entity.MenuEntity;
import com.gizzi.core.domain.menu.entity.MenuType;
import com.gizzi.core.domain.menu.repository.MenuRepository;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.core.module.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 메뉴 관리 서비스
// 메뉴 CRUD, 트리 조회, 정렬을 담당한다
// 메뉴는 URL 단축(Alias) 시스템으로서 권한 필터링 없이 모든 보이는 메뉴를 노출한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 상수 ]
	//----------------------------------------------------------------------------------------------------------------------
	private static final int     MAX_DEPTH		= 3;	// 최대 메뉴 깊이 (3단계까지 허용)
	private static final Pattern ALIAS_PATTERN	= Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");	// 단축 경로 유효성 정규식

	// Next.js 정적 라우트 예약어 목록 (단축 경로와 충돌 방지)
	private static final Set<String> RESERVED_PATHS = Set.of(
			"login", "sign-up", "profile", "api", "auth",
			"_next", "favicon.ico", "robots.txt", "sitemap.xml"
	);

	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final MenuRepository           menuRepository;		// 메뉴 리포지토리
	private final ModuleInstanceRepository instanceRepository;	// 모듈 인스턴스 리포지토리 (MODULE 타입 URL 생성용)
	private final ModuleRepository         moduleRepository;	// 모듈 리포지토리 (slug 조회 + alias 충돌 검사용)

	//======================================================================================================================
	// 메뉴 항목 생성
	//======================================================================================================================
	@Transactional
	public MenuResponseDto createMenu(CreateMenuRequestDto request)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 메뉴 유형 파싱
		//----------------------------------------------------------------------------------------------------------------------
		MenuType menuType = parseMenuType(request.getMenuType());

		//----------------------------------------------------------------------------------------------------------------------
		// MODULE 타입 유효성 검증
		//----------------------------------------------------------------------------------------------------------------------
		if (menuType == MenuType.MODULE && (request.getModuleInstanceId() == null
				|| request.getModuleInstanceId().isBlank()))
		{
			throw new BusinessException(MenuErrorCode.MENU_MODULE_INSTANCE_REQUIRED);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 부모 메뉴 존재 여부 검증
		//----------------------------------------------------------------------------------------------------------------------
		if (request.getParentId() != null && !request.getParentId().isBlank())
		{
			// 부모 메뉴 존재 확인
			if (!menuRepository.existsById(request.getParentId()))
			{
				throw new BusinessException(MenuErrorCode.MENU_PARENT_NOT_FOUND);
			}
			// 깊이 제한 확인
			int parentDepth = calculateDepth(request.getParentId());
			if (parentDepth >= MAX_DEPTH)
			{
				throw new BusinessException(MenuErrorCode.MENU_MAX_DEPTH_EXCEEDED);
			}
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 단축 경로(alias) 유효성 검증
		//----------------------------------------------------------------------------------------------------------------------
		String aliasPath = normalizeAlias(request.getAliasPath());
		if (aliasPath != null)
		{
			validateAliasPath(aliasPath, null);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 엔티티 생성 및 저장
		//----------------------------------------------------------------------------------------------------------------------
		MenuEntity entity = MenuEntity.create(
				request.getName(),
				request.getIcon(),
				menuType,
				menuType == MenuType.MODULE ? request.getModuleInstanceId() : null,
				menuType == MenuType.LINK ? request.getCustomUrl() : null,
				menuType == MenuType.MODULE ? aliasPath : null,
				menuType == MenuType.MODULE ? normalizeContentPath(request.getContentPath()) : null,
				request.getParentId() != null && !request.getParentId().isBlank()
						? request.getParentId() : null,
				request.getSortOrder()
		);

		// DB에 저장
		menuRepository.save(entity);
		log.info("메뉴 생성: {} ({})", entity.getName(), entity.getMenuType());

		// 응답 DTO 변환 후 반환
		return toResponseDto(entity, List.of());
	}

	//======================================================================================================================
	// 메뉴 항목 수정
	//======================================================================================================================
	@Transactional
	public MenuResponseDto updateMenu(String id, UpdateMenuRequestDto request)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 메뉴 조회
		//----------------------------------------------------------------------------------------------------------------------
		MenuEntity entity = menuRepository.findById(id)
				.orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

		//----------------------------------------------------------------------------------------------------------------------
		// 메뉴 유형 파싱
		//----------------------------------------------------------------------------------------------------------------------
		MenuType menuType = parseMenuType(request.getMenuType());

		//----------------------------------------------------------------------------------------------------------------------
		// MODULE 타입 유효성 검증
		//----------------------------------------------------------------------------------------------------------------------
		if (menuType == MenuType.MODULE && (request.getModuleInstanceId() == null
				|| request.getModuleInstanceId().isBlank()))
		{
			throw new BusinessException(MenuErrorCode.MENU_MODULE_INSTANCE_REQUIRED);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 단축 경로(alias) 유효성 검증
		//----------------------------------------------------------------------------------------------------------------------
		String aliasPath = normalizeAlias(request.getAliasPath());
		if (aliasPath != null)
		{
			validateAliasPath(aliasPath, id);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 정보 수정
		//----------------------------------------------------------------------------------------------------------------------
		entity.updateInfo(
				request.getName(),
				request.getIcon(),
				menuType,
				menuType == MenuType.MODULE ? request.getModuleInstanceId() : null,
				menuType == MenuType.LINK ? request.getCustomUrl() : null,
				menuType == MenuType.MODULE ? aliasPath : null,
				menuType == MenuType.MODULE ? normalizeContentPath(request.getContentPath()) : null
		);

		// DB에 저장
		menuRepository.save(entity);
		log.info("메뉴 수정: {} ({})", entity.getName(), entity.getId());

		// 응답 DTO 변환 후 반환
		return toResponseDto(entity, List.of());
	}

	//======================================================================================================================
	// 메뉴 항목 삭제 (CASCADE로 하위 메뉴도 삭제)
	//======================================================================================================================
	@Transactional
	public void deleteMenu(String id)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 메뉴 존재 확인
		//----------------------------------------------------------------------------------------------------------------------
		MenuEntity entity = menuRepository.findById(id)
				.orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

		// DB에서 삭제
		menuRepository.delete(entity);
		log.info("메뉴 삭제: {} ({})", entity.getName(), entity.getId());
	}

	//======================================================================================================================
	// 정렬 순서 및 부모 변경
	//======================================================================================================================
	@Transactional
	public void updateOrder(String id, Integer sortOrder, String parentId)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 메뉴 조회
		//----------------------------------------------------------------------------------------------------------------------
		MenuEntity entity = menuRepository.findById(id)
				.orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

		//----------------------------------------------------------------------------------------------------------------------
		// 순환 참조 검증 (자기 자신을 부모로 지정 불가)
		//----------------------------------------------------------------------------------------------------------------------
		if (parentId != null && parentId.equals(id))
		{
			throw new BusinessException(MenuErrorCode.MENU_CIRCULAR_REFERENCE);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 부모 메뉴 존재 확인
		//----------------------------------------------------------------------------------------------------------------------
		if (parentId != null && !parentId.isBlank())
		{
			if (!menuRepository.existsById(parentId))
			{
				throw new BusinessException(MenuErrorCode.MENU_PARENT_NOT_FOUND);
			}
			// 자식을 부모로 지정하는 순환 참조 확인
			if (isDescendant(id, parentId))
			{
				throw new BusinessException(MenuErrorCode.MENU_CIRCULAR_REFERENCE);
			}
		}

		// 정렬 순서와 부모 갱신 후 저장
		entity.updateOrder(sortOrder, parentId != null && !parentId.isBlank() ? parentId : null);
		menuRepository.save(entity);
	}

	//======================================================================================================================
	// 가시성 토글
	//======================================================================================================================
	@Transactional
	public void toggleVisibility(String id)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 메뉴 조회
		//----------------------------------------------------------------------------------------------------------------------
		MenuEntity entity = menuRepository.findById(id)
				.orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

		// 가시성 반전 후 저장
		entity.toggleVisibility();
		menuRepository.save(entity);
	}

	//======================================================================================================================
	// 전체 메뉴 트리 조회 (관리자용 — 모든 메뉴 포함)
	//======================================================================================================================
	public List<MenuResponseDto> getAllMenuTree()
	{
		// 전체 메뉴 로드
		List<MenuEntity> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
		// 트리 구조로 변환하여 반환
		return buildTree(allMenus, null);
	}

	//======================================================================================================================
	// 보이는 메뉴 트리 조회 (모든 사용자 공개 — 권한 필터링 없음)
	// is_visible = true인 메뉴만 반환한다
	//======================================================================================================================
	public List<MenuResponseDto> getVisibleMenuTree()
	{
		// is_visible = true인 메뉴만 로드
		List<MenuEntity> visibleMenus = menuRepository.findByIsVisibleTrueOrderBySortOrderAsc();
		// 트리 구조로 변환하여 반환
		return buildTree(visibleMenus, null);
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 메뉴 트리를 재귀적으로 구성
	//----------------------------------------------------------------------------------------------------------------------
	private List<MenuResponseDto> buildTree(List<MenuEntity> allMenus, String parentId)
	{
		List<MenuResponseDto> result = new ArrayList<>();

		//----------------------------------------------------------------------------------------------------------------------
		// 현재 부모에 속하는 메뉴 필터링
		//----------------------------------------------------------------------------------------------------------------------
		List<MenuEntity> children = allMenus.stream()
				.filter(m -> parentId == null
						? m.getParentId() == null
						: parentId.equals(m.getParentId()))
				.collect(Collectors.toList());

		for (MenuEntity menu : children)
		{
			// 하위 메뉴 재귀 구성
			List<MenuResponseDto> childDtos = buildTree(allMenus, menu.getId());

			// SEPARATOR는 자식이 없으면 제외 (사용자 트리에서)
			if (menu.getMenuType() == MenuType.SEPARATOR && childDtos.isEmpty())
			{
				continue;
			}

			// 응답 DTO로 변환하여 결과에 추가
			result.add(toResponseDto(menu, childDtos));
		}

		return result;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// MenuEntity → 응답 DTO 변환
	//----------------------------------------------------------------------------------------------------------------------
	private MenuResponseDto toResponseDto(MenuEntity entity, List<MenuResponseDto> children)
	{
		return MenuResponseDto.builder()
				.id(entity.getId())
				.name(entity.getName())
				.icon(entity.getIcon())
				.menuType(entity.getMenuType().name())
				.url(buildUrl(entity))
				.moduleInstanceId(entity.getModuleInstanceId())
				.customUrl(entity.getCustomUrl())
				.aliasPath(entity.getAliasPath())
				.contentPath(entity.getContentPath())
				.sortOrder(entity.getSortOrder())
				.isVisible(entity.getIsVisible())
				.children(children.isEmpty() ? null : children)
				.build();
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 메뉴 URL 생성
	// MODULE: /{module-slug}/{content-path} (SINGLE+contentPath) 또는 기존 동작
	// LINK: customUrl 반환
	// SEPARATOR: null
	//----------------------------------------------------------------------------------------------------------------------
	private String buildUrl(MenuEntity entity)
	{
		switch (entity.getMenuType())
		{
			case MODULE:
				// 모듈 인스턴스 기반 URL 생성
				return buildModuleUrl(entity.getModuleInstanceId(), entity.getContentPath());
			case LINK:
				// 커스텀 URL 반환
				return entity.getCustomUrl();
			default:
				// SEPARATOR는 URL 없음
				return null;
		}
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 모듈 인스턴스 ID + contentPath로 URL 생성
	//----------------------------------------------------------------------------------------------------------------------
	private String buildModuleUrl(String instanceId, String contentPath)
	{
		// 인스턴스 ID가 없으면 URL 생성 불가
		if (instanceId == null)
		{
			return null;
		}

		// 인스턴스 조회
		return instanceRepository.findById(instanceId)
				.map(instance -> {
					// 모듈 조회하여 slug 확보
					return moduleRepository.findByCode(instance.getModuleCode())
							.map(module -> {
								// SINGLE 모듈
								if ("SINGLE".equals(module.getType().name()))
								{
									// contentPath가 있으면 /{module-slug}/{content-path}
									if (contentPath != null && !contentPath.isBlank())
									{
										return "/" + module.getSlug() + "/" + contentPath;
									}
									return "/" + module.getSlug();
								}
								// MULTI 모듈: /{module-slug}/{instance-slug}
								return "/" + module.getSlug() + "/" + instance.getSlug();
							})
							.orElse(null);
				})
				.orElse(null);
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 단축 경로(alias) 유효성 검증
	// alias가 null이면 검증하지 않음, null이 아니면 형식/중복/충돌 체크
	//----------------------------------------------------------------------------------------------------------------------
	private void validateAliasPath(String aliasPath, String excludeMenuId)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 1. 형식 검증 (영소문자+숫자+하이픈)
		//----------------------------------------------------------------------------------------------------------------------
		if (!ALIAS_PATTERN.matcher(aliasPath).matches())
		{
			throw new BusinessException(MenuErrorCode.MENU_ALIAS_INVALID_FORMAT);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 2. 중복 체크
		//----------------------------------------------------------------------------------------------------------------------
		if (excludeMenuId == null)
		{
			// 생성 시
			if (menuRepository.existsByAliasPath(aliasPath))
			{
				throw new BusinessException(MenuErrorCode.MENU_ALIAS_DUPLICATE);
			}
		}
		else
		{
			// 수정 시 (자기 자신 제외)
			if (menuRepository.existsByAliasPathAndIdNot(aliasPath, excludeMenuId))
			{
				throw new BusinessException(MenuErrorCode.MENU_ALIAS_DUPLICATE);
			}
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 3. 모듈 slug 충돌 체크 (page, board 등)
		//----------------------------------------------------------------------------------------------------------------------
		if (moduleRepository.findBySlug(aliasPath).isPresent())
		{
			throw new BusinessException(MenuErrorCode.MENU_ALIAS_CONFLICTS_MODULE);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 4. 예약어 충돌 체크 (login, sign-up, profile 등)
		//----------------------------------------------------------------------------------------------------------------------
		if (RESERVED_PATHS.contains(aliasPath))
		{
			throw new BusinessException(MenuErrorCode.MENU_ALIAS_RESERVED);
		}
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 단축 경로 정규화 (빈 문자열 → null)
	//----------------------------------------------------------------------------------------------------------------------
	private String normalizeAlias(String alias)
	{
		// null 또는 빈 문자열이면 null 반환
		if (alias == null || alias.isBlank())
		{
			return null;
		}
		// 앞뒤 공백 제거 후 반환
		return alias.trim();
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 콘텐츠 경로 정규화 (빈 문자열 → null)
	//----------------------------------------------------------------------------------------------------------------------
	private String normalizeContentPath(String contentPath)
	{
		// null 또는 빈 문자열이면 null 반환
		if (contentPath == null || contentPath.isBlank())
		{
			return null;
		}
		// 앞뒤 공백 제거 후 반환
		return contentPath.trim();
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 메뉴 유형 문자열 파싱
	//----------------------------------------------------------------------------------------------------------------------
	private MenuType parseMenuType(String menuType)
	{
		try
		{
			// 대문자로 변환 후 MenuType enum으로 파싱
			return MenuType.valueOf(menuType.toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			// 유효하지 않은 메뉴 유형이면 예외 발생
			throw new BusinessException(MenuErrorCode.MENU_INVALID_TYPE);
		}
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 특정 메뉴의 깊이 계산 (0-based: 최상위=0)
	//----------------------------------------------------------------------------------------------------------------------
	private int calculateDepth(String menuId)
	{
		int    depth     = 0;
		String currentId = menuId;
		// 무한 루프 방지용 방문 집합
		Set<String> visited = new HashSet<>();

		while (currentId != null)
		{
			// 이미 방문한 노드면 순환 참조이므로 탈출
			if (visited.contains(currentId))
			{
				break;
			}
			visited.add(currentId);

			// 부모 메뉴 ID 조회
			String parentId = menuRepository.findById(currentId)
					.map(MenuEntity::getParentId)
					.orElse(null);

			// 부모가 없으면 최상위 도달
			if (parentId == null)
			{
				break;
			}

			// 깊이 증가 및 부모로 이동
			depth++;
			currentId = parentId;
		}

		return depth;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// targetId가 sourceId의 하위(자손)인지 확인 (순환 참조 검증용)
	//----------------------------------------------------------------------------------------------------------------------
	private boolean isDescendant(String sourceId, String targetId)
	{
		// sourceId의 모든 자손을 BFS로 탐색
		Set<String>  visited = new HashSet<>();
		List<String> queue   = new ArrayList<>();
		queue.add(sourceId);

		while (!queue.isEmpty())
		{
			// 큐에서 현재 노드 추출
			String current = queue.remove(0);
			// 이미 방문한 노드면 건너뜀
			if (visited.contains(current))
			{
				continue;
			}
			visited.add(current);

			// current의 자식 메뉴 조회
			List<MenuEntity> childMenus = menuRepository.findByParentIdOrderBySortOrderAsc(current);
			for (MenuEntity child : childMenus)
			{
				// 대상 ID와 일치하면 자손임
				if (child.getId().equals(targetId))
				{
					return true;
				}
				// 큐에 추가하여 계속 탐색
				queue.add(child.getId());
			}
		}

		// 탐색 완료 후 대상을 찾지 못함
		return false;
	}
}
