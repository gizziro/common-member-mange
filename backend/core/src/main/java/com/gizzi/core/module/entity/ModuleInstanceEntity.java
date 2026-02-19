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

// 모듈 인스턴스 엔티티 (tb_module_instances 테이블 매핑)
// MULTI 타입 모듈에서 생성되는 인스턴스를 관리한다
// 예: 게시판 모듈 → "공지사항" 인스턴스, "자유게시판" 인스턴스
@Entity
@Table(name = "tb_module_instances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModuleInstanceEntity
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ PK ]
	//----------------------------------------------------------------------------------------------------------------------

	// 인스턴스 PK (UUID)
	@Id
	@Column(name = "instance_id", length = 50)
	private String instanceId;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 메타데이터 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 모듈 코드 FK (tb_modules.code 참조)
	@Column(name = "module_code", nullable = false, length = 50)
	private String moduleCode;

	// 인스턴스 표시명 (예: "공지사항")
	@Column(name = "instance_name", nullable = false, length = 100)
	private String instanceName;

	// URL 슬러그 (동일 모듈 내 유니크, 예: "notice")
	@Column(name = "slug", nullable = false, length = 50)
	private String slug;

	// 인스턴스 설명
	@Column(name = "description", length = 500)
	private String description;

	// 소유자 사용자 PK
	@Column(name = "owner_id", nullable = false, length = 50)
	private String ownerId;

	// 인스턴스 유형
	@Column(name = "instance_type", nullable = false, length = 20)
	private String instanceType;

	// 활성화 여부
	@Column(name = "enabled", nullable = false)
	private Boolean enabled;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 감사 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 생성자
	@Column(name = "created_by", nullable = false, length = 100)
	private String createdBy;

	// 생성 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 수정자
	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	// 수정 일시
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 생명주기 콜백 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 엔티티 저장 전 UUID PK 자동 생성
	@PrePersist
	private void prePersist()
	{
		// ID가 없을 때만 새로 생성
		if (this.instanceId == null)
		{
			this.instanceId = UUID.randomUUID().toString();
		}
		// 생성 일시 설정
		if (this.createdAt == null)
		{
			this.createdAt = LocalDateTime.now();
		}
		// 수정 일시 초기값 설정
		this.updatedAt = LocalDateTime.now();
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 인스턴스 생성 팩토리 메서드
	public static ModuleInstanceEntity create(String moduleCode, String instanceName,
	                                          String slug, String description,
	                                          String ownerId, String instanceType,
	                                          String createdBy)
	{
		ModuleInstanceEntity entity = new ModuleInstanceEntity();
		entity.moduleCode   = moduleCode;
		entity.instanceName = instanceName;
		entity.slug         = slug;
		entity.description  = description;
		entity.ownerId      = ownerId;
		entity.instanceType = instanceType;
		entity.enabled      = true;
		entity.createdBy    = createdBy;
		return entity;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 비즈니스 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 인스턴스 정보 수정
	public void updateInfo(String instanceName, String slug, String description, String updatedBy)
	{
		this.instanceName = instanceName;
		this.slug         = slug;
		this.description  = description;
		this.updatedBy    = updatedBy;
		this.updatedAt    = LocalDateTime.now();
	}

	// 인스턴스 활성화/비활성화
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
}
