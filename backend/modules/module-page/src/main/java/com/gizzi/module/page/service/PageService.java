package com.gizzi.module.page.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.page.dto.CreatePageRequestDto;
import com.gizzi.module.page.dto.PageListResponseDto;
import com.gizzi.module.page.dto.PageResponseDto;
import com.gizzi.module.page.dto.UpdatePageRequestDto;
import com.gizzi.module.page.entity.PageContentType;
import com.gizzi.module.page.entity.PageEntity;
import com.gizzi.module.page.exception.PageErrorCode;
import com.gizzi.module.page.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// 페이지 관리 서비스
// 페이지 CRUD, 공개/비공개 전환을 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageService {

	// 페이지 리포지토리
	private final PageRepository pageRepository;

	// 페이지 생성
	@Transactional
	public PageResponseDto createPage(CreatePageRequestDto request, String createdBy) {
		// 슬러그 중복 확인
		if (pageRepository.existsBySlug(request.getSlug())) {
			throw new BusinessException(PageErrorCode.PAGE_DUPLICATE_SLUG);
		}

		// 콘텐츠 유형 파싱
		PageContentType contentType = parseContentType(request.getContentType());

		// 엔티티 생성 및 저장
		PageEntity entity = PageEntity.create(
				request.getSlug(),
				request.getTitle(),
				request.getContent(),
				contentType,
				createdBy
		);

		pageRepository.save(entity);
		log.info("페이지 생성: {} (slug: {})", entity.getTitle(), entity.getSlug());

		return toResponseDto(entity);
	}

	// 페이지 수정
	@Transactional
	public PageResponseDto updatePage(String id, UpdatePageRequestDto request, String updatedBy) {
		// 페이지 조회
		PageEntity entity = pageRepository.findById(id)
				.orElseThrow(() -> new BusinessException(PageErrorCode.PAGE_NOT_FOUND));

		// 슬러그 변경 시 중복 확인
		if (!entity.getSlug().equals(request.getSlug())
				&& pageRepository.existsBySlug(request.getSlug())) {
			throw new BusinessException(PageErrorCode.PAGE_DUPLICATE_SLUG);
		}

		// 콘텐츠 유형 파싱
		PageContentType contentType = parseContentType(request.getContentType());

		// 정보 수정
		entity.updateInfo(
				request.getTitle(),
				request.getSlug(),
				request.getContent(),
				contentType,
				updatedBy
		);

		pageRepository.save(entity);
		log.info("페이지 수정: {} (id: {})", entity.getTitle(), entity.getId());

		return toResponseDto(entity);
	}

	// 페이지 삭제
	@Transactional
	public void deletePage(String id) {
		PageEntity entity = pageRepository.findById(id)
				.orElseThrow(() -> new BusinessException(PageErrorCode.PAGE_NOT_FOUND));

		pageRepository.delete(entity);
		log.info("페이지 삭제: {} (id: {})", entity.getTitle(), entity.getId());
	}

	// 페이지 상세 조회 (관리자용 — ID로 조회)
	public PageResponseDto getPage(String id) {
		PageEntity entity = pageRepository.findById(id)
				.orElseThrow(() -> new BusinessException(PageErrorCode.PAGE_NOT_FOUND));
		return toResponseDto(entity);
	}

	// 페이지 목록 조회 (관리자용 — 전체)
	public List<PageListResponseDto> getPages() {
		return pageRepository.findAllByOrderBySortOrderAscCreatedAtDesc()
				.stream()
				.map(this::toListResponseDto)
				.collect(Collectors.toList());
	}

	// 공개/비공개 토글
	@Transactional
	public PageResponseDto togglePublish(String id) {
		PageEntity entity = pageRepository.findById(id)
				.orElseThrow(() -> new BusinessException(PageErrorCode.PAGE_NOT_FOUND));

		entity.togglePublish();
		pageRepository.save(entity);

		log.info("페이지 공개 상태 변경: {} → {}", entity.getTitle(), entity.getIsPublished());
		return toResponseDto(entity);
	}

	// slug로 공개 페이지 조회 (사용자용)
	public PageResponseDto getPageBySlug(String slug) {
		PageEntity entity = pageRepository.findBySlug(slug)
				.orElseThrow(() -> new BusinessException(PageErrorCode.PAGE_NOT_FOUND));

		// 비공개 페이지 접근 차단
		if (!entity.getIsPublished()) {
			throw new BusinessException(PageErrorCode.PAGE_NOT_PUBLISHED);
		}

		return toResponseDto(entity);
	}

	// 공개 페이지 목록 조회 (사용자용)
	public List<PageListResponseDto> getPublishedPages() {
		return pageRepository.findByIsPublishedTrueOrderBySortOrderAsc()
				.stream()
				.map(this::toListResponseDto)
				.collect(Collectors.toList());
	}

	// 콘텐츠 유형 문자열 파싱 (null이면 HTML 기본값)
	private PageContentType parseContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return PageContentType.HTML;
		}
		try {
			return PageContentType.valueOf(contentType.toUpperCase());
		} catch (IllegalArgumentException e) {
			return PageContentType.HTML;
		}
	}

	// PageEntity → PageResponseDto 변환
	private PageResponseDto toResponseDto(PageEntity entity) {
		return PageResponseDto.builder()
				.id(entity.getId())
				.slug(entity.getSlug())
				.title(entity.getTitle())
				.content(entity.getContent())
				.contentType(entity.getContentType().name())
				.isPublished(entity.getIsPublished())
				.sortOrder(entity.getSortOrder())
				.createdBy(entity.getCreatedBy())
				.createdAt(entity.getCreatedAt())
				.updatedBy(entity.getUpdatedBy())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}

	// PageEntity → PageListResponseDto 변환 (본문 제외)
	private PageListResponseDto toListResponseDto(PageEntity entity) {
		return PageListResponseDto.builder()
				.id(entity.getId())
				.slug(entity.getSlug())
				.title(entity.getTitle())
				.contentType(entity.getContentType().name())
				.isPublished(entity.getIsPublished())
				.sortOrder(entity.getSortOrder())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
