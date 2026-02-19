package com.gizzi.core.module.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 인스턴스에 대한 사용자별 권한 현황 응답 DTO
// 페이지 권한 설정에서 개인 사용자 권한 체크박스 테이블을 구성할 때 사용한다
@Getter
@Builder
public class UserInstancePermissionDto
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String       userId;                 // 사용자 PK
	private final String       loginId;                // 로그인 ID
	private final String       username;               // 사용자 이름
	private final List<String> grantedPermissionIds;   // 부여된 권한 ID 목록
}
