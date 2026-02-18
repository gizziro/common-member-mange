package com.gizzi.module.board.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 댓글 Closure Table 복합 키
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BoardCommentClosureId implements Serializable {

	// 조상 댓글 ID
	private String ancestorId;

	// 자손 댓글 ID
	private String descendantId;
}
