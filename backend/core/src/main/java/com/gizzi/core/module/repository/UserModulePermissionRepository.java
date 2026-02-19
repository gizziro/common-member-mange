package com.gizzi.core.module.repository;

import com.gizzi.core.module.entity.UserModulePermissionEntity;
import com.gizzi.core.module.entity.UserModulePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 사용자 모듈 권한 리포지토리 (tb_user_module_permissions 테이블 접근)
// PermissionChecker에서 사용자의 직접 권한을 조회할 때 사용한다
public interface UserModulePermissionRepository
		extends JpaRepository<UserModulePermissionEntity, UserModulePermissionId>
{

	//----------------------------------------------------------------------------------------------------------------------
	// 인스턴스별 조회
	//----------------------------------------------------------------------------------------------------------------------

	// 특정 사용자의 특정 인스턴스에 대한 권한 목록 조회
	List<UserModulePermissionEntity> findByUserIdAndModuleInstanceId(
			String userId, String moduleInstanceId);

	// 특정 인스턴스에 부여된 사용자 권한 레코드 수 조회 (권한 설정 여부 판별용)
	long countByModuleInstanceId(String moduleInstanceId);

	// 특정 인스턴스에 대해 직접 권한이 부여된 모든 사용자 권한 조회
	List<UserModulePermissionEntity> findByModuleInstanceId(String moduleInstanceId);

	//----------------------------------------------------------------------------------------------------------------------
	// 사용자별 조회
	//----------------------------------------------------------------------------------------------------------------------

	// 특정 사용자의 특정 인스턴스에 대한 권한 ID 목록 조회 (권한 문자열 조합용)
	@Query("SELECT ump.modulePermissionId FROM UserModulePermissionEntity ump " +
	       "WHERE ump.userId = :userId AND ump.moduleInstanceId = :instanceId")
	List<String> findPermissionIdsByUserIdAndInstanceId(
			@Param("userId") String userId,
			@Param("instanceId") String instanceId);

	// 특정 사용자의 전체 인스턴스 직접 권한 조회 (권한 요약용)
	List<UserModulePermissionEntity> findByUserId(String userId);
}
