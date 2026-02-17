package com.gizzi.core.module.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 개별 권한 항목 DTO
// 권한 요약에서 부여된 권한의 상세 정보를 표시할 때 사용한다
@Getter
@Builder
@AllArgsConstructor
public class PermissionItemDto {

	// 리소스명 (예: "page", "admin")
	private final String resource;

	// 액션명 (예: "read", "write", "access")
	private final String action;

	// 권한 표시명 (예: "페이지 읽기", "관리 패널 조회")
	private final String name;
}
