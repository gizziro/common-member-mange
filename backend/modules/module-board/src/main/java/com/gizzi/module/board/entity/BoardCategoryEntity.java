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

// 게시판 카테고리 엔티티 (tb_board_categories 테이블 매핑)
// 각 게시판 인스턴스 내에서 게시글을 분류하는 카테고리를 관리한다
@Entity
@Table(name = "tb_board_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardCategoryEntity {

	// 카테고리 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 게시판 인스턴스 ID (어떤 게시판에 속하는지)
	@Column(name = "board_instance_id", nullable = false, length = 50)
	private String boardInstanceId;

	// 카테고리 이름 (예: "공지", "질문")
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// URL 슬러그 (예: "notice", "question")
	@Column(name = "slug", nullable = false, length = 50)
	private String slug;

	// 카테고리 설명
	@Column(name = "description", length = 500)
	private String description;

	// 정렬 순서 (낮을수록 먼저 표시)
	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	// 활성화 여부
	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	// 생성 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 수정 일시
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 엔티티 저장 전 UUID PK + 생성/수정 일시 자동 생성
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

	// 카테고리 생성 팩토리 메서드
	public static BoardCategoryEntity create(String boardInstanceId, String name,
	                                          String slug, String description, int sortOrder) {
		BoardCategoryEntity entity = new BoardCategoryEntity();
		entity.boardInstanceId     = boardInstanceId;
		entity.name                = name;
		entity.slug                = slug;
		entity.description         = description;
		entity.sortOrder           = sortOrder;
		entity.isActive            = true;
		return entity;
	}

	// 카테고리 정보 수정
	public void updateInfo(String name, String slug, String description, int sortOrder) {
		this.name        = name;
		this.slug        = slug;
		this.description = description;
		this.sortOrder   = sortOrder;
		this.updatedAt   = LocalDateTime.now();
	}

	// 활성화/비활성화 토글
	public void toggleActive() {
		this.isActive  = !this.isActive;
		this.updatedAt = LocalDateTime.now();
	}
}
