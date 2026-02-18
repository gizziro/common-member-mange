package com.gizzi.module.board.admin.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.module.board.dto.post.PostListResponseDto;
import com.gizzi.module.board.dto.post.PostResponseDto;
import com.gizzi.module.board.service.BoardPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 게시판 게시글 관리 컨트롤러 (admin-api에서만 활성화)
// app.api-type=admin 일 때만 빈으로 등록된다
// 관리자용 게시글 목록 조회, 삭제, 공지 토글 API를 제공한다
@Slf4j
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "admin")
public class BoardAdminPostController {

	// 게시글 서비스
	private final BoardPostService postService;

	// ─── 관리자용 게시글 관리 ───

	// 관리자용 게시글 목록 조회 (임시저장 포함, 소프트삭제 제외)
	@GetMapping("/{id}/posts")
	public ResponseEntity<ApiResponseDto<Page<PostListResponseDto>>> getAdminPosts(
			@PathVariable String id,
			Pageable pageable) {
		// 관리자 전용 목록 — 임시저장(isDraft) 포함하여 전체 조회
		Page<PostListResponseDto> posts = postService.getAdminPosts(id, pageable);
		return ResponseEntity.ok(ApiResponseDto.ok(posts));
	}

	// 게시글 상세 조회
	@GetMapping("/{id}/posts/{postId}")
	public ResponseEntity<ApiResponseDto<PostResponseDto>> getPost(
			@PathVariable String id,
			@PathVariable String postId,
			Authentication authentication) {
		// 인증된 사용자 ID 추출
		String userId = authentication.getName();
		// 게시글 상세 조회 (조회수 증가 포함)
		PostResponseDto response = postService.getPost(id, postId, userId);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 게시글 삭제 (관리자 권한)
	@DeleteMapping("/{id}/posts/{postId}")
	public ResponseEntity<ApiResponseDto<Void>> deletePost(
			@PathVariable String id,
			@PathVariable String postId,
			Authentication authentication) {
		// 인증된 사용자 ID 추출
		String userId = authentication.getName();
		// 게시글 소프트 삭제 (관리자 권한으로 처리)
		postService.deletePost(id, postId, userId);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// 공지글 토글 (공지 설정/해제 전환)
	@PatchMapping("/{id}/posts/{postId}/notice")
	public ResponseEntity<ApiResponseDto<PostResponseDto>> toggleNotice(
			@PathVariable String postId,
			@RequestParam(required = false) String scope) {
		// 공지 상태 토글 (scope: BOARD 또는 GLOBAL, 기본값 BOARD)
		PostResponseDto response = postService.toggleNotice(postId, scope);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
