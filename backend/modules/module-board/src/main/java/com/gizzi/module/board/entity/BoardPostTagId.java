package com.gizzi.module.board.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 게시글-태그 연결 복합 키
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BoardPostTagId implements Serializable {

	// 게시글 ID
	private String postId;

	// 태그 ID
	private String tagId;
}
