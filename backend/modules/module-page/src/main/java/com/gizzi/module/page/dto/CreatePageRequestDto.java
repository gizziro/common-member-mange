package com.gizzi.module.page.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// 페이지 생성 요청 DTO
@Getter
public class CreatePageRequestDto {

	// 페이지 슬러그 (영소문자+숫자+하이픈)
	@NotBlank(message = "슬러그는 필수입니다")
	@Size(min = 2, max = 100, message = "슬러그는 2~100자여야 합니다")
	@Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "슬러그는 영소문자, 숫자, 하이픈만 사용 가능합니다")
	private String slug;

	// 페이지 제목
	@NotBlank(message = "제목은 필수입니다")
	@Size(max = 200, message = "제목은 200자 이내여야 합니다")
	private String title;

	// 페이지 본문 (선택)
	private String content;

	// 콘텐츠 유형 (HTML / MARKDOWN / TEXT, 기본값: HTML)
	private String contentType;
}
