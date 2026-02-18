package com.gizzi.module.board.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// 게시판 수정 요청 DTO
@Getter
public class UpdateBoardRequestDto {

	// 게시판 이름
	@NotBlank(message = "게시판 이름은 필수입니다")
	@Size(max = 100, message = "게시판 이름은 100자 이내여야 합니다")
	private String name;

	// 게시판 슬러그 (URL 경로)
	@NotBlank(message = "슬러그는 필수입니다")
	@Size(min = 2, max = 50, message = "슬러그는 2~50자여야 합니다")
	@Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "슬러그는 영소문자, 숫자, 하이픈만 사용 가능합니다")
	private String slug;

	// 게시판 설명 (선택)
	@Size(max = 500, message = "설명은 500자 이내여야 합니다")
	private String description;
}
