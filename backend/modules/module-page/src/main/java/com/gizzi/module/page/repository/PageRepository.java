package com.gizzi.module.page.repository;

import com.gizzi.module.page.entity.PageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 페이지 리포지토리 (tb_page_pages 테이블 접근)
public interface PageRepository extends JpaRepository<PageEntity, String> {

	// 슬러그로 페이지 조회 (사용자 slug 기반 접근)
	Optional<PageEntity> findBySlug(String slug);

	// 슬러그 중복 확인
	boolean existsBySlug(String slug);

	// 모듈 인스턴스 ID로 페이지 조회
	Optional<PageEntity> findByModuleInstanceId(String moduleInstanceId);

	// 공개된 페이지 목록 조회 (정렬 순서)
	List<PageEntity> findByIsPublishedTrueOrderBySortOrderAsc();

	// 전체 페이지 목록 조회 (관리자용, 정렬 순서)
	List<PageEntity> findAllByOrderBySortOrderAscCreatedAtDesc();
}
