package com.gizzi.core.module.repository;

import com.gizzi.core.module.entity.ModuleInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 모듈 인스턴스 리포지토리 (tb_module_instances 테이블 접근)
public interface ModuleInstanceRepository extends JpaRepository<ModuleInstanceEntity, String>
{

	//----------------------------------------------------------------------------------------------------------------------
	// 조회
	//----------------------------------------------------------------------------------------------------------------------

	// 모듈 코드 + 슬러그로 인스턴스 조회 (Resolve API에서 사용)
	Optional<ModuleInstanceEntity> findByModuleCodeAndSlug(String moduleCode, String slug);

	// 모듈 코드로 인스턴스 목록 조회
	List<ModuleInstanceEntity> findByModuleCode(String moduleCode);

	// 소유자별 인스턴스 목록 조회
	List<ModuleInstanceEntity> findByOwnerId(String ownerId);

	//----------------------------------------------------------------------------------------------------------------------
	// 존재 여부 확인
	//----------------------------------------------------------------------------------------------------------------------

	// 동일 모듈 내 슬러그 중복 확인
	boolean existsByModuleCodeAndSlug(String moduleCode, String slug);

	// 동일 모듈 내 인스턴스명 중복 확인
	boolean existsByModuleCodeAndInstanceName(String moduleCode, String instanceName);

	// 모듈 코드 + 인스턴스 유형 존재 여부 확인 (SINGLE 모듈 시스템 인스턴스 체크용)
	boolean existsByModuleCodeAndInstanceType(String moduleCode, String instanceType);
}
