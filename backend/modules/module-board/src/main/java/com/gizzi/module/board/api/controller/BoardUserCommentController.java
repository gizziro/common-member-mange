package com.gizzi.module.board.api.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.exception.AuthErrorCode;
import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.board.dto.comment.CommentResponseDto;
import com.gizzi.module.board.dto.comment.CreateCommentRequestDto;
import com.gizzi.module.board.dto.comment.UpdateCommentRequestDto;
import com.gizzi.module.board.dto.vote.VoteRequestDto;
import com.gizzi.module.board.dto.vote.VoteResponseDto;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.service.BoardCommentService;
import com.gizzi.module.board.service.BoardPermissionHelper;
import com.gizzi.module.board.service.BoardVoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 사용자 댓글 컨트롤러 (user-api에서만 활성화)
// 댓글 CRUD + 투표 기능을 제공한다
// /boards/** 는 SecurityConfig에서 permitAll이므로 비로그인 사용자도 접근 가능
// Authentication이 null이면 비로그인, 있으면 userId로 권한 체크
@Slf4j
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "user")
public class BoardUserCommentController {

	// 댓글 서비스
	private final BoardCommentService    commentService;

	// 투표 서비스
	private final BoardVoteService       voteService;

	// 게시판 권한 헬퍼
	private final BoardPermissionHelper  permissionHelper;

	// ─── 댓글 목록 조회 (트리 구조) ───

	// 게시글의 댓글 트리 구조 조회 (비로그인도 가능, 게시판 접근 권한 체크)
	@GetMapping("/{id}/posts/{postId}/comments")
	public ResponseEntity<ApiResponseDto<List<CommentResponseDto>>> getComments(
			@PathVariable String id,
			@PathVariable String postId,
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인 시 null)
		String userId = extractUserId(authentication);

		// 게시판 접근 권한 확인
		if (!permissionHelper.canAccessBoard(userId, id)) {
			throw new BusinessException(BoardErrorCode.BOARD_ACCESS_DENIED);
		}

		// 댓글 트리 구조 조회
		List<CommentResponseDto> comments = commentService.getComments(postId);
		return ResponseEntity.ok(ApiResponseDto.ok(comments));
	}

	// ─── 댓글 작성 ───

	// 댓글 작성 (인증 필수, 댓글 작성 권한 체크)
	@PostMapping("/{id}/posts/{postId}/comments")
	public ResponseEntity<ApiResponseDto<CommentResponseDto>> createComment(
			@PathVariable String id,
			@PathVariable String postId,
			@Valid @RequestBody CreateCommentRequestDto request,
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인이면 인증 오류)
		String userId = extractUserId(authentication);
		if (userId == null) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
		}

		// 댓글 작성 권한 확인
		if (!permissionHelper.canWriteComment(userId, id)) {
			throw new BusinessException(BoardErrorCode.BOARD_ACCESS_DENIED);
		}

		// 댓글 생성 (userId를 username으로도 전달, 서비스에서 실제 사용자명 조회 가능)
		CommentResponseDto response = commentService.createComment(id, postId, request, userId, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(response));
	}

	// ─── 댓글 수정 ───

	// 댓글 수정 (인증 필수, 본인 댓글 또는 관리자만 수정 가능)
	@PutMapping("/{id}/comments/{commentId}")
	public ResponseEntity<ApiResponseDto<CommentResponseDto>> updateComment(
			@PathVariable String id,
			@PathVariable String commentId,
			@Valid @RequestBody UpdateCommentRequestDto request,
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인이면 인증 오류)
		String userId = extractUserId(authentication);
		if (userId == null) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
		}

		// 댓글 수정 (수정 권한 체크는 서비스에서 처리)
		CommentResponseDto response = commentService.updateComment(id, commentId, request, userId);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// ─── 댓글 삭제 ───

	// 댓글 삭제 (인증 필수, 본인 댓글 또는 관리자만 삭제 가능)
	@DeleteMapping("/{id}/comments/{commentId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteComment(
			@PathVariable String id,
			@PathVariable String commentId,
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인이면 인증 오류)
		String userId = extractUserId(authentication);
		if (userId == null) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
		}

		// 댓글 삭제 (삭제 권한 체크는 서비스에서 처리)
		commentService.deleteComment(id, commentId, userId);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 댓글 추천/비추천 ───

	// 댓글 추천 또는 비추천 투표 (인증 필수, 본인 댓글 투표 불가)
	@PostMapping("/{id}/comments/{commentId}/vote")
	public ResponseEntity<ApiResponseDto<VoteResponseDto>> voteComment(
			@PathVariable String id,
			@PathVariable String commentId,
			@Valid @RequestBody VoteRequestDto request,
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인이면 인증 오류)
		String userId = extractUserId(authentication);
		if (userId == null) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
		}

		// 투표 권한 확인
		if (!permissionHelper.canVote(userId, id)) {
			throw new BusinessException(BoardErrorCode.BOARD_ACCESS_DENIED);
		}

		// 댓글 투표 처리 (중복 투표 → 취소, 다른 타입 → 변경)
		VoteResponseDto response = voteService.voteComment(id, commentId, userId, request);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// ─── Private 헬퍼 ───

	// Authentication에서 userId 추출 (null 안전)
	private String extractUserId(Authentication authentication) {
		// 인증 정보가 없거나 인증되지 않은 경우 null 반환
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		// authentication.getName()은 사용자 PK (UUID)를 반환
		return authentication.getName();
	}
}
