package com.gizzi.core.module.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 사용자 모듈 권한 복합 키 클래스
// (user_id + module_instance_id + module_permission_id) 3중 복합 PK
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserModulePermissionId implements Serializable
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 복합 키 구성 요소 ]
	//----------------------------------------------------------------------------------------------------------------------

	private String userId;               // 사용자 PK
	private String moduleInstanceId;     // 모듈 인스턴스 PK
	private String modulePermissionId;   // 모듈 권한 PK
}
