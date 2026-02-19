package com.gizzi.core.domain.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// 메뉴 수정 요청 DTO
@Getter
public class UpdateMenuRequestDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필수 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	@NotBlank(message = "메뉴 이름은 필수입니다")
	@Size(max = 100, message = "메뉴 이름은 100자 이내여야 합니다")
	private String name;			// 메뉴 표시명

	@NotBlank(message = "메뉴 유형은 필수입니다")
	private String menuType;		// 메뉴 유형 (MODULE / LINK / SEPARATOR)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 선택 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Size(max = 50, message = "아이콘 식별자는 50자 이내여야 합니다")
	private String icon;			// 아이콘 식별자

	private String moduleInstanceId;	// 모듈 인스턴스 ID (MODULE 타입 필수)

	@Size(max = 500, message = "URL은 500자 이내여야 합니다")
	private String customUrl;		// 커스텀 URL (LINK 타입 사용)

	@Size(max = 100, message = "단축 경로는 100자 이내여야 합니다")
	private String aliasPath;		// 외부 단축 경로 (예: "test", "free")

	@Size(max = 200, message = "콘텐츠 경로는 200자 이내여야 합니다")
	private String contentPath;		// SINGLE 모듈 콘텐츠 경로 (예: "test" → /page/test)
}
