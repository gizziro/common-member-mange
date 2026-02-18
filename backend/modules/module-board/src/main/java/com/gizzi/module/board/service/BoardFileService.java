package com.gizzi.module.board.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.board.dto.file.FileResponseDto;
import com.gizzi.module.board.entity.BoardFileEntity;
import com.gizzi.module.board.entity.BoardSettingsEntity;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.repository.BoardFileRepository;
import com.gizzi.module.board.repository.BoardSettingsRepository;
import com.gizzi.module.board.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 게시판 첨부파일 관리 서비스
// 파일 업로드/다운로드/삭제 및 파일 제한 검증을 수행한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardFileService {

	// 파일 DB 리포지토리
	private final BoardFileRepository    fileRepository;

	// 게시판 설정 리포지토리 (파일 업로드 제한 조회)
	private final BoardSettingsRepository settingsRepository;

	// 파일 저장소 서비스 (실제 파일 저장/로드/삭제 위임)
	private final FileStorageService     storageService;

	// 썸네일 기본 크기 (픽셀)
	private static final int THUMBNAIL_WIDTH  = 200;
	private static final int THUMBNAIL_HEIGHT = 200;

	// ─── 파일 업로드 ───

	// 파일 업로드 (설정 기반 검증 → 저장 → 썸네일 생성 → DB 등록)
	@Transactional
	public FileResponseDto uploadFile(String boardId, String postId,
	                                  MultipartFile file, String userId) {
		// 1. 게시판 설정 조회
		BoardSettingsEntity settings = settingsRepository.findById(boardId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));

		// 2. 파일 업로드 허용 여부 확인
		if (!settings.getAllowFileUpload()) {
			throw new BusinessException(BoardErrorCode.BOARD_FILE_UPLOAD_DISABLED);
		}

		// 3. 파일 타입(확장자) 검증
		String originalName = file.getOriginalFilename();
		if (settings.getAllowedFileTypes() != null && !settings.getAllowedFileTypes().isBlank()) {
			validateFileType(originalName, settings.getAllowedFileTypes());
		}

		// 4. 파일 크기 검증 (바이트 단위)
		if (file.getSize() > settings.getMaxFileSize()) {
			throw new BusinessException(BoardErrorCode.BOARD_FILE_SIZE_EXCEEDED);
		}

		// 5. 게시글당 최대 파일 수 검증
		long currentFileCount = fileRepository.countByPostId(postId);
		if (currentFileCount >= settings.getMaxFilesPerPost()) {
			throw new BusinessException(BoardErrorCode.BOARD_FILE_COUNT_EXCEEDED);
		}

		// 6. 파일 저장소에 실제 파일 저장
		String filePath = storageService.store(file, boardId);

		// 7. 저장된 파일명 생성 (UUID 기반)
		String storedName = storageService.generateStoredName(originalName);

		// 8. MIME 타입 확인
		String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

		// 9. 이미지 여부 판단 (MIME 타입 기반)
		boolean isImage = mimeType.startsWith("image/");

		// 10. 이미지인 경우 썸네일 생성
		String thumbnailPath = null;
		if (isImage) {
			thumbnailPath = storageService.generateThumbnail(filePath, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
		}

		// 11. 파일 엔티티 생성 + DB 저장
		int sortOrder = (int) currentFileCount;
		BoardFileEntity fileEntity = BoardFileEntity.create(
				postId, originalName, storedName, filePath,
				file.getSize(), mimeType, isImage, thumbnailPath, sortOrder
		);
		fileRepository.save(fileEntity);

		log.info("파일 업로드: {} → {} (postId: {}, size: {}bytes)",
				originalName, filePath, postId, file.getSize());

		// 응답 DTO 변환 후 반환
		return toFileResponseDto(fileEntity);
	}

	// ─── 파일 다운로드 ───

	// 파일 리소스 로드 + 다운로드 횟수 증가
	@Transactional
	public Resource downloadFile(String fileId) {
		// 파일 엔티티 조회 (없으면 BOARD_FILE_NOT_FOUND 예외)
		BoardFileEntity fileEntity = fileRepository.findById(fileId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_FILE_NOT_FOUND));

		// 다운로드 횟수 1 증가
		fileEntity.incrementDownloadCount();
		fileRepository.save(fileEntity);

		// 저장소에서 파일 리소스 로드
		return storageService.loadAsResource(fileEntity.getFilePath());
	}

	// ─── 파일 삭제 ───

	// 파일 삭제 (저장소 실제 파일 + DB 레코드 동시 삭제)
	@Transactional
	public void deleteFile(String fileId) {
		// 파일 엔티티 조회 (없으면 BOARD_FILE_NOT_FOUND 예외)
		BoardFileEntity fileEntity = fileRepository.findById(fileId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_FILE_NOT_FOUND));

		// 저장소에서 실제 파일 삭제
		storageService.delete(fileEntity.getFilePath());

		// 썸네일이 있으면 썸네일도 삭제
		if (fileEntity.getThumbnailPath() != null) {
			storageService.delete(fileEntity.getThumbnailPath());
		}

		// DB에서 파일 레코드 삭제
		fileRepository.delete(fileEntity);

		log.info("파일 삭제: {} (fileId: {})", fileEntity.getOriginalName(), fileId);
	}

	// ─── 파일 목록 조회 ───

	// 게시글의 첨부파일 목록 조회 (정렬 순서대로)
	public List<FileResponseDto> getFilesByPostId(String postId) {
		return fileRepository.findByPostIdOrderBySortOrderAsc(postId).stream()
				.map(this::toFileResponseDto)
				.collect(Collectors.toList());
	}

	// ─── Private 헬퍼 ───

	// BoardFileEntity → FileResponseDto 변환
	private FileResponseDto toFileResponseDto(BoardFileEntity entity) {
		return FileResponseDto.builder()
				.id(entity.getId())
				.postId(entity.getPostId())
				.originalName(entity.getOriginalName())
				.storedName(entity.getStoredName())
				.filePath(entity.getFilePath())
				.fileSize(entity.getFileSize())
				.mimeType(entity.getMimeType())
				.isImage(entity.getIsImage())
				.thumbnailPath(entity.getThumbnailPath())
				.sortOrder(entity.getSortOrder())
				.downloadCount(entity.getDownloadCount() != null ? entity.getDownloadCount().longValue() : 0L)
				.createdAt(entity.getCreatedAt())
				.build();
	}

	// 파일 확장자 검증 (허용 목록에 포함되는지 확인)
	private void validateFileType(String originalName, String allowedTypes) {
		// 파일 확장자 추출
		String extension = getFileExtension(originalName);
		if (extension.isEmpty()) {
			// 확장자가 없는 파일은 거부
			throw new BusinessException(BoardErrorCode.BOARD_FILE_TYPE_NOT_ALLOWED);
		}

		// 허용 확장자 목록 파싱 (콤마 구분 → Set 변환)
		Set<String> allowed = Arrays.stream(allowedTypes.split(","))
				.map(String::trim)
				.map(String::toLowerCase)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());

		// 확장자가 허용 목록에 없으면 거부
		if (!allowed.contains(extension.toLowerCase())) {
			throw new BusinessException(BoardErrorCode.BOARD_FILE_TYPE_NOT_ALLOWED);
		}
	}

	// 파일명에서 확장자 추출 (마지막 '.' 이후 문자열)
	private String getFileExtension(String fileName) {
		// null이거나 '.'이 없으면 빈 문자열 반환
		if (fileName == null || !fileName.contains(".")) {
			return "";
		}
		return fileName.substring(fileName.lastIndexOf(".") + 1);
	}
}
