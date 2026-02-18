package com.gizzi.module.board.api.controller;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.common.exception.AuthErrorCode;
import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.board.dto.file.FileResponseDto;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.service.BoardFileService;
import com.gizzi.module.board.service.BoardPermissionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 사용자 파일 컨트롤러 (user-api에서만 활성화)
// 첨부파일 업로드/다운로드/삭제 기능을 제공한다
// /boards/** 는 SecurityConfig에서 permitAll이므로 비로그인 사용자도 접근 가능
// 단, 업로드/삭제는 인증 필수
@Slf4j
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.api-type", havingValue = "user")
public class BoardUserFileController {

	// 파일 서비스
	private final BoardFileService       fileService;

	// 게시판 권한 헬퍼
	private final BoardPermissionHelper  permissionHelper;

	// ─── 파일 업로드 ───

	// 파일 업로드 (인증 필수, 파일 업로드 권한 체크)
	@PostMapping("/{id}/files")
	public ResponseEntity<ApiResponseDto<FileResponseDto>> uploadFile(
			@PathVariable String id,
			@RequestParam("file") MultipartFile file,
			@RequestParam("postId") String postId,
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인이면 인증 오류)
		String userId = extractUserId(authentication);
		if (userId == null) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
		}

		// 파일 업로드 권한 확인
		if (!permissionHelper.canUploadFile(userId, id)) {
			throw new BusinessException(BoardErrorCode.BOARD_ACCESS_DENIED);
		}

		// 파일 업로드 처리 (설정 기반 검증 → 저장 → 썸네일 생성 → DB 등록)
		FileResponseDto response = fileService.uploadFile(id, postId, file, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(response));
	}

	// ─── 파일 다운로드 ───

	// 파일 다운로드 (인증 불필요, 파일 ID로 직접 다운로드)
	@GetMapping("/files/{fileId}/download")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
		// 파일 리소스 로드 (다운로드 횟수 자동 증가)
		Resource resource = fileService.downloadFile(fileId);

		// Content-Disposition 헤더 설정 (첨부파일 다운로드)
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	// ─── 파일 삭제 ───

	// 파일 삭제 (인증 필수, 저장소 실제 파일 + DB 레코드 동시 삭제)
	@DeleteMapping("/{id}/files/{fileId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteFile(
			@PathVariable String id,
			@PathVariable String fileId,
			Authentication authentication) {
		// 인증 정보에서 userId 추출 (비로그인이면 인증 오류)
		String userId = extractUserId(authentication);
		if (userId == null) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
		}

		// 관리자 권한 확인 (파일 삭제는 관리자 또는 업로드 권한 보유자)
		if (!permissionHelper.canUploadFile(userId, id)) {
			throw new BusinessException(BoardErrorCode.BOARD_ACCESS_DENIED);
		}

		// 파일 삭제 처리 (저장소 실제 파일 + DB 레코드 + 썸네일 삭제)
		fileService.deleteFile(fileId);
		return ResponseEntity.ok(ApiResponseDto.ok());
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
