package com.gizzi.core.module.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 모듈 권한 정의 엔티티 (tb_module_permissions 테이블 매핑)
// 3단계 권한 모델: 모듈(module_code) → 리소스(resource) → 액션(action)
// 런타임 권한 문자열: UPPER(module_code) + "_" + UPPER(resource) + "_" + UPPER(action)
// 예: module_code=board, resource=post, action=write → "BOARD_POST_WRITE"
@Entity
@Table(name = "tb_module_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModulePermissionEntity
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ PK ]
	//----------------------------------------------------------------------------------------------------------------------

	// 모듈 권한 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 3단계 권한 정의 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 모듈 코드 FK (tb_modules.code 참조)
	@Column(name = "module_code", nullable = false, length = 50)
	private String moduleCode;

	// 모듈 내 리소스 (예: "post", "comment", "category")
	@Column(name = "resource", nullable = false, length = 50)
	private String resource;

	// 권한 액션 (예: "read", "write", "delete")
	@Column(name = "action", nullable = false, length = 30)
	private String action;

	// 권한 표시명 (예: "게시글 작성")
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 감사 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 생성 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 생명주기 콜백 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 엔티티 저장 전 UUID PK + 생성 일시 자동 생성
	@PrePersist
	private void prePersist()
	{
		if (this.id == null)
		{
			this.id = UUID.randomUUID().toString();
		}
		if (this.createdAt == null)
		{
			this.createdAt = LocalDateTime.now();
		}
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 모듈 권한 생성 팩토리 메서드 (ModuleRegistry에서 사용)
	public static ModulePermissionEntity create(String moduleCode, String resource,
	                                            String action, String name)
	{
		ModulePermissionEntity entity = new ModulePermissionEntity();
		entity.moduleCode = moduleCode;
		entity.resource   = resource;
		entity.action     = action;
		entity.name       = name;
		return entity;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 유틸리티 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 플랫 문자열 권한 코드 생성 (런타임 비교용)
	// 예: "BOARD_POST_WRITE"
	public String toFlatPermissionString()
	{
		return (moduleCode + "_" + resource + "_" + action).toUpperCase();
	}
}
