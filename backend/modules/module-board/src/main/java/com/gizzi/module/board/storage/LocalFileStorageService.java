package com.gizzi.module.board.storage;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.board.exception.BoardErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

import javax.imageio.ImageIO;

// 로컬 파일시스템 기반 파일 저장소 구현체
// 저장 경로: {upload-dir}/{boardInstanceId}/{year}/{month}/{uuid}.{ext}
// 향후 S3 등으로 교체 시 FileStorageService 인터페이스의 새 구현체를 만들면 된다
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

	// 파일 저장 루트 디렉토리 (application.yml에서 설정)
	private final Path rootLocation;

	// 썸네일 기본 너비
	private static final int DEFAULT_THUMBNAIL_WIDTH  = 200;

	// 썸네일 기본 높이
	private static final int DEFAULT_THUMBNAIL_HEIGHT = 200;

	// 생성자 — 저장 루트 디렉토리 초기화
	public LocalFileStorageService(@Value("${app.board.upload-dir:./uploads/board}") String uploadDir) {
		this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
		try {
			// 루트 디렉토리가 없으면 생성
			Files.createDirectories(this.rootLocation);
			log.info("파일 저장소 초기화: {}", this.rootLocation);
		} catch (IOException e) {
			log.error("파일 저장소 디렉토리 생성 실패: {}", this.rootLocation, e);
			throw new BusinessException(BoardErrorCode.BOARD_FILE_STORAGE_ERROR);
		}
	}

	// 파일 저장 — {boardInstanceId}/{year}/{month}/{storedName}
	@Override
	public String store(MultipartFile file, String boardInstanceId) {
		try {
			// 저장 경로 구성: boardInstanceId/year/month
			LocalDate now = LocalDate.now();
			String subPath = boardInstanceId + "/" + now.getYear() + "/" +
			                 String.format("%02d", now.getMonthValue());

			// 저장 디렉토리 생성
			Path targetDir = rootLocation.resolve(subPath);
			Files.createDirectories(targetDir);

			// UUID 기반 저장 파일명 생성
			String storedName = generateStoredName(file.getOriginalFilename());
			Path targetFile = targetDir.resolve(storedName);

			// 파일 복사
			Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

			// 상대 경로 반환
			String relativePath = subPath + "/" + storedName;
			log.debug("파일 저장 완료: {} → {}", file.getOriginalFilename(), relativePath);

			return relativePath;
		} catch (IOException e) {
			log.error("파일 저장 실패: {}", file.getOriginalFilename(), e);
			throw new BusinessException(BoardErrorCode.BOARD_FILE_STORAGE_ERROR);
		}
	}

	// 파일 리소스 로드 (다운로드용)
	@Override
	public Resource loadAsResource(String filePath) {
		try {
			// 루트 경로 기준으로 절대 경로 생성
			Path file = rootLocation.resolve(filePath).normalize();
			Resource resource = new UrlResource(file.toUri());

			// 리소스 존재 여부 확인
			if (resource.exists() && resource.isReadable()) {
				return resource;
			} else {
				log.warn("파일을 찾을 수 없음: {}", filePath);
				throw new BusinessException(BoardErrorCode.BOARD_FILE_NOT_FOUND);
			}
		} catch (MalformedURLException e) {
			log.error("파일 경로 오류: {}", filePath, e);
			throw new BusinessException(BoardErrorCode.BOARD_FILE_NOT_FOUND);
		}
	}

	// 파일 삭제
	@Override
	public void delete(String filePath) {
		try {
			Path file = rootLocation.resolve(filePath).normalize();
			Files.deleteIfExists(file);
			log.debug("파일 삭제: {}", filePath);
		} catch (IOException e) {
			log.warn("파일 삭제 실패 (무시): {}", filePath, e);
		}
	}

	// 이미지 썸네일 생성
	@Override
	public String generateThumbnail(String filePath, int width, int height) {
		try {
			Path sourcePath = rootLocation.resolve(filePath).normalize();

			// 원본 이미지 읽기
			BufferedImage originalImage = ImageIO.read(sourcePath.toFile());
			if (originalImage == null) {
				log.debug("이미지 읽기 실패 (지원하지 않는 형식): {}", filePath);
				return null;
			}

			// 리사이즈
			int targetWidth  = width > 0 ? width : DEFAULT_THUMBNAIL_WIDTH;
			int targetHeight = height > 0 ? height : DEFAULT_THUMBNAIL_HEIGHT;
			BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = thumbnail.createGraphics();
			graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
			graphics.dispose();

			// 썸네일 파일명 생성 (원본 경로에 _thumb 접미사)
			String thumbPath = filePath.replaceFirst("(\\.[^.]+)$", "_thumb$1");
			Path thumbTarget = rootLocation.resolve(thumbPath).normalize();

			// 썸네일 저장
			String extension = getFileExtension(filePath).toLowerCase();
			String formatName = "jpg".equals(extension) || "jpeg".equals(extension) ? "JPEG" : "PNG";
			ImageIO.write(thumbnail, formatName, thumbTarget.toFile());

			log.debug("썸네일 생성 완료: {} ({}x{})", thumbPath, targetWidth, targetHeight);
			return thumbPath;

		} catch (IOException e) {
			log.warn("썸네일 생성 실패 (무시): {}", filePath, e);
			return null;
		}
	}

	// UUID 기반 저장 파일명 생성
	@Override
	public String generateStoredName(String originalName) {
		String extension = getFileExtension(originalName);
		return UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
	}

	// 파일 확장자 추출
	private String getFileExtension(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			return "";
		}
		return fileName.substring(fileName.lastIndexOf(".") + 1);
	}
}
