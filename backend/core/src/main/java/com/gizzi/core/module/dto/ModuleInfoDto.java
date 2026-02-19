package com.gizzi.core.module.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

// 모듈 기본 정보 응답 DTO
// Resolve API, 메뉴 조회 API 등에서 모듈 메타데이터를 전달할 때 사용한다
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModuleInfoDto
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String  code;          // 모듈 코드 (예: "board")
	private final String  name;          // 모듈 표시명 (예: "게시판")
	private final String  slug;          // URL 슬러그 (예: "board")
	private final String  description;   // 모듈 설명 (예: "게시글, 댓글 관리")
	private final String  type;          // 모듈 유형 (SINGLE / MULTI)
	private final Boolean enabled;       // 활성화 여부
}
