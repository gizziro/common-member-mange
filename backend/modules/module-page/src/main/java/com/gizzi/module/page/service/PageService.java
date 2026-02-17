package com.gizzi.module.page.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.module.PermissionChecker;
import com.gizzi.core.module.entity.ModuleInstanceEntity;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
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
// 페이지 CRUD, 공개/비공개 전환, 모듈 인스턴스 생명주기, 권한 기반 접근 제어를 담당한다
//
// MULTI 모듈 구조:
//   - 페이지 생성 시 → ModuleInstanceEntity + PageEntity 동시 생성
//   - 페이지 삭제 시 → 모듈 인스턴스도 함께 삭제 (cascade로 권한도 삭제)
//   - 페이지 수정 시 → slug/title 변경 시 모듈 인스턴스도 동기화
//
// 가시성 모델:
//   - is_published=false → 비공개 (사용자 접근 차단)
//   - is_published=true + 권한 미설정 → 전체 공개
//   - is_published=true + 권한 설정됨 → 해당 그룹/사용자만 접근
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageService {

	// 페이지 모듈 코드 상수
	private static final String MODULE_CODE    = "page";

	// 모듈 인스턴스 유형 상수 (사용자가 생성한 인스턴스)
	private static final String INSTANCE_TYPE  = "USER";

	// 페이지 리포지토리
	private final PageRepository           pageRepository;

	// 모듈 인스턴스 리포지토리
	private final ModuleInstanceRepository instanceRepository;

	// 권한 체크 유틸리티
	private final PermissionChecker        permissionChecker;

	// ─── 관리자용 API (admin-api) ───

	// 페이지 생성 — ModuleInstance + PageEntity 동시 생성
	@Transactional
	public PageResponseDto createPage(CreatePageRequestDto request, String createdBy) {
		// 슬러그 중복 확인
		if (pageRepository.existsBySlug(request.getSlug())) {
			throw new BusinessException(PageErrorCode.PAGE_DUPLICATE_SLUG);
		}

		// 콘텐츠 유형 파싱
		PageContentType contentType = parseContentType(request.getContentType());

		// 1. 모듈 인스턴스 생성 (moduleCode=page, slug=pageSlug, instanceType=USER)
		ModuleInstanceEntity instance = ModuleInstanceEntity.create(
				MODULE_CODE,
				request.getTitle(),     // instanceName = 페이지 제목
				request.getSlug(),      // slug = 페이지 slug
				null,                   // description
				createdBy,              // ownerId = 생성자
				INSTANCE_TYPE,          // instanceType = USER
				createdBy               // createdBy
		);
		instanceRepository.save(instance);

		// 2. 페이지 엔티티 생성 (moduleInstanceId 연결)
		PageEntity entity = PageEntity.create(
				request.getSlug(),
				request.getTitle(),
				request.getContent(),
				contentType,
				instance.getInstanceId(),
				createdBy
		);
		pageRepository.save(entity);

		log.info("페이지 생성: {} (slug: {}, instanceId: {})",
				entity.getTitle(), entity.getSlug(), instance.getInstanceId());

		return toResponseDto(entity);
	}

	// 페이지 수정 — slug/title 변경 시 모듈 인스턴스도 동기화
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

		// 연결된 모듈 인스턴스가 있으면 slug/title 동기화
		if (entity.getModuleInstanceId() != null) {
			instanceRepository.findById(entity.getModuleInstanceId())
					.ifPresent(instance -> {
						instance.updateInfo(
								request.getTitle(),    // instanceName 동기화
								request.getSlug(),     // slug 동기화
								null,                  // description 유지
								updatedBy
						);
						instanceRepository.save(instance);
					});
		}

		// 페이지 정보 수정
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

	// 페이지 삭제 — 연결된 모듈 인스턴스도 삭제 (cascade로 권한도 삭제)
	@Transactional
	public void deletePage(String id) {
		PageEntity entity = pageRepository.findById(id)
				.orElseThrow(() -> new BusinessException(PageErrorCode.PAGE_NOT_FOUND));

		// 연결된 모듈 인스턴스 삭제 (cascade로 tb_group_module_permissions, tb_user_module_permissions도 삭제)
		if (entity.getModuleInstanceId() != null) {
			instanceRepository.findById(entity.getModuleInstanceId())
					.ifPresent(instanceRepository::delete);
		}

		// 페이지 삭제
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

	// ─── 사용자용 API (user-api) ───

	// slug로 공개 페이지 조회 (사용자용 — 권한 체크 포함)
	// userId가 null이면 비로그인 사용자
	public PageResponseDto getPublishedPage(String slug, String userId) {
		PageEntity entity = pageRepository.findBySlug(slug)
				.orElseThrow(() -> new BusinessException(PageErrorCode.PAGE_NOT_FOUND));

		// 비공개 페이지 접근 차단
		if (!entity.getIsPublished()) {
			throw new BusinessException(PageErrorCode.PAGE_NOT_PUBLISHED);
		}

		// 권한 체크
		checkPageAccess(entity, userId);

		return toResponseDto(entity);
	}

	// 공개 페이지 목록 조회 (사용자용 — 권한 필터링)
	// userId가 null이면 비로그인 사용자 → 공개 페이지만 표시
	public List<PageListResponseDto> getPublishedPages(String userId) {
		return pageRepository.findByIsPublishedTrueOrderBySortOrderAsc()
				.stream()
				// 권한 필터: 접근 가능한 페이지만 포함
				.filter(entity -> canAccessPage(entity, userId))
				.map(this::toListResponseDto)
				.collect(Collectors.toList());
	}

	// ─── 레거시 호환 (기존 SINGLE 시절 메서드 — 외부 참조 없으면 삭제 가능) ───

	// slug로 공개 페이지 조회 (권한 체크 없음, 레거시 호환)
	public PageResponseDto getPageBySlug(String slug) {
		return getPublishedPage(slug, null);
	}

	// 공개 페이지 목록 조회 (권한 체크 없음, 레거시 호환)
	public List<PageListResponseDto> getPublishedPages() {
		return getPublishedPages(null);
	}

	// ─── 내부 메서드 ───

	// 페이지 접근 권한 체크
	// 가시성 모델:
	//   1. moduleInstanceId가 null → 공개 (레거시 데이터)
	//   2. 권한 미설정 (어떤 그룹/사용자에도 권한 부여 안 됨) → 전체 공개
	//   3. 권한 설정됨 → PAGE_PAGE_READ 권한 보유 시 접근 허용
	private void checkPageAccess(PageEntity entity, String userId) {
		// moduleInstanceId가 없으면 공개 (레거시 호환)
		if (entity.getModuleInstanceId() == null) {
			return;
		}

		// 권한이 하나도 설정되지 않은 인스턴스 → 전체 공개
		if (!permissionChecker.hasAnyPermissionGranted(entity.getModuleInstanceId())) {
			return;
		}

		// 권한이 설정된 페이지 — 비로그인 사용자는 접근 차단
		if (userId == null) {
			throw new BusinessException(PageErrorCode.PAGE_ACCESS_DENIED);
		}

		// PAGE_PAGE_READ 권한 체크
		if (!permissionChecker.hasPermission(userId, entity.getModuleInstanceId(), "PAGE_PAGE_READ")) {
			throw new BusinessException(PageErrorCode.PAGE_ACCESS_DENIED);
		}
	}

	// 페이지 접근 가능 여부 (목록 필터링용, 예외 대신 boolean 반환)
	private boolean canAccessPage(PageEntity entity, String userId) {
		// moduleInstanceId가 없으면 공개 (레거시 호환)
		if (entity.getModuleInstanceId() == null) {
			return true;
		}

		// 권한이 하나도 설정되지 않은 인스턴스 → 전체 공개
		if (!permissionChecker.hasAnyPermissionGranted(entity.getModuleInstanceId())) {
			return true;
		}

		// 권한이 설정된 페이지 — 비로그인 사용자는 접근 불가
		if (userId == null) {
			return false;
		}

		// PAGE_PAGE_READ 권한 체크
		return permissionChecker.hasPermission(userId, entity.getModuleInstanceId(), "PAGE_PAGE_READ");
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
				.moduleInstanceId(entity.getModuleInstanceId())
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
				.moduleInstanceId(entity.getModuleInstanceId())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
