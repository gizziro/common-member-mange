package com.gizzi.core.domain.group.repository;

import com.gizzi.core.domain.group.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 그룹 리포지토리 (tb_groups 테이블 접근)
public interface GroupRepository extends JpaRepository<GroupEntity, String> {

	// 그룹 코드로 조회 (시스템 그룹 조회 등)
	Optional<GroupEntity> findByGroupCode(String groupCode);

	// 그룹 코드 중복 검증
	boolean existsByGroupCode(String groupCode);

	// 그룹 이름 중복 검증
	boolean existsByName(String name);
}
