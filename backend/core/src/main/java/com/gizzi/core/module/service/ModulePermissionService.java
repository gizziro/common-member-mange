package com.gizzi.core.module.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.ModuleErrorCode;
import com.gizzi.core.domain.group.entity.GroupEntity;
import com.gizzi.core.domain.group.entity.GroupMemberEntity;
import com.gizzi.core.domain.group.repository.GroupMemberRepository;
import com.gizzi.core.domain.group.repository.GroupRepository;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import com.gizzi.core.module.dto.InstancePermissionDto;
import com.gizzi.core.module.dto.PermissionItemDto;
import com.gizzi.core.module.dto.PermissionSummaryDto;
import com.gizzi.core.module.dto.SetPermissionsRequestDto;
import com.gizzi.core.module.dto.SetUserPermissionsRequestDto;
import com.gizzi.core.module.dto.UserInstancePermissionDto;
import com.gizzi.core.module.entity.GroupModulePermissionEntity;
import com.gizzi.core.module.entity.ModuleEntity;
import com.gizzi.core.module.entity.ModuleInstanceEntity;
import com.gizzi.core.module.entity.ModulePermissionEntity;
import com.gizzi.core.module.entity.UserModulePermissionEntity;
import com.gizzi.core.module.repository.GroupModulePermissionRepository;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.core.module.repository.ModulePermissionRepository;
import com.gizzi.core.module.repository.ModuleRepository;
import com.gizzi.core.module.repository.UserModulePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 모듈 인스턴스 권한 부여/회수/조회 서비스
// 그룹/사용자별 권한을 통합 관리하고 권한 요약을 제공한다
// 기존 PermissionChecker는 읽기 전용 조회, 이 서비스는 쓰기(부여/회수) 담당
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModulePermissionService {

	// 모듈 인스턴스 리포지토리
	private final ModuleInstanceRepository         instanceRepository;

	// 모듈 권한 정의 리포지토리
	private final ModulePermissionRepository        permissionRepository;

	// 그룹 모듈 권한 리포지토리
	private final GroupModulePermissionRepository   groupPermissionRepository;

	// 사용자 모듈 권한 리포지토리
	private final UserModulePermissionRepository    userPermissionRepository;

	// 그룹 리포지토리
	private final GroupRepository                   groupRepository;

	// 사용자 리포지토리
	private final UserRepository                    userRepository;

	// 그룹 멤버 리포지토리
	private final GroupMemberRepository             groupMemberRepository;

	// 모듈 리포지토리
	private final ModuleRepository                  moduleRepository;

	// ─── 그룹 권한 관리 ───

	// 인스턴스의 그룹별 권한 현황 조회
	// 모든 그룹에 대해 해당 인스턴스에 부여된 권한 ID 목록을 반환한다
	public List<InstancePermissionDto> getGroupPermissions(String instanceId) {
		// 인스턴스 존재 확인
		if (!instanceRepository.existsById(instanceId)) {
			throw new BusinessException(ModuleErrorCode.MODULE_INSTANCE_NOT_FOUND);
		}

		// 모든 그룹 조회
		List<GroupEntity> allGroups = groupRepository.findAll();

		// 각 그룹별 부여된 권한 조회
		List<InstancePermissionDto> result = new ArrayList<>();
		for (GroupEntity group : allGroups) {
			List<GroupModulePermissionEntity> groupPerms =
					groupPermissionRepository.findByGroupIdAndModuleInstanceId(
							group.getId(), instanceId);

			List<String> grantedIds = groupPerms.stream()
					.map(GroupModulePermissionEntity::getModulePermissionId)
					.collect(Collectors.toList());

			result.add(InstancePermissionDto.builder()
					.groupId(group.getId())
					.groupName(group.getName())
					.groupCode(group.getGroupCode())
					.grantedPermissionIds(grantedIds)
					.build());
		}

		return result;
	}

	// 그룹별 권한 일괄 설정 (기존 삭제 후 재설정)
	@Transactional
	public void setGroupPermissions(String instanceId, SetPermissionsRequestDto request) {
		// 인스턴스 존재 확인
		if (!instanceRepository.existsById(instanceId)) {
			throw new BusinessException(ModuleErrorCode.MODULE_INSTANCE_NOT_FOUND);
		}

		String groupId = request.getGroupId();

		// 기존 권한 전체 삭제
		List<GroupModulePermissionEntity> existing =
				groupPermissionRepository.findByGroupIdAndModuleInstanceId(groupId, instanceId);
		groupPermissionRepository.deleteAll(existing);

		// 새 권한 부여
		for (String permissionId : request.getPermissionIds()) {
			GroupModulePermissionEntity entity = GroupModulePermissionEntity.create(
					groupId, instanceId, permissionId
			);
			groupPermissionRepository.save(entity);
		}

		log.info("그룹 권한 설정 완료: groupId={}, instanceId={}, 권한 {}개",
				groupId, instanceId, request.getPermissionIds().size());
	}

	// ─── 사용자 권한 관리 ───

	// 인스턴스의 개별 사용자 권한 현황 조회
	// 직접 권한이 부여된 사용자만 반환한다
	public List<UserInstancePermissionDto> getUserPermissions(String instanceId) {
		// 인스턴스 존재 확인
		if (!instanceRepository.existsById(instanceId)) {
			throw new BusinessException(ModuleErrorCode.MODULE_INSTANCE_NOT_FOUND);
		}

		// 해당 인스턴스에 직접 권한이 부여된 모든 사용자 권한 조회
		List<UserModulePermissionEntity> allPerms =
				userPermissionRepository.findByModuleInstanceId(instanceId);

		// 사용자별로 권한 그룹핑 (순서 보장용 LinkedHashMap)
		Map<String, List<String>> userPermMap = new LinkedHashMap<>();
		for (UserModulePermissionEntity perm : allPerms) {
			userPermMap.computeIfAbsent(perm.getUserId(), k -> new ArrayList<>())
					.add(perm.getModulePermissionId());
		}

		// 사용자 정보 조회 후 DTO 변환
		List<UserInstancePermissionDto> result = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : userPermMap.entrySet()) {
			String userPk = entry.getKey();
			// 사용자 정보 조회
			UserEntity user = userRepository.findById(userPk).orElse(null);
			if (user == null) {
				continue;  // 삭제된 사용자는 건너뜀
			}

			result.add(UserInstancePermissionDto.builder()
					.userId(userPk)
					.loginId(user.getUserId())
					.username(user.getUsername())
					.grantedPermissionIds(entry.getValue())
					.build());
		}

		return result;
	}

	// 사용자별 권한 일괄 설정 (기존 삭제 후 재설정)
	@Transactional
	public void setUserPermissions(String instanceId, SetUserPermissionsRequestDto request) {
		// 인스턴스 존재 확인
		if (!instanceRepository.existsById(instanceId)) {
			throw new BusinessException(ModuleErrorCode.MODULE_INSTANCE_NOT_FOUND);
		}

		String userId = request.getUserId();

		// 기존 권한 전체 삭제
		List<UserModulePermissionEntity> existing =
				userPermissionRepository.findByUserIdAndModuleInstanceId(userId, instanceId);
		userPermissionRepository.deleteAll(existing);

		// 새 권한 부여
		for (String permissionId : request.getPermissionIds()) {
			UserModulePermissionEntity entity = UserModulePermissionEntity.create(
					userId, instanceId, permissionId
			);
			userPermissionRepository.save(entity);
		}

		log.info("사용자 권한 설정 완료: userId={}, instanceId={}, 권한 {}개",
				userId, instanceId, request.getPermissionIds().size());
	}

	// ─── 권한 요약 (읽기 전용) ───

	// 사용자의 전체 모듈 권한 요약 조회
	// 직접 부여된 권한 + 그룹을 통한 권한을 인스턴스별로 집계한다
	public List<PermissionSummaryDto> getUserPermissionSummary(String userPk) {
		List<PermissionSummaryDto> result = new ArrayList<>();

		// 1. 직접 부여된 권한 조회
		List<UserModulePermissionEntity> directPerms = userPermissionRepository.findByUserId(userPk);
		// 인스턴스별로 그룹핑
		Map<String, List<String>> directPermsByInstance = new LinkedHashMap<>();
		for (UserModulePermissionEntity perm : directPerms) {
			directPermsByInstance.computeIfAbsent(perm.getModuleInstanceId(), k -> new ArrayList<>())
					.add(perm.getModulePermissionId());
		}

		// 직접 권한 → PermissionSummaryDto 변환
		for (Map.Entry<String, List<String>> entry : directPermsByInstance.entrySet()) {
			PermissionSummaryDto summary = buildPermissionSummary(
					entry.getKey(), entry.getValue(), "DIRECT");
			if (summary != null) {
				result.add(summary);
			}
		}

		// 2. 소속 그룹을 통한 권한 조회
		List<GroupMemberEntity> memberships = groupMemberRepository.findByUserId(userPk);
		for (GroupMemberEntity membership : memberships) {
			String groupId = membership.getGroupId();
			// 그룹명 조회
			GroupEntity group = groupRepository.findById(groupId).orElse(null);
			if (group == null) continue;

			// 그룹의 전체 인스턴스 권한 조회
			List<GroupModulePermissionEntity> groupPerms =
					groupPermissionRepository.findByGroupId(groupId);

			// 인스턴스별로 그룹핑
			Map<String, List<String>> groupPermsByInstance = new LinkedHashMap<>();
			for (GroupModulePermissionEntity perm : groupPerms) {
				groupPermsByInstance.computeIfAbsent(perm.getModuleInstanceId(), k -> new ArrayList<>())
						.add(perm.getModulePermissionId());
			}

			// 그룹 권한 → PermissionSummaryDto 변환
			String source = "GROUP:" + group.getName();
			for (Map.Entry<String, List<String>> entry : groupPermsByInstance.entrySet()) {
				PermissionSummaryDto summary = buildPermissionSummary(
						entry.getKey(), entry.getValue(), source);
				if (summary != null) {
					result.add(summary);
				}
			}
		}

		return result;
	}

	// 그룹의 전체 모듈 권한 요약 조회
	public List<PermissionSummaryDto> getGroupPermissionSummary(String groupId) {
		List<PermissionSummaryDto> result = new ArrayList<>();

		// 그룹의 전체 인스턴스 권한 조회
		List<GroupModulePermissionEntity> groupPerms =
				groupPermissionRepository.findByGroupId(groupId);

		// 인스턴스별로 그룹핑
		Map<String, List<String>> permsByInstance = new LinkedHashMap<>();
		for (GroupModulePermissionEntity perm : groupPerms) {
			permsByInstance.computeIfAbsent(perm.getModuleInstanceId(), k -> new ArrayList<>())
					.add(perm.getModulePermissionId());
		}

		// 인스턴스별 PermissionSummaryDto 변환
		for (Map.Entry<String, List<String>> entry : permsByInstance.entrySet()) {
			PermissionSummaryDto summary = buildPermissionSummary(
					entry.getKey(), entry.getValue(), "DIRECT");
			if (summary != null) {
				result.add(summary);
			}
		}

		return result;
	}

	// ─── 공통 메서드 ───

	// 해당 모듈의 사용 가능한 권한 목록 조회
	public List<ModulePermissionEntity> getAvailablePermissions(String moduleCode) {
		return permissionRepository.findByModuleCode(moduleCode);
	}

	// 인스턴스의 모듈 코드 조회
	public String getModuleCode(String instanceId) {
		return instanceRepository.findById(instanceId)
				.map(ModuleInstanceEntity::getModuleCode)
				.orElseThrow(() -> new BusinessException(ModuleErrorCode.MODULE_INSTANCE_NOT_FOUND));
	}

	// ─── 헬퍼 메서드 ───

	// 인스턴스 ID + 권한 ID 목록 → PermissionSummaryDto 빌드
	private PermissionSummaryDto buildPermissionSummary(String instanceId,
	                                                   List<String> permissionIds,
	                                                   String source) {
		// 인스턴스 조회
		ModuleInstanceEntity instance = instanceRepository.findById(instanceId).orElse(null);
		if (instance == null) return null;

		// 모듈 정보 조회
		ModuleEntity module = moduleRepository.findByCode(instance.getModuleCode()).orElse(null);
		String moduleName = (module != null) ? module.getName() : instance.getModuleCode();

		// 권한 정의 조회 후 PermissionItemDto 변환
		List<PermissionItemDto> permissionItems = new ArrayList<>();
		for (String permId : permissionIds) {
			ModulePermissionEntity permEntity = permissionRepository.findById(permId).orElse(null);
			if (permEntity == null) continue;

			permissionItems.add(PermissionItemDto.builder()
					.resource(permEntity.getResource())
					.action(permEntity.getAction())
					.name(permEntity.getName())
					.build());
		}

		return PermissionSummaryDto.builder()
				.instanceId(instanceId)
				.instanceName(instance.getInstanceName())
				.instanceSlug(instance.getSlug())
				.moduleCode(instance.getModuleCode())
				.moduleName(moduleName)
				.source(source)
				.permissions(permissionItems)
				.build();
	}
}
