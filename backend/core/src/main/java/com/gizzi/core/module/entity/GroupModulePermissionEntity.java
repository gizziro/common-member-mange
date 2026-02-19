package com.gizzi.core.module.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 그룹 모듈 권한 엔티티 (tb_group_module_permissions 테이블 매핑)
// 특정 그룹에 특정 모듈 인스턴스에 대한 권한을 부여한다
// 그룹에 소속된 모든 사용자가 이 권한을 상속받으며,
// 사용자 직접 권한(UserModulePermissionEntity)과 합산(additive)된다
@Entity
@Table(name = "tb_group_module_permissions")
@IdClass(GroupModulePermissionId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupModulePermissionEntity
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 복합 PK ]
	//----------------------------------------------------------------------------------------------------------------------

	// 그룹 PK
	@Id
	@Column(name = "group_id", length = 36)
	private String groupId;

	// 모듈 인스턴스 PK
	@Id
	@Column(name = "module_instance_id", length = 50)
	private String moduleInstanceId;

	// 모듈 권한 PK
	@Id
	@Column(name = "module_permission_id", length = 36)
	private String modulePermissionId;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 감사 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 권한 부여 일시
	@Column(name = "granted_at", nullable = false)
	private LocalDateTime grantedAt;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 그룹 모듈 권한 생성 팩토리 메서드
	public static GroupModulePermissionEntity create(String groupId, String moduleInstanceId,
	                                                 String modulePermissionId)
	{
		GroupModulePermissionEntity entity = new GroupModulePermissionEntity();
		entity.groupId            = groupId;
		entity.moduleInstanceId   = moduleInstanceId;
		entity.modulePermissionId = modulePermissionId;
		entity.grantedAt          = LocalDateTime.now();
		return entity;
	}
}
