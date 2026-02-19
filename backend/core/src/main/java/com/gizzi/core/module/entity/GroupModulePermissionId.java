package com.gizzi.core.module.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 그룹 모듈 권한 복합 키 클래스
// (group_id + module_instance_id + module_permission_id) 3중 복합 PK
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupModulePermissionId implements Serializable
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 복합 키 구성 요소 ]
	//----------------------------------------------------------------------------------------------------------------------

	private String groupId;              // 그룹 PK
	private String moduleInstanceId;     // 모듈 인스턴스 PK
	private String modulePermissionId;   // 모듈 권한 PK
}
