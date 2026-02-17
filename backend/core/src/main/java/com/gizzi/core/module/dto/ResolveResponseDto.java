package com.gizzi.core.module.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

// Slug 기반 동적 라우팅 Resolve API 응답 DTO
// GET /resolve/{module-slug}/{instance-slug} 요청 시
// 모듈 정보 + 인스턴스 정보 + 사용자 권한 맵을 반환한다
//
// 프론트엔드에서 permissions.post.contains("write") 체크로 UI 동적 제어
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResolveResponseDto {

	// 모듈 기본 정보
	private final ModuleInfoDto module;

	// 인스턴스 정보 (SINGLE 모듈은 null)
	private final InstanceInfoDto instance;

	// 리소스별 허용된 액션 목록
	// 예: {"post": ["read", "write"], "comment": ["read"]}
	private final Map<String, List<String>> permissions;
}
