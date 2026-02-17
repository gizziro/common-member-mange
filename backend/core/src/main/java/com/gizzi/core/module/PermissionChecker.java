package com.gizzi.core.module;

import com.gizzi.core.module.entity.ModuleInstanceEntity;
import com.gizzi.core.module.entity.ModulePermissionEntity;
import com.gizzi.core.module.repository.GroupModulePermissionRepository;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.core.module.repository.ModulePermissionRepository;
import com.gizzi.core.module.repository.UserModulePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// 플랫 문자열 기반 권한 체크 유틸리티
// 모듈 인스턴스에 대한 사용자 권한을 검증한다
//
// 권한 체크 흐름:
//   1. 인스턴스 소유자 확인 → 소유자면 전체 권한 (true)
//   2. 사용자 직접 권한 (tb_user_module_permissions) 조회 → Set<플랫문자열>
//   3. 소속 그룹 권한 (tb_group_module_permissions) 합산 → Set<플랫문자열>
//   4. Set.contains("BOARD_POST_WRITE") → true/false
//
// 사용 예:
//   permissionChecker.hasPermission(userId, instanceId, "BOARD_POST_WRITE");
//   permissionChecker.getPermissionMap(userId, instanceId);
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionChecker {

	// 모듈 인스턴스 리포지토리 (소유자 확인용)
	private final ModuleInstanceRepository         instanceRepository;

	// 모듈 권한 정의 리포지토리 (권한 ID → 플랫 문자열 변환용)
	private final ModulePermissionRepository        permissionRepository;

	// 사용자 직접 권한 리포지토리
	private final UserModulePermissionRepository    userPermissionRepository;

	// 그룹 권한 리포지토리
	private final GroupModulePermissionRepository   groupPermissionRepository;

	// 특정 권한 보유 여부 확인
	// permission: 플랫 문자열 (예: "BOARD_POST_WRITE")
	public boolean hasPermission(String userId, String instanceId, String permission) {
		// 1. 인스턴스 소유자 확인 — 소유자는 전체 권한
		if (isOwner(userId, instanceId)) {
			return true;
		}

		// 2. 사용자 직접 + 그룹 권한 합산 후 포함 여부 확인
		Set<String> allPermissions = collectAllPermissions(userId, instanceId);
		return allPermissions.contains(permission.toUpperCase());
	}

	// 리소스별 허용된 액션 목록 반환
	// 메뉴 가시성 판단, Resolve API 응답, 프론트엔드 UI 제어에 사용
	// 반환 예: {"post": ["read", "write"], "comment": ["read"]}
	public Map<String, List<String>> getPermissionMap(String userId, String instanceId) {
		// 인스턴스에서 모듈 코드 조회
		Optional<ModuleInstanceEntity> instanceOpt = instanceRepository.findById(instanceId);
		if (instanceOpt.isEmpty()) {
			return Map.of();
		}

		ModuleInstanceEntity instance = instanceOpt.get();
		String moduleCode = instance.getModuleCode();

		// 소유자는 해당 모듈의 모든 권한 보유
		if (userId != null && userId.equals(instance.getOwnerId())) {
			return buildFullPermissionMap(moduleCode);
		}

		// 사용자 직접 + 그룹 권한 합산
		Set<String> permissionIds = collectAllPermissionIds(userId, instanceId);

		// 권한 ID → (resource, action) 매핑으로 변환
		return buildPermissionMapFromIds(moduleCode, permissionIds);
	}

	// 인스턴스 소유자 여부 확인
	private boolean isOwner(String userId, String instanceId) {
		if (userId == null) {
			return false;
		}
		return instanceRepository.findById(instanceId)
				.map(instance -> userId.equals(instance.getOwnerId()))
				.orElse(false);
	}

	// 사용자 직접 + 그룹 권한 합산하여 플랫 문자열 Set 반환
	private Set<String> collectAllPermissions(String userId, String instanceId) {
		// 권한 ID 수집
		Set<String> permissionIds = collectAllPermissionIds(userId, instanceId);

		// 권한 ID → 플랫 문자열 변환
		Set<String> flatPermissions = new HashSet<>();
		for (String permId : permissionIds) {
			permissionRepository.findById(permId)
					.ifPresent(perm -> flatPermissions.add(perm.toFlatPermissionString()));
		}

		return flatPermissions;
	}

	// 사용자 직접 + 그룹 권한 ID 합산
	private Set<String> collectAllPermissionIds(String userId, String instanceId) {
		Set<String> permissionIds = new HashSet<>();

		if (userId == null) {
			return permissionIds;
		}

		// 사용자 직접 권한 ID 조회
		List<String> userPermIds = userPermissionRepository
				.findPermissionIdsByUserIdAndInstanceId(userId, instanceId);
		permissionIds.addAll(userPermIds);

		// 소속 그룹 권한 ID 합산 조회
		List<String> groupPermIds = groupPermissionRepository
				.findPermissionIdsByUserGroupsAndInstanceId(userId, instanceId);
		permissionIds.addAll(groupPermIds);

		return permissionIds;
	}

	// 모듈의 모든 권한을 리소스별 액션 맵으로 변환 (소유자용)
	private Map<String, List<String>> buildFullPermissionMap(String moduleCode) {
		List<ModulePermissionEntity> allPerms = permissionRepository.findByModuleCode(moduleCode);
		return groupByResource(allPerms);
	}

	// 권한 ID Set에 해당하는 권한만 리소스별 액션 맵으로 변환
	private Map<String, List<String>> buildPermissionMapFromIds(String moduleCode,
	                                                            Set<String> permissionIds) {
		// 해당 모듈의 모든 권한 정의에서 부여된 ID만 필터링
		List<ModulePermissionEntity> granted = permissionRepository.findByModuleCode(moduleCode)
				.stream()
				.filter(perm -> permissionIds.contains(perm.getId()))
				.collect(Collectors.toList());

		return groupByResource(granted);
	}

	// 권한 엔티티 목록을 리소스별 액션 맵으로 그룹핑
	// 예: [post:read, post:write, comment:read] → {post: [read, write], comment: [read]}
	private Map<String, List<String>> groupByResource(List<ModulePermissionEntity> permissions) {
		Map<String, List<String>> map = new HashMap<>();

		for (ModulePermissionEntity perm : permissions) {
			map.computeIfAbsent(perm.getResource(), k -> new ArrayList<>())
			   .add(perm.getAction());
		}

		return map;
	}
}
