package com.gizzi.core.domain.menu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

// 메뉴 응답 DTO (트리 구조)
// children 필드로 재귀적 트리 구조를 표현한다
// MODULE 타입은 url이 slug에서 자동 생성, permissions에 리소스별 액션 포함
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponseDto {

	// 메뉴 PK
	private final String id;

	// 메뉴 표시명
	private final String name;

	// 아이콘 식별자
	private final String icon;

	// 메뉴 유형 (MODULE / LINK / SEPARATOR)
	private final String menuType;

	// URL (MODULE: slug 조합, LINK: customUrl, SEPARATOR: null)
	private final String url;

	// 모듈 인스턴스 ID (MODULE 타입에서만, 관리자 편집용)
	private final String moduleInstanceId;

	// 커스텀 URL (LINK 타입에서만, 관리자 편집용)
	private final String customUrl;

	// LINK 타입 가시성 제어용 역할
	private final String requiredRole;

	// 정렬 순서
	private final Integer sortOrder;

	// 가시성 여부
	private final Boolean isVisible;

	// MODULE 타입: 리소스별 허용된 액션 목록
	// 예: {"page": ["read", "write"]}
	private final Map<String, List<String>> permissions;

	// 하위 메뉴 (트리 구조)
	private final List<MenuResponseDto> children;
}
