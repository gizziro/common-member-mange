package com.gizzi.module.board.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.module.entity.ModuleInstanceEntity;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.module.board.dto.board.BoardListResponseDto;
import com.gizzi.module.board.dto.board.BoardResponseDto;
import com.gizzi.module.board.dto.board.CreateBoardRequestDto;
import com.gizzi.module.board.dto.board.UpdateBoardRequestDto;
import com.gizzi.module.board.dto.settings.BoardSettingsResponseDto;
import com.gizzi.module.board.dto.settings.UpdateBoardSettingsRequestDto;
import com.gizzi.module.board.entity.BoardSettingsEntity;
import com.gizzi.module.board.entity.DisplayFormat;
import com.gizzi.module.board.entity.PaginationType;
import com.gizzi.module.board.entity.PostContentType;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.repository.BoardPostRepository;
import com.gizzi.module.board.repository.BoardSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// 게시판 인스턴스 관리 서비스
// ModuleInstanceEntity + BoardSettingsEntity 생명주기를 함께 관리한다
// 게시판 생성/조회/수정/삭제 및 설정 관리 담당
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardInstanceService {

	// 모듈 인스턴스 리포지토리 (core 모듈)
	private final ModuleInstanceRepository instanceRepository;

	// 게시판 설정 리포지토리
	private final BoardSettingsRepository  settingsRepository;

	// 게시판 게시글 리포지토리 (게시글 수 카운트용)
	private final BoardPostRepository      postRepository;

	// ─── 게시판 CRUD ───

	// 게시판 생성 — ModuleInstance + BoardSettings 동시 생성
	@Transactional
	public BoardResponseDto createBoard(CreateBoardRequestDto request, String createdBy) {
		// 모듈 인스턴스 생성 (board 모듈, 인스턴스 타입 USER)
		ModuleInstanceEntity instance = ModuleInstanceEntity.create(
				"board", request.getName(), request.getSlug(), request.getDescription(),
				createdBy, "USER", createdBy);
		// DB에 인스턴스 저장
		instanceRepository.save(instance);

		// 게시판 설정 생성 (기본값으로 초기화)
		BoardSettingsEntity settings = BoardSettingsEntity.create(instance.getInstanceId());
		// DB에 설정 저장
		settingsRepository.save(settings);

		log.info("게시판 생성: {} (slug: {}, instanceId: {})",
				request.getName(), request.getSlug(), instance.getInstanceId());

		// 응답 DTO 변환 (신규 생성이므로 게시글 수 0)
		return toResponseDto(instance, 0L);
	}

	// 게시판 목록 조회
	public List<BoardListResponseDto> getBoards() {
		// 모듈 코드가 "board"인 인스턴스만 조회 후 생성일시 역순 정렬
		return instanceRepository.findByModuleCode("board").stream()
				.sorted(Comparator.comparing(ModuleInstanceEntity::getCreatedAt).reversed())
				.map(instance -> toListResponseDto(instance,
						postRepository.countByBoardInstanceIdAndIsDeletedFalse(instance.getInstanceId())))
				.collect(Collectors.toList());
	}

	// 게시판 상세 조회
	public BoardResponseDto getBoard(String boardId) {
		// 인스턴스 조회 (없으면 BOARD_NOT_FOUND 예외)
		ModuleInstanceEntity instance = instanceRepository.findById(boardId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
		// 삭제되지 않은 게시글 수 카운트
		long postCount = postRepository.countByBoardInstanceIdAndIsDeletedFalse(boardId);
		// 응답 DTO 변환
		return toResponseDto(instance, postCount);
	}

	// 게시판 수정 — 인스턴스 메타데이터(이름, 슬러그, 설명) 동기화
	@Transactional
	public BoardResponseDto updateBoard(String boardId, UpdateBoardRequestDto request, String updatedBy) {
		// 인스턴스 조회 (없으면 BOARD_NOT_FOUND 예외)
		ModuleInstanceEntity instance = instanceRepository.findById(boardId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
		// 인스턴스 정보 수정
		instance.updateInfo(request.getName(), request.getSlug(), request.getDescription(), updatedBy);
		// 변경 사항 저장
		instanceRepository.save(instance);
		// 삭제되지 않은 게시글 수 카운트
		long postCount = postRepository.countByBoardInstanceIdAndIsDeletedFalse(boardId);
		// 응답 DTO 변환
		return toResponseDto(instance, postCount);
	}

	// 게시판 삭제 — 인스턴스 + 설정은 FK cascade로 함께 삭제
	@Transactional
	public void deleteBoard(String boardId) {
		// 인스턴스 조회 (없으면 BOARD_NOT_FOUND 예외)
		ModuleInstanceEntity instance = instanceRepository.findById(boardId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
		// 인스턴스 삭제 (Settings, Posts, Comments, Files 모두 FK cascade 삭제)
		instanceRepository.delete(instance);

		log.info("게시판 삭제: {} (id: {})", instance.getInstanceName(), boardId);
	}

	// ─── 게시판 설정 ───

	// 게시판 설정 조회
	public BoardSettingsResponseDto getSettings(String boardId) {
		// 설정 조회 (없으면 BOARD_NOT_FOUND 예외)
		BoardSettingsEntity settings = settingsRepository.findById(boardId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
		// 응답 DTO 변환
		return toSettingsResponseDto(settings);
	}

	// 게시판 설정 수정
	@Transactional
	public BoardSettingsResponseDto updateSettings(String boardId, UpdateBoardSettingsRequestDto request) {
		// 설정 조회 (없으면 BOARD_NOT_FOUND 예외)
		BoardSettingsEntity settings = settingsRepository.findById(boardId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));

		// null인 필드는 기존 값 유지하며 설정 일괄 수정
		settings.updateSettings(
				request.getEditorType() != null ? PostContentType.valueOf(request.getEditorType()) : settings.getEditorType(),
				request.getPostsPerPage() != null ? request.getPostsPerPage() : settings.getPostsPerPage(),
				request.getDisplayFormat() != null ? DisplayFormat.valueOf(request.getDisplayFormat()) : settings.getDisplayFormat(),
				request.getPaginationType() != null ? PaginationType.valueOf(request.getPaginationType()) : settings.getPaginationType(),
				request.getAllowAnonymousAccess() != null ? request.getAllowAnonymousAccess() : settings.getAllowAnonymousAccess(),
				request.getAllowFileUpload() != null ? request.getAllowFileUpload() : settings.getAllowFileUpload(),
				request.getAllowedFileTypes() != null ? request.getAllowedFileTypes() : settings.getAllowedFileTypes(),
				request.getMaxFileSize() != null ? request.getMaxFileSize() : settings.getMaxFileSize(),
				request.getMaxFilesPerPost() != null ? request.getMaxFilesPerPost() : settings.getMaxFilesPerPost(),
				request.getMaxReplyDepth() != null ? request.getMaxReplyDepth() : settings.getMaxReplyDepth(),
				request.getMaxCommentDepth() != null ? request.getMaxCommentDepth() : settings.getMaxCommentDepth(),
				request.getAllowSecretPosts() != null ? request.getAllowSecretPosts() : settings.getAllowSecretPosts(),
				request.getAllowDraft() != null ? request.getAllowDraft() : settings.getAllowDraft(),
				request.getAllowTags() != null ? request.getAllowTags() : settings.getAllowTags(),
				request.getAllowVote() != null ? request.getAllowVote() : settings.getAllowVote(),
				request.getUseCategory() != null ? request.getUseCategory() : settings.getUseCategory()
		);
		// 변경 사항 저장
		settingsRepository.save(settings);

		// 응답 DTO 변환
		return toSettingsResponseDto(settings);
	}

	// ─── DTO 변환 헬퍼 ───

	// ModuleInstanceEntity → BoardResponseDto 변환
	private BoardResponseDto toResponseDto(ModuleInstanceEntity instance, long postCount) {
		return BoardResponseDto.builder()
				.id(instance.getInstanceId())
				.name(instance.getInstanceName())
				.slug(instance.getSlug())
				.description(instance.getDescription())
				.ownerId(instance.getOwnerId())
				.enabled(instance.getEnabled())
				.postCount(postCount)
				.createdAt(instance.getCreatedAt())
				.updatedAt(instance.getUpdatedAt())
				.build();
	}

	// ModuleInstanceEntity → BoardListResponseDto 변환
	private BoardListResponseDto toListResponseDto(ModuleInstanceEntity instance, long postCount) {
		return BoardListResponseDto.builder()
				.id(instance.getInstanceId())
				.name(instance.getInstanceName())
				.slug(instance.getSlug())
				.description(instance.getDescription())
				.ownerId(instance.getOwnerId())
				.enabled(instance.getEnabled())
				.postCount(postCount)
				.createdAt(instance.getCreatedAt())
				.updatedAt(instance.getUpdatedAt())
				.build();
	}

	// BoardSettingsEntity → BoardSettingsResponseDto 변환
	private BoardSettingsResponseDto toSettingsResponseDto(BoardSettingsEntity settings) {
		return BoardSettingsResponseDto.builder()
				.editorType(settings.getEditorType().name())
				.postsPerPage(settings.getPostsPerPage())
				.displayFormat(settings.getDisplayFormat().name())
				.paginationType(settings.getPaginationType().name())
				.allowAnonymousAccess(settings.getAllowAnonymousAccess())
				.allowFileUpload(settings.getAllowFileUpload())
				.allowedFileTypes(settings.getAllowedFileTypes())
				.maxFileSize(settings.getMaxFileSize())
				.maxFilesPerPost(settings.getMaxFilesPerPost())
				.maxReplyDepth(settings.getMaxReplyDepth())
				.maxCommentDepth(settings.getMaxCommentDepth())
				.allowSecretPosts(settings.getAllowSecretPosts())
				.allowDraft(settings.getAllowDraft())
				.allowTags(settings.getAllowTags())
				.allowVote(settings.getAllowVote())
				.useCategory(settings.getUseCategory())
				.build();
	}
}
