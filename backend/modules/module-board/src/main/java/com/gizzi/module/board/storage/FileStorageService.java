package com.gizzi.module.board.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

// 파일 저장소 서비스 인터페이스
// 로컬 파일시스템, S3 등 다양한 구현체로 교체 가능하도록 추상화한다
public interface FileStorageService {

	// 파일 저장
	// 반환값: 저장된 파일 경로 (상대 경로)
	String store(MultipartFile file, String boardInstanceId);

	// 파일 리소스 로드 (다운로드용)
	Resource loadAsResource(String filePath);

	// 파일 삭제
	void delete(String filePath);

	// 이미지 썸네일 생성
	// 반환값: 썸네일 파일 경로 (null이면 썸네일 생성 실패/불필요)
	String generateThumbnail(String filePath, int width, int height);

	// 저장된 파일명 생성 (UUID 기반)
	String generateStoredName(String originalName);
}
