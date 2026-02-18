package com.gizzi.module.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 게시판 부관리자 엔티티 (tb_board_admins 테이블 매핑)
// 게시판 인스턴스별 부관리자 권한을 가진 사용자를 관리한다
@Entity
@Table(name = "tb_board_admins")
@IdClass(BoardAdminId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardAdminEntity {

	// 게시판 인스턴스 ID (복합 PK 구성 요소)
	@Id
	@Column(name = "board_instance_id", length = 50)
	private String boardInstanceId;

	// 사용자 ID (복합 PK 구성 요소)
	@Id
	@Column(name = "user_id", length = 36)
	private String userId;

	// 생성 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 엔티티 저장 전 생성 일시 자동 설정
	@PrePersist
	private void prePersist() {
		// 생성 일시 자동 설정
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}

	// 게시판 부관리자 생성 팩토리 메서드
	public static BoardAdminEntity create(String boardInstanceId, String userId) {
		BoardAdminEntity entity  = new BoardAdminEntity();
		entity.boardInstanceId   = boardInstanceId;
		entity.userId            = userId;
		return entity;
	}
}
