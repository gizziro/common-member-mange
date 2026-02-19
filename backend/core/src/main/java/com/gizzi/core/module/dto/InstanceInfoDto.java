package com.gizzi.core.module.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

// 모듈 인스턴스 정보 응답 DTO
// Resolve API에서 인스턴스 상세 정보를 전달할 때 사용한다
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceInfoDto
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String  instanceId;    // 인스턴스 PK (UUID)
	private final String  name;          // 인스턴스 표시명 (예: "공지사항")
	private final String  slug;          // URL 슬러그 (예: "notice")
	private final String  description;   // 인스턴스 설명
	private final Boolean enabled;       // 활성화 여부
}
