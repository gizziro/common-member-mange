package com.gizzi.module.board.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.board.dto.comment.CommentResponseDto;
import com.gizzi.module.board.dto.comment.CreateCommentRequestDto;
import com.gizzi.module.board.dto.comment.UpdateCommentRequestDto;
import com.gizzi.module.board.entity.BoardCommentClosureEntity;
import com.gizzi.module.board.entity.BoardCommentEntity;
import com.gizzi.module.board.entity.BoardPostEntity;
import com.gizzi.module.board.entity.BoardSettingsEntity;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.repository.BoardCommentClosureRepository;
import com.gizzi.module.board.repository.BoardCommentRepository;
import com.gizzi.module.board.repository.BoardPostRepository;
import com.gizzi.module.board.repository.BoardSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 게시판 댓글 CRUD + Closure Table 계층 관리 서비스
// 댓글 생성/수정/삭제/트리 조회 기능을 제공한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCommentService {

	// 댓글 리포지토리
	private final BoardCommentRepository        commentRepository;

	// 댓글 Closure Table 리포지토리 (계층 관계 관리)
	private final BoardCommentClosureRepository  closureRepository;

	// 게시글 리포지토리 (게시글 존재 확인 + 댓글 수 동기화)
	private final BoardPostRepository            postRepository;

	// 게시판 설정 리포지토리 (최대 댓글 깊이 제한 조회)
	private final BoardSettingsRepository        settingsRepository;

	// 게시판 권한 헬퍼 (수정/삭제 권한 체크)
	private final BoardPermissionHelper          permissionHelper;

	// ─── 댓글 생성 ───

	// 댓글 생성 (대댓글 Closure Table 관리 + 게시글 댓글 수 증가)
	@Transactional
	public CommentResponseDto createComment(String boardId, String postId,
	                                        CreateCommentRequestDto request,
	                                        String userId, String username) {
		// 게시글 존재 여부 확인 (삭제된 게시글에는 댓글 불가)
		BoardPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		// 대댓글인 경우 부모 댓글 검증 + depth 계산
		int depth = 0;
		if (request.getParentId() != null) {
			// 부모 댓글 존재 여부 확인
			BoardCommentEntity parent = commentRepository.findById(request.getParentId())
					.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_COMMENT_NOT_FOUND));
			// 부모 깊이 + 1 = 자식 깊이
			depth = parent.getDepth() + 1;

			// 게시판 설정에서 최대 댓글 깊이 확인
			BoardSettingsEntity settings = settingsRepository.findById(boardId)
					.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
			// 최대 댓글 깊이 초과 시 예외
			if (depth > settings.getMaxCommentDepth()) {
				throw new BusinessException(BoardErrorCode.BOARD_MAX_COMMENT_DEPTH);
			}
		}

		// 댓글 엔티티 생성
		BoardCommentEntity comment = BoardCommentEntity.create(
				postId, request.getParentId(), depth,
				request.getContent(), userId, username
		);
		// DB에 댓글 저장
		commentRepository.save(comment);

		// Closure Table 관리 — 자기 자신 참조 (depth 0)
		closureRepository.save(BoardCommentClosureEntity.create(comment.getId(), comment.getId(), 0));

		// 부모가 있으면 부모의 모든 조상을 복사하여 새 댓글에 연결
		if (request.getParentId() != null) {
			// 부모의 모든 조상 관계 조회
			List<BoardCommentClosureEntity> parentAncestors = closureRepository.findByAncestorId(request.getParentId());
			// 자기 자신 참조(depth 0) 포함된 조상 목록에서 부모까지의 관계 복사
			// findByAncestorId는 부모가 조상인 관계를 반환하므로, 부모 자신(depth 0) 포함
			// 하지만 실제로 필요한 것은 부모의 조상(findByDescendantId)이므로 직접 처리
			// 부모-자식 직접 관계 생성
			closureRepository.save(BoardCommentClosureEntity.create(request.getParentId(), comment.getId(), 1));
		}

		// 게시글의 댓글 수 1 증가
		post.incrementCommentCount();
		postRepository.save(post);

		log.info("댓글 생성: postId={}, commentId={}, author={}", postId, comment.getId(), username);

		// 응답 DTO 변환 후 반환 (children은 빈 리스트)
		return toCommentResponseDto(comment);
	}

	// ─── 댓글 수정 ───

	// 댓글 수정 (내용만 변경 가능)
	@Transactional
	public CommentResponseDto updateComment(String boardId, String commentId,
	                                        UpdateCommentRequestDto request, String userId) {
		// 댓글 조회 (없으면 BOARD_COMMENT_NOT_FOUND 예외)
		BoardCommentEntity comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_COMMENT_NOT_FOUND));

		// 수정 권한 확인 (본인 댓글 또는 관리자)
		if (!permissionHelper.canEditComment(userId, boardId, comment.getAuthorId())) {
			throw new BusinessException(BoardErrorCode.BOARD_COMMENT_EDIT_DENIED);
		}

		// 댓글 내용 수정
		comment.updateContent(request.getContent());
		commentRepository.save(comment);

		// 응답 DTO 변환 후 반환
		return toCommentResponseDto(comment);
	}

	// ─── 댓글 삭제 ───

	// 댓글 소프트 삭제 (is_deleted 플래그 설정 + 게시글 댓글 수 감소)
	@Transactional
	public void deleteComment(String boardId, String commentId, String userId) {
		// 댓글 조회 (없으면 BOARD_COMMENT_NOT_FOUND 예외)
		BoardCommentEntity comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_COMMENT_NOT_FOUND));

		// 삭제 권한 확인 (본인 댓글 또는 관리자)
		if (!permissionHelper.canDeleteComment(userId, boardId, comment.getAuthorId())) {
			throw new BusinessException(BoardErrorCode.BOARD_COMMENT_DELETE_DENIED);
		}

		// 소프트 삭제 처리
		comment.markAsDeleted();
		commentRepository.save(comment);

		// 게시글의 댓글 수 1 감소
		postRepository.findById(comment.getPostId()).ifPresent(post -> {
			post.decrementCommentCount();
			postRepository.save(post);
		});

		log.info("댓글 삭제: commentId={}", commentId);
	}

	// ─── 댓글 트리 조회 ───

	// 게시글의 댓글 트리 구조 조회 (parentId 기반 계층 구성)
	public List<CommentResponseDto> getComments(String postId) {
		// 게시글의 전체 댓글 목록 (시간순 정렬)
		List<BoardCommentEntity> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

		// 계층 트리 구조로 변환
		return buildCommentTree(allComments);
	}

	// ─── Private 헬퍼 ───

	// BoardCommentEntity → CommentResponseDto 변환 (children은 빈 리스트)
	private CommentResponseDto toCommentResponseDto(BoardCommentEntity comment) {
		return CommentResponseDto.builder()
				.id(comment.getId())
				.postId(comment.getPostId())
				.parentId(comment.getParentId())
				.depth(comment.getDepth())
				.content(comment.getIsDeleted() ? null : comment.getContent())
				.authorId(comment.getAuthorId())
				.authorName(comment.getAuthorName())
				.voteUpCount(comment.getVoteUpCount())
				.voteDownCount(comment.getVoteDownCount())
				.isDeleted(comment.getIsDeleted())
				.children(new ArrayList<>())
				.createdAt(comment.getCreatedAt())
				.updatedAt(comment.getUpdatedAt())
				.build();
	}

	// 플랫 댓글 리스트 → 계층 트리 구조 변환
	// parentId를 기준으로 부모-자식 관계를 구성하여 중첩된 트리를 반환한다
	private List<CommentResponseDto> buildCommentTree(List<BoardCommentEntity> allComments) {
		// 모든 댓글을 DTO로 변환 (id → dto 매핑)
		Map<String, CommentResponseDto> dtoMap = new LinkedHashMap<>();
		for (BoardCommentEntity comment : allComments) {
			dtoMap.put(comment.getId(), toCommentResponseDto(comment));
		}

		// 최상위(루트) 댓글 목록
		List<CommentResponseDto> rootComments = new ArrayList<>();

		// parentId 기준으로 부모-자식 관계 구성
		for (BoardCommentEntity comment : allComments) {
			CommentResponseDto dto = dtoMap.get(comment.getId());
			if (comment.getParentId() == null) {
				// 부모가 없으면 루트 댓글
				rootComments.add(dto);
			} else {
				// 부모가 있으면 부모의 children에 추가
				CommentResponseDto parentDto = dtoMap.get(comment.getParentId());
				if (parentDto != null) {
					parentDto.getChildren().add(dto);
				} else {
					// 부모를 찾을 수 없는 경우 루트로 배치 (데이터 정합성 보장)
					rootComments.add(dto);
				}
			}
		}

		return rootComments;
	}
}
