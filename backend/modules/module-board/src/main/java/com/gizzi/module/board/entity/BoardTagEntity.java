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

// 게시판 태그 엔티티 (tb_board_tags 테이블 매핑)
// 게시글에 부여할 수 있는 태그를 관리하고 사용 빈도를 추적한다
@Entity
@Table(name = "tb_board_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardTagEntity {

	// 태그 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 게시판 인스턴스 ID (어떤 게시판에 속하는지)
	@Column(name = "board_instance_id", nullable = false, length = 50)
	private String boardInstanceId;

	// 태그 이름 (예: "Java", "Spring Boot")
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// URL 슬러그 (예: "java", "spring-boot")
	@Column(name = "slug", nullable = false, length = 100)
	private String slug;

	// 이 태그가 부여된 게시글 수
	@Column(name = "post_count", nullable = false)
	private Integer postCount;

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

	// 태그 생성 팩토리 메서드
	public static BoardTagEntity create(String boardInstanceId, String name, String slug) {
		BoardTagEntity entity  = new BoardTagEntity();
		entity.boardInstanceId = boardInstanceId;
		entity.name            = name;
		entity.slug            = slug;
		entity.postCount       = 0;
		return entity;
	}

	// 태그 사용 게시글 수 증가
	public void incrementPostCount() {
		this.postCount = this.postCount + 1;
		this.updatedAt = LocalDateTime.now();
	}

	// 태그 사용 게시글 수 감소 (0 미만 방지)
	public void decrementPostCount() {
		if (this.postCount > 0) {
			this.postCount = this.postCount - 1;
		}
		this.updatedAt = LocalDateTime.now();
	}
}
