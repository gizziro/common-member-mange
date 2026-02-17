package com.gizzi.core.module.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 모듈 내 리소스별 권한 액션 정의
// ModuleDefinition.getPermissions()에서 리소스-액션 쌍을 정의할 때 사용한다
// 예: resource="post", action="write", name="게시글 작성"
@Getter
@AllArgsConstructor
public class ResourcePermissionDefinition {

	// 리소스 코드 (예: "post", "comment", "category")
	private final String resource;

	// 액션 코드 (예: "read", "write", "delete")
	private final String action;

	// 권한 표시명 (예: "게시글 작성", "댓글 삭제")
	private final String name;
}
