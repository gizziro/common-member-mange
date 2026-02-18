package com.gizzi.module.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 게시판 첨부파일 엔티티 (tb_board_files 테이블 매핑)
// 게시글에 첨부된 파일 정보를 관리한다
@Entity
@Table(name = "tb_board_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardFileEntity {

	// 파일 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 게시글 ID (FK)
	@Column(name = "post_id", nullable = false, length = 36)
	private String postId;

	// 원본 파일명
	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName;

	// 저장된 파일명 (UUID 기반)
	@Column(name = "stored_name", nullable = false, length = 255)
	private String storedName;

	// 파일 저장 경로
	@Column(name = "file_path", nullable = false, length = 500)
	private String filePath;

	// 파일 크기 (바이트)
	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	// MIME 타입 (예: image/png, application/pdf)
	@Column(name = "mime_type", nullable = false, length = 100)
	private String mimeType;

	// 이미지 여부
	@Column(name = "is_image", nullable = false)
	private Boolean isImage;

	// 썸네일 저장 경로 (이미지인 경우에만)
	@Column(name = "thumbnail_path", length = 500)
	private String thumbnailPath;

	// 정렬 순서
	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	// 다운로드 횟수
	@Column(name = "download_count", nullable = false)
	private Integer downloadCount;

	// 생성 일시 (수정 일시 없음)
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 엔티티 저장 전 UUID PK + 생성 일시 자동 생성
	@PrePersist
	private void prePersist() {
		// PK가 없으면 UUID 자동 생성
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
		// 생성 일시 자동 설정
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}

	// 첨부파일 생성 팩토리 메서드
	public static BoardFileEntity create(String postId, String originalName, String storedName,
	                                     String filePath, long fileSize, String mimeType,
	                                     boolean isImage, String thumbnailPath, int sortOrder) {
		BoardFileEntity entity = new BoardFileEntity();
		entity.postId          = postId;
		entity.originalName    = originalName;
		entity.storedName      = storedName;
		entity.filePath        = filePath;
		entity.fileSize        = fileSize;
		entity.mimeType        = mimeType;
		entity.isImage         = isImage;
		entity.thumbnailPath   = thumbnailPath;
		entity.sortOrder       = sortOrder;
		entity.downloadCount   = 0;
		return entity;
	}

	// 다운로드 횟수 1 증가
	public void incrementDownloadCount() {
		this.downloadCount++;
	}
}
