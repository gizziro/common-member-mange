package com.gizzi.module.board.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

// 게시글 생성 요청 DTO
@Getter
public class CreatePostRequestDto {

	// 게시글 제목
	@NotBlank(message = "제목은 필수입니다")
	@Size(max = 300, message = "제목은 300자 이내여야 합니다")
	private String title;

	// 게시글 본문 (선택)
	private String content;

	// 콘텐츠 유형 (MARKDOWN / HTML / TEXT, 기본값: MARKDOWN)
	private String contentType;

	// 카테고리 ID (선택)
	private String categoryId;

	// 부모 게시글 ID (답글인 경우)
	private String parentId;

	// 게시글 슬러그 (선택, URL 경로용)
	@Size(max = 100, message = "슬러그는 100자 이내여야 합니다")
	@Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "슬러그는 영소문자, 숫자, 하이픈만 사용 가능합니다")
	private String slug;

	// 비밀글 여부 (선택)
	private Boolean isSecret;

	// 임시 저장 여부 (선택)
	private Boolean isDraft;

	// 태그 이름 목록 (선택)
	private List<String> tagNames;

	// SEO 메타 제목 (선택)
	@Size(max = 200, message = "메타 제목은 200자 이내여야 합니다")
	private String metaTitle;

	// SEO 메타 설명 (선택)
	@Size(max = 500, message = "메타 설명은 500자 이내여야 합니다")
	private String metaDescription;
}
