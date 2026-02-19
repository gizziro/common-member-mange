package com.gizzi.core.domain.menu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 메뉴 응답 DTO (트리 구조)
// children 필드로 재귀적 트리 구조를 표현한다
// MODULE 타입은 url이 slug에서 자동 생성, aliasPath로 단축 경로 제공
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponseDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 메뉴 기본 정보 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String	id;					// 메뉴 PK
	private final String	name;				// 메뉴 표시명
	private final String	icon;				// 아이콘 식별자
	private final String	menuType;			// 메뉴 유형 (MODULE / LINK / SEPARATOR)

	//----------------------------------------------------------------------------------------------------------------------
	// [ URL 및 경로 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String	url;				// URL (MODULE: slug 조합, LINK: customUrl, SEPARATOR: null)
	private final String	moduleInstanceId;	// 모듈 인스턴스 ID (MODULE 타입에서만, 관리자 편집용)
	private final String	customUrl;			// 커스텀 URL (LINK 타입에서만, 관리자 편집용)
	private final String	aliasPath;			// 외부 단축 경로 (예: "test", "free")
	private final String	contentPath;		// SINGLE 모듈 콘텐츠 경로 (예: "test" → /page/test)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 표시 및 트리 구조 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final Integer	sortOrder;			// 정렬 순서
	private final Boolean	isVisible;			// 가시성 여부

	private final List<MenuResponseDto> children;	// 하위 메뉴 (트리 구조)
}
