package com.gizzi.core.module.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 모듈 인스턴스별 권한 요약 DTO
// 사용자/그룹 상세 화면에서 부여된 권한을 인스턴스별로 집계하여 표시할 때 사용한다
@Getter
@Builder
public class PermissionSummaryDto {

	// 모듈 인스턴스 ID
	private final String instanceId;

	// 인스턴스 표시명 (예: "소개 페이지")
	private final String instanceName;

	// 인스턴스 slug
	private final String instanceSlug;

	// 모듈 코드 (예: "page")
	private final String moduleCode;

	// 모듈 표시명 (예: "페이지")
	private final String moduleName;

	// 권한 출처: "DIRECT" 또는 "GROUP:그룹명"
	private final String source;

	// 부여된 권한 목록
	private final List<PermissionItemDto> permissions;
}
