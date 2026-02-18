package com.gizzi.module.board.admin.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.module.board.dto.comment.CommentResponseDto;
import com.gizzi.module.board.service.BoardCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 게시판 댓글 관리 컨트롤러 (admin-api에서만 활성화)
// app.api-type=admin 일 때만 빈으로 등록된다
// 관리자용 댓글 목록 조회 및 삭제 API를 제공한다
@Slf4j
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "admin")
public class BoardAdminCommentController {

	// 댓글 서비스
	private final BoardCommentService commentService;

	// ─── 관리자용 댓글 관리 ───

	// 게시글의 댓글 목록 조회 (트리 구조로 반환)
	@GetMapping("/{id}/posts/{postId}/comments")
	public ResponseEntity<ApiResponseDto<List<CommentResponseDto>>> getComments(
			@PathVariable String id,
			@PathVariable String postId) {
		// 게시글의 전체 댓글을 계층 트리 구조로 조회
		List<CommentResponseDto> comments = commentService.getComments(postId);
		return ResponseEntity.ok(ApiResponseDto.ok(comments));
	}

	// 댓글 삭제 (관리자 권한)
	@DeleteMapping("/{id}/comments/{commentId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteComment(
			@PathVariable String id,
			@PathVariable String commentId,
			Authentication authentication) {
		// 인증된 사용자 ID 추출
		String userId = authentication.getName();
		// 댓글 소프트 삭제 (관리자 권한으로 처리, boardId=id 전달)
		commentService.deleteComment(id, commentId, userId);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}
}
