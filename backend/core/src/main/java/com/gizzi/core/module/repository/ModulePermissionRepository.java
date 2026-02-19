package com.gizzi.core.module.repository;

import com.gizzi.core.module.entity.ModulePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 모듈 권한 정의 리포지토리 (tb_module_permissions 테이블 접근)
public interface ModulePermissionRepository extends JpaRepository<ModulePermissionEntity, String>
{

	//----------------------------------------------------------------------------------------------------------------------
	// 조회
	//----------------------------------------------------------------------------------------------------------------------

	// 모듈 코드로 해당 모듈의 모든 권한 정의 조회
	List<ModulePermissionEntity> findByModuleCode(String moduleCode);

	// 모듈 코드 + 리소스 + 액션으로 특정 권한 조회
	Optional<ModulePermissionEntity> findByModuleCodeAndResourceAndAction(
			String moduleCode, String resource, String action);

	//----------------------------------------------------------------------------------------------------------------------
	// 존재 여부 확인
	//----------------------------------------------------------------------------------------------------------------------

	// 모듈 코드 + 리소스 + 액션 존재 여부 확인
	boolean existsByModuleCodeAndResourceAndAction(
			String moduleCode, String resource, String action);
}
