package com.gizzi.module.board.dto.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 첨부 파일 응답 DTO
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileResponseDto {

	// 파일 PK
	private final String id;

	// 게시글 ID
	private final String postId;

	// 원본 파일명
	private final String originalName;

	// 저장된 파일명
	private final String storedName;

	// 파일 저장 경로
	private final String filePath;

	// 파일 크기 (바이트)
	private final Long fileSize;

	// MIME 타입
	private final String mimeType;

	// 이미지 여부
	private final Boolean isImage;

	// 썸네일 경로
	private final String thumbnailPath;

	// 정렬 순서
	private final Integer sortOrder;

	// 다운로드 횟수
	private final Long downloadCount;

	// 생성 일시
	private final LocalDateTime createdAt;
}
