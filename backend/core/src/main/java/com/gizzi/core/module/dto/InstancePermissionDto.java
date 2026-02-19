package com.gizzi.core.module.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 인스턴스에 대한 그룹별 권한 현황 응답 DTO
// 메뉴 관리에서 그룹별 권한 체크박스 테이블을 구성할 때 사용한다
@Getter
@Builder
public class InstancePermissionDto
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String       groupId;                // 그룹 PK
	private final String       groupName;              // 그룹 표시명
	private final String       groupCode;              // 그룹 코드
	private final List<String> grantedPermissionIds;   // 부여된 권한 ID 목록
}
