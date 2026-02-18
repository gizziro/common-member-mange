package com.gizzi.module.board.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 게시글 Closure Table 복합 키
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BoardPostClosureId implements Serializable {

	// 조상 게시글 ID
	private String ancestorId;

	// 자손 게시글 ID
	private String descendantId;
}
