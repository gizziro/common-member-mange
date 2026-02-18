package com.gizzi.module.board.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 게시판 부관리자 복합 키
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BoardAdminId implements Serializable {

	// 게시판 인스턴스 ID
	private String boardInstanceId;

	// 사용자 ID
	private String userId;
}
