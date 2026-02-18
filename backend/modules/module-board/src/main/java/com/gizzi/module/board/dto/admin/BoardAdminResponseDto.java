package com.gizzi.module.board.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 게시판 관리자 응답 DTO
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardAdminResponseDto {

	// 게시판 인스턴스 ID
	private final String boardInstanceId;

	// 관리자 사용자 ID
	private final String userId;

	// 관리자 사용자 이름
	private final String username;

	// 관리자 등록 일시
	private final LocalDateTime createdAt;
}
