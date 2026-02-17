package com.gizzi.module.page.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 페이지 엔티티 (tb_page_pages 테이블 매핑)
// 텍스트/HTML/Markdown 페이지를 관리한다
@Entity
@Table(name = "tb_page_pages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageEntity {

	// 페이지 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// URL 슬러그 (유니크, 예: "about", "terms")
	@Column(name = "slug", nullable = false, length = 100, unique = true)
	private String slug;

	// 페이지 제목
	@Column(name = "title", nullable = false, length = 200)
	private String title;

	// 페이지 본문 (LONGTEXT)
	@Lob
	@Column(name = "content", columnDefinition = "LONGTEXT")
	private String content;

	// 콘텐츠 유형 (HTML / MARKDOWN / TEXT)
	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", nullable = false, length = 20)
	private PageContentType contentType;

	// 공개 여부
	@Column(name = "is_published", nullable = false)
	private Boolean isPublished;

	// 정렬 순서
	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	// 연결된 모듈 인스턴스 ID (권한 관리용)
	@Column(name = "module_instance_id", length = 50)
	private String moduleInstanceId;

	// 생성자
	@Column(name = "created_by", nullable = false, length = 100)
	private String createdBy;

	// 생성 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 수정자
	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	// 수정 일시
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 엔티티 저장 전 UUID PK + 생성 일시 자동 생성
	@PrePersist
	private void prePersist() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
		this.updatedAt = LocalDateTime.now();
	}

	// 페이지 생성 팩토리 메서드
	public static PageEntity create(String slug, String title, String content,
	                                 PageContentType contentType, String moduleInstanceId,
	                                 String createdBy) {
		PageEntity entity      = new PageEntity();
		entity.slug             = slug;
		entity.title            = title;
		entity.content          = content;
		entity.contentType      = contentType != null ? contentType : PageContentType.HTML;
		entity.isPublished      = false;
		entity.sortOrder        = 0;
		entity.moduleInstanceId = moduleInstanceId;
		entity.createdBy        = createdBy;
		return entity;
	}

	// 페이지 정보 수정
	public void updateInfo(String title, String slug, String content,
	                        PageContentType contentType, String updatedBy) {
		this.title       = title;
		this.slug        = slug;
		this.content     = content;
		this.contentType = contentType;
		this.updatedBy   = updatedBy;
		this.updatedAt   = LocalDateTime.now();
	}

	// 모듈 인스턴스 ID 설정 (레거시 데이터 마이그레이션용)
	public void setModuleInstanceId(String moduleInstanceId) {
		this.moduleInstanceId = moduleInstanceId;
	}

	// 공개/비공개 토글
	public void togglePublish() {
		this.isPublished = !this.isPublished;
		this.updatedAt   = LocalDateTime.now();
	}
}
