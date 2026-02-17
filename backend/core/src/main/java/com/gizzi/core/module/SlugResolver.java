package com.gizzi.core.module;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.ModuleErrorCode;
import com.gizzi.core.domain.menu.entity.MenuEntity;
import com.gizzi.core.domain.menu.entity.MenuType;
import com.gizzi.core.domain.menu.repository.MenuRepository;
import com.gizzi.core.module.dto.InstanceInfoDto;
import com.gizzi.core.module.dto.ModuleInfoDto;
import com.gizzi.core.module.dto.ResolveResponseDto;
import com.gizzi.core.module.entity.ModuleEntity;
import com.gizzi.core.module.entity.ModuleInstanceEntity;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.core.module.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Slug 기반 동적 라우팅 해석 서비스
// 프론트엔드 URL의 slug를 모듈/인스턴스 정보로 변환한다
//
// URL 패턴:
//   SINGLE 모듈: /{module-slug}          예: /dashboard
//   MULTI 모듈:  /{module-slug}/{instance-slug}  예: /board/notice
//   별칭(alias): /{alias}               예: /test → page 모듈 + contentPath
//   별칭+하위:   /{alias}/{sub}          예: /free/write → board/freeboard + subPath
//
// 사용 흐름:
//   1. 사용자가 /board/notice 접근
//   2. 프론트엔드 → GET /resolve/board/notice
//   3. SlugResolver.resolve("board", "notice", userId)
//   4. 응답: 모듈 정보 + 인스턴스 정보 + 사용자 권한
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlugResolver {

	// 슬러그 유효성 검증 정규식 (영소문자+숫자+하이픈, 2~50자)
	private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

	// 슬러그 최소/최대 길이
	private static final int SLUG_MIN_LENGTH = 2;
	private static final int SLUG_MAX_LENGTH = 50;

	// 모듈 리포지토리
	private final ModuleRepository         moduleRepository;

	// 모듈 인스턴스 리포지토리
	private final ModuleInstanceRepository instanceRepository;

	// 권한 체크 유틸리티
	private final PermissionChecker        permissionChecker;

	// 메뉴 리포지토리 (별칭 해석용)
	private final MenuRepository           menuRepository;

	// SINGLE 모듈 slug 해석 (인스턴스 없음)
	// GET /resolve/{moduleSlug}
	public ResolveResponseDto resolveSingle(String moduleSlug, String userId) {
		// 슬러그 유효성 검증
		validateSlug(moduleSlug);

		// 모듈 조회
		ModuleEntity module = moduleRepository.findBySlug(moduleSlug)
				.orElseThrow(() -> new BusinessException(ModuleErrorCode.MODULE_NOT_FOUND));

		// 비활성 모듈 접근 차단
		if (!module.getIsEnabled()) {
			throw new BusinessException(ModuleErrorCode.MODULE_DISABLED);
		}

		// 모듈 정보 DTO 생성
		ModuleInfoDto moduleInfo = toModuleInfo(module);

		return ResolveResponseDto.builder()
				.module(moduleInfo)
				.build();
	}

	// MULTI 모듈 slug 해석 (모듈 + 인스턴스)
	// GET /resolve/{moduleSlug}/{instanceSlug}
	public ResolveResponseDto resolveMulti(String moduleSlug, String instanceSlug, String userId) {
		// 슬러그 유효성 검증
		validateSlug(moduleSlug);
		validateSlug(instanceSlug);

		// 모듈 조회
		ModuleEntity module = moduleRepository.findBySlug(moduleSlug)
				.orElseThrow(() -> new BusinessException(ModuleErrorCode.MODULE_NOT_FOUND));

		// 비활성 모듈 접근 차단
		if (!module.getIsEnabled()) {
			throw new BusinessException(ModuleErrorCode.MODULE_DISABLED);
		}

		// 인스턴스 조회 (모듈 코드 + 인스턴스 슬러그)
		ModuleInstanceEntity instance = instanceRepository
				.findByModuleCodeAndSlug(module.getCode(), instanceSlug)
				.orElseThrow(() -> new BusinessException(ModuleErrorCode.MODULE_INSTANCE_NOT_FOUND));

		// 인스턴스 비활성 접근 차단
		if (!instance.getEnabled()) {
			throw new BusinessException(ModuleErrorCode.MODULE_DISABLED);
		}

		// 모듈/인스턴스 정보 DTO 생성
		ModuleInfoDto moduleInfo     = toModuleInfo(module);
		InstanceInfoDto instanceInfo = toInstanceInfo(instance);

		// 사용자 권한 맵 조회 (리소스별 액션 목록)
		Map<String, List<String>> permissions =
				permissionChecker.getPermissionMap(userId, instance.getInstanceId());

		return ResolveResponseDto.builder()
				.module(moduleInfo)
				.instance(instanceInfo)
				.permissions(permissions.isEmpty() ? null : permissions)
				.build();
	}

	// 별칭(alias) 기반 모듈 해석
	// 메뉴의 alias_path → 모듈/인스턴스 정보로 변환
	// additionalSubPath: 별칭 이후의 추가 경로 세그먼트 (예: /free/write → "write")
	// 해석 실패 시 null 반환 (예외를 던지지 않음)
	public ResolveResponseDto resolveByAlias(String aliasPath, String additionalSubPath, String userId) {
		// 1. alias_path로 메뉴 조회
		MenuEntity menu = menuRepository.findByAliasPath(aliasPath).orElse(null);
		if (menu == null || menu.getMenuType() != MenuType.MODULE) {
			return null;
		}

		// 2. 모듈 인스턴스 조회
		if (menu.getModuleInstanceId() == null) {
			return null;
		}
		ModuleInstanceEntity instance = instanceRepository.findById(menu.getModuleInstanceId())
				.orElse(null);
		if (instance == null) {
			return null;
		}

		// 3. 모듈 조회
		ModuleEntity module = moduleRepository.findByCode(instance.getModuleCode())
				.orElse(null);
		if (module == null || !module.getIsEnabled()) {
			return null;
		}

		// 4. 사용자 권한 맵 조회
		Map<String, List<String>> permissions =
				permissionChecker.getPermissionMap(userId, instance.getInstanceId());

		// 5. subPath 결정: contentPath + additionalSubPath 조합
		String subPath = combineSubPath(menu.getContentPath(), additionalSubPath);

		// 6. 응답 생성
		return ResolveResponseDto.builder()
				.module(toModuleInfo(module))
				.instance(toInstanceInfo(instance))
				.permissions(permissions.isEmpty() ? null : permissions)
				.subPath(subPath)
				.build();
	}

	// contentPath와 additionalSubPath를 조합하여 최종 subPath 생성
	// 예: contentPath="test", additional=null → "test"
	// 예: contentPath=null, additional="write" → "write"
	// 예: contentPath="test", additional="edit" → "test/edit"
	// 예: contentPath=null, additional=null → null
	private String combineSubPath(String contentPath, String additionalSubPath) {
		boolean hasContent    = contentPath != null && !contentPath.isBlank();
		boolean hasAdditional = additionalSubPath != null && !additionalSubPath.isBlank();

		if (hasContent && hasAdditional) {
			return contentPath + "/" + additionalSubPath;
		} else if (hasContent) {
			return contentPath;
		} else if (hasAdditional) {
			return additionalSubPath;
		}
		return null;
	}

	// 슬러그 유효성 검증
	public void validateSlug(String slug) {
		// null 또는 빈 문자열 검증
		if (slug == null || slug.isBlank()) {
			throw new BusinessException(ModuleErrorCode.MODULE_INVALID_SLUG);
		}
		// 길이 검증
		if (slug.length() < SLUG_MIN_LENGTH || slug.length() > SLUG_MAX_LENGTH) {
			throw new BusinessException(ModuleErrorCode.MODULE_INVALID_SLUG);
		}
		// 형식 검증 (영소문자+숫자+하이픈)
		if (!SLUG_PATTERN.matcher(slug).matches()) {
			throw new BusinessException(ModuleErrorCode.MODULE_INVALID_SLUG);
		}
	}

	// ModuleEntity → ModuleInfoDto 변환
	private ModuleInfoDto toModuleInfo(ModuleEntity entity) {
		return ModuleInfoDto.builder()
				.code(entity.getCode())
				.name(entity.getName())
				.slug(entity.getSlug())
				.description(entity.getDescription())
				.type(entity.getType().name())
				.enabled(entity.getIsEnabled())
				.build();
	}

	// ModuleInstanceEntity → InstanceInfoDto 변환
	private InstanceInfoDto toInstanceInfo(ModuleInstanceEntity entity) {
		return InstanceInfoDto.builder()
				.instanceId(entity.getInstanceId())
				.name(entity.getInstanceName())
				.slug(entity.getSlug())
				.description(entity.getDescription())
				.enabled(entity.getEnabled())
				.build();
	}
}
