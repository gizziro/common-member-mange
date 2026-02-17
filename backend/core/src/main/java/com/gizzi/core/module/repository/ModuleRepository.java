package com.gizzi.core.module.repository;

import com.gizzi.core.module.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 모듈 리포지토리 (tb_modules 테이블 접근)
public interface ModuleRepository extends JpaRepository<ModuleEntity, String> {

	// 모듈 코드로 조회
	Optional<ModuleEntity> findByCode(String code);

	// 슬러그로 조회
	Optional<ModuleEntity> findBySlug(String slug);

	// 모듈 코드 존재 여부 확인
	boolean existsByCode(String code);

	// 슬러그 존재 여부 확인
	boolean existsBySlug(String slug);
}
