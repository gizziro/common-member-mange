package com.gizzi.module.board.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.board.dto.category.CategoryResponseDto;
import com.gizzi.module.board.dto.category.CreateCategoryRequestDto;
import com.gizzi.module.board.dto.category.UpdateCategoryRequestDto;
import com.gizzi.module.board.entity.BoardCategoryEntity;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.repository.BoardCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// 게시판 카테고리 관리 서비스
// 게시판 인스턴스 내 카테고리의 생성/조회/수정/삭제/토글을 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCategoryService {

	// 카테고리 리포지토리
	private final BoardCategoryRepository categoryRepository;

	// ─── 카테고리 CRUD ───

	// 카테고리 목록 조회 (정렬 순서 오름차순)
	public List<CategoryResponseDto> getCategories(String boardId) {
		// 게시판의 전체 카테고리를 정렬 순서로 조회
		return categoryRepository.findByBoardInstanceIdOrderBySortOrderAsc(boardId).stream()
				.map(this::toCategoryResponseDto)
				.collect(Collectors.toList());
	}

	// 카테고리 생성
	@Transactional
	public CategoryResponseDto createCategory(String boardId, CreateCategoryRequestDto request) {
		// 동일 게시판 내 슬러그 중복 확인
		if (categoryRepository.existsByBoardInstanceIdAndSlug(boardId, request.getSlug())) {
			throw new BusinessException(BoardErrorCode.BOARD_CATEGORY_DUPLICATE_SLUG);
		}

		// 정렬 순서 기본값 (null이면 0)
		int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : 0;

		// 카테고리 엔티티 생성
		BoardCategoryEntity category = BoardCategoryEntity.create(
				boardId, request.getName(), request.getSlug(),
				request.getDescription(), sortOrder);
		// DB에 저장
		categoryRepository.save(category);

		log.info("카테고리 생성: {} (boardId: {}, slug: {})",
				request.getName(), boardId, request.getSlug());

		// 응답 DTO 변환
		return toCategoryResponseDto(category);
	}

	// 카테고리 수정 — 슬러그 중복 체크 (자신 제외)
	@Transactional
	public CategoryResponseDto updateCategory(String boardId, String categoryId, UpdateCategoryRequestDto request) {
		// 카테고리 조회 (없으면 BOARD_CATEGORY_NOT_FOUND 예외)
		BoardCategoryEntity category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_CATEGORY_NOT_FOUND));

		// 슬러그가 변경되었으면 중복 확인 (자신의 슬러그는 제외)
		if (!category.getSlug().equals(request.getSlug())
				&& categoryRepository.existsByBoardInstanceIdAndSlug(boardId, request.getSlug())) {
			throw new BusinessException(BoardErrorCode.BOARD_CATEGORY_DUPLICATE_SLUG);
		}

		// 정렬 순서 기본값 (null이면 기존 값 유지)
		int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : category.getSortOrder();

		// 카테고리 정보 수정
		category.updateInfo(request.getName(), request.getSlug(), request.getDescription(), sortOrder);
		// 변경 사항 저장
		categoryRepository.save(category);

		log.info("카테고리 수정: {} (categoryId: {}, boardId: {})",
				request.getName(), categoryId, boardId);

		// 응답 DTO 변환
		return toCategoryResponseDto(category);
	}

	// 카테고리 삭제
	@Transactional
	public void deleteCategory(String categoryId) {
		// 카테고리 조회 (없으면 BOARD_CATEGORY_NOT_FOUND 예외)
		BoardCategoryEntity category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_CATEGORY_NOT_FOUND));
		// 카테고리 삭제 (해당 카테고리의 게시글은 categoryId가 null로 변경됨)
		categoryRepository.delete(category);

		log.info("카테고리 삭제: {} (categoryId: {})", category.getName(), categoryId);
	}

	// 카테고리 활성화/비활성화 토글
	@Transactional
	public CategoryResponseDto toggleCategory(String categoryId) {
		// 카테고리 조회 (없으면 BOARD_CATEGORY_NOT_FOUND 예외)
		BoardCategoryEntity category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_CATEGORY_NOT_FOUND));
		// 활성화 상태 토글
		category.toggleActive();
		// 변경 사항 저장
		categoryRepository.save(category);

		log.info("카테고리 토글: {} → {} (categoryId: {})",
				category.getName(), category.getIsActive(), categoryId);

		// 응답 DTO 변환
		return toCategoryResponseDto(category);
	}

	// ─── DTO 변환 헬퍼 ───

	// BoardCategoryEntity → CategoryResponseDto 변환
	private CategoryResponseDto toCategoryResponseDto(BoardCategoryEntity entity) {
		return CategoryResponseDto.builder()
				.id(entity.getId())
				.boardInstanceId(entity.getBoardInstanceId())
				.name(entity.getName())
				.slug(entity.getSlug())
				.description(entity.getDescription())
				.sortOrder(entity.getSortOrder())
				.isActive(entity.getIsActive())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
