package com.gizzi.core.domain.menu.repository;

import com.gizzi.core.domain.menu.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// 메뉴 리포지토리 (tb_menus 테이블 접근)
public interface MenuRepository extends JpaRepository<MenuEntity, String> {

	// 전체 메뉴를 정렬 순서대로 조회
	List<MenuEntity> findAllByOrderBySortOrderAsc();

	// 가시성이 true인 메뉴만 정렬 순서대로 조회 (사용자 메뉴 조회용)
	List<MenuEntity> findByIsVisibleTrueOrderBySortOrderAsc();

	// 특정 부모 아래의 자식 메뉴 조회
	List<MenuEntity> findByParentIdOrderBySortOrderAsc(String parentId);

	// 최상위 메뉴(부모 없음) 조회
	@Query("SELECT m FROM MenuEntity m WHERE m.parentId IS NULL ORDER BY m.sortOrder ASC")
	List<MenuEntity> findRootMenus();

	// 단축 경로(alias)로 메뉴 조회
	Optional<MenuEntity> findByAliasPath(String aliasPath);

	// 단축 경로 중복 확인
	boolean existsByAliasPath(String aliasPath);

	// 특정 메뉴 제외 단축 경로 중복 확인 (수정 시)
	boolean existsByAliasPathAndIdNot(String aliasPath, String id);
}
