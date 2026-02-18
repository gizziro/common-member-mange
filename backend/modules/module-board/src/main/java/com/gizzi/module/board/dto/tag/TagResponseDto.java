package com.gizzi.module.board.dto.tag;

import lombok.Builder;
import lombok.Getter;

// 태그 응답 DTO
// 게시판의 태그 목록 조회 시 사용한다
@Getter
@Builder
public class TagResponseDto {

	// 태그 ID (UUID)
	private String  id;

	// 태그 이름
	private String  name;

	// URL 슬러그
	private String  slug;

	// 이 태그가 부여된 게시글 수
	private Integer postCount;
}
