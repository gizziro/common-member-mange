package com.gizzi.core.module.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.ModuleErrorCode;
import com.gizzi.core.domain.group.entity.GroupEntity;
import com.gizzi.core.domain.group.repository.GroupRepository;
import com.gizzi.core.module.dto.InstancePermissionDto;
import com.gizzi.core.module.dto.SetPermissionsRequestDto;
import com.gizzi.core.module.entity.GroupModulePermissionEntity;
import com.gizzi.core.module.entity.ModuleInstanceEntity;
import com.gizzi.core.module.entity.ModulePermissionEntity;
import com.gizzi.core.module.repository.GroupModulePermissionRepository;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.core.module.repository.ModulePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 모듈 인스턴스 권한 부여/회수/조회 서비스
// 메뉴 관리에서 그룹별 권한을 통합 관리하기 위해 사용한다
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

	// 그룹 리포지토리
	private final GroupRepository                   groupRepository;

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
}
