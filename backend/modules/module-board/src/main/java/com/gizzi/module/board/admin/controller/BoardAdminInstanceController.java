package com.gizzi.module.board.admin.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.module.board.dto.board.BoardListResponseDto;
import com.gizzi.module.board.dto.board.BoardResponseDto;
import com.gizzi.module.board.dto.board.CreateBoardRequestDto;
import com.gizzi.module.board.dto.board.UpdateBoardRequestDto;
import com.gizzi.module.board.dto.settings.BoardSettingsResponseDto;
import com.gizzi.module.board.dto.settings.UpdateBoardSettingsRequestDto;
import com.gizzi.module.board.service.BoardInstanceService;
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

// 게시판 인스턴스 관리 컨트롤러 (admin-api에서만 활성화)
// app.api-type=admin 일 때만 빈으로 등록된다
// 게시판 CRUD + 설정 관리 API를 제공한다
@Slf4j
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "admin")
public class BoardAdminInstanceController {

	// 게시판 인스턴스 서비스
	private final BoardInstanceService boardInstanceService;

	// ─── 게시판 CRUD ───

	// 게시판 생성
	@PostMapping
	public ResponseEntity<ApiResponseDto<BoardResponseDto>> createBoard(
			@Valid @RequestBody CreateBoardRequestDto request,
			Authentication authentication) {
		// 인증된 사용자 ID를 생성자로 설정
		String createdBy = authentication.getName();
		// 게시판 인스턴스 + 설정 동시 생성
		BoardResponseDto response = boardInstanceService.createBoard(request, createdBy);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(response));
	}

	// 게시판 목록 조회
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<BoardListResponseDto>>> getBoards() {
		// 전체 게시판 인스턴스 목록 조회 (생성일시 역순)
		List<BoardListResponseDto> boards = boardInstanceService.getBoards();
		return ResponseEntity.ok(ApiResponseDto.ok(boards));
	}

	// 게시판 상세 조회
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponseDto<BoardResponseDto>> getBoard(
			@PathVariable String id) {
		// 게시판 인스턴스 상세 정보 조회
		BoardResponseDto response = boardInstanceService.getBoard(id);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 게시판 수정
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDto<BoardResponseDto>> updateBoard(
			@PathVariable String id,
			@Valid @RequestBody UpdateBoardRequestDto request,
			Authentication authentication) {
		// 인증된 사용자 ID를 수정자로 설정
		String updatedBy = authentication.getName();
		// 게시판 인스턴스 메타데이터 수정
		BoardResponseDto response = boardInstanceService.updateBoard(id, request, updatedBy);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 게시판 삭제
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponseDto<Void>> deleteBoard(
			@PathVariable String id) {
		// 게시판 인스턴스 삭제 (Settings, Posts, Comments, Files FK cascade 삭제)
		boardInstanceService.deleteBoard(id);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 게시판 설정 ───

	// 게시판 설정 조회
	@GetMapping("/{id}/settings")
	public ResponseEntity<ApiResponseDto<BoardSettingsResponseDto>> getSettings(
			@PathVariable String id) {
		// 게시판 설정 정보 조회
		BoardSettingsResponseDto response = boardInstanceService.getSettings(id);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 게시판 설정 수정
	@PutMapping("/{id}/settings")
	public ResponseEntity<ApiResponseDto<BoardSettingsResponseDto>> updateSettings(
			@PathVariable String id,
			@RequestBody UpdateBoardSettingsRequestDto request) {
		// 게시판 설정 일괄 수정 (null인 필드는 기존 값 유지)
		BoardSettingsResponseDto response = boardInstanceService.updateSettings(id, request);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}
}
