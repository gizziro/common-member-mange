package com.gizzi.core.module.repository;

import com.gizzi.core.module.entity.GroupModulePermissionEntity;
import com.gizzi.core.module.entity.GroupModulePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 그룹 모듈 권한 리포지토리 (tb_group_module_permissions 테이블 접근)
// PermissionChecker에서 사용자 소속 그룹의 권한을 합산 조회할 때 사용한다
public interface GroupModulePermissionRepository
		extends JpaRepository<GroupModulePermissionEntity, GroupModulePermissionId> {

	// 특정 그룹의 특정 인스턴스에 대한 권한 목록 조회
	List<GroupModulePermissionEntity> findByGroupIdAndModuleInstanceId(
			String groupId, String moduleInstanceId);

	// 특정 인스턴스에 부여된 그룹 권한 레코드 수 조회 (권한 설정 여부 판별용)
	long countByModuleInstanceId(String moduleInstanceId);

	// 사용자가 소속된 모든 그룹의 특정 인스턴스에 대한 권한 ID 합산 조회
	// tb_group_members JOIN tb_group_module_permissions로 사용자 소속 그룹의 권한을 한번에 조회한다
	@Query("SELECT DISTINCT gmp.modulePermissionId FROM GroupModulePermissionEntity gmp " +
	       "WHERE gmp.moduleInstanceId = :instanceId " +
	       "AND gmp.groupId IN (" +
	       "  SELECT gm.groupId FROM com.gizzi.core.domain.group.entity.GroupMemberEntity gm " +
	       "  WHERE gm.userId = :userId" +
	       ")")
	List<String> findPermissionIdsByUserGroupsAndInstanceId(
			@Param("userId") String userId,
			@Param("instanceId") String instanceId);

	// 특정 그룹의 전체 인스턴스 권한 조회 (권한 요약용)
	List<GroupModulePermissionEntity> findByGroupId(String groupId);
}
