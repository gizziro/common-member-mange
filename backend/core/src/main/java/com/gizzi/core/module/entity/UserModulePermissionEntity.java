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

// 사용자 직접 모듈 권한 엔티티 (tb_user_module_permissions 테이블 매핑)
// 특정 사용자에게 특정 모듈 인스턴스에 대한 권한을 직접 부여한다
// 그룹 권한(GroupModulePermissionEntity)과 합산(additive)되어 최종 권한을 결정한다
@Entity
@Table(name = "tb_user_module_permissions")
@IdClass(UserModulePermissionId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserModulePermissionEntity {

	// 사용자 PK
	@Id
	@Column(name = "user_id", length = 36)
	private String userId;

	// 모듈 인스턴스 PK
	@Id
	@Column(name = "module_instance_id", length = 50)
	private String moduleInstanceId;

	// 모듈 권한 PK
	@Id
	@Column(name = "module_permission_id", length = 36)
	private String modulePermissionId;

	// 권한 부여 일시
	@Column(name = "granted_at", nullable = false)
	private LocalDateTime grantedAt;

	// 사용자 모듈 권한 생성 팩토리 메서드
	public static UserModulePermissionEntity create(String userId, String moduleInstanceId,
	                                                String modulePermissionId) {
		UserModulePermissionEntity entity = new UserModulePermissionEntity();
		entity.userId             = userId;
		entity.moduleInstanceId   = moduleInstanceId;
		entity.modulePermissionId = modulePermissionId;
		entity.grantedAt          = LocalDateTime.now();
		return entity;
	}
}
