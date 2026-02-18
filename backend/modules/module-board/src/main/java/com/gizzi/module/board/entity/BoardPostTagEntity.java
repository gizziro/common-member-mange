package com.gizzi.module.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 게시글-태그 연결 엔티티 (tb_board_post_tags 테이블 매핑)
// 게시글과 태그 간의 다대다 관계를 관리한다
@Entity
@Table(name = "tb_board_post_tags")
@IdClass(BoardPostTagId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPostTagEntity {

	// 게시글 ID (복합 PK 구성 요소)
	@Id
	@Column(name = "post_id", length = 36)
	private String postId;

	// 태그 ID (복합 PK 구성 요소)
	@Id
	@Column(name = "tag_id", length = 36)
	private String tagId;

	// 게시글-태그 연결 생성 팩토리 메서드
	public static BoardPostTagEntity create(String postId, String tagId) {
		BoardPostTagEntity entity = new BoardPostTagEntity();
		entity.postId             = postId;
		entity.tagId              = tagId;
		return entity;
	}
}
