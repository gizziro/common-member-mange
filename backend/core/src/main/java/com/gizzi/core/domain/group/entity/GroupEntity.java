package com.gizzi.core.domain.group.entity;

import com.gizzi.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 그룹 엔티티 (tb_groups 테이블 매핑)
// 시스템 그룹(administrator, user)과 사용자 생성 그룹을 모두 관리한다
@Entity
@Table(name = "tb_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupEntity extends BaseEntity
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ PK ]
	//----------------------------------------------------------------------------------------------------------------------

	// 그룹 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 기본 정보 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 그룹 코드 (프로그래밍 식별자, 영소문자+숫자+하이픈)
	@Column(name = "group_code", nullable = false, length = 50)
	private String groupCode;

	// 그룹 표시명
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// 그룹 설명
	@Column(name = "description", length = 255)
	private String description;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 시스템/소유자 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 시스템 그룹 여부 (true: 삭제/코드변경 불가)
	@Column(name = "is_system", nullable = false)
	private Boolean isSystem;

	// 그룹 소유자 사용자 PK (시스템 그룹은 NULL)
	@Column(name = "owner_user_id", length = 36)
	private String ownerUserId;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 생명주기 콜백 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 엔티티 저장 전 UUID PK 자동 생성
	@PrePersist
	private void generateId()
	{
		// ID가 없을 때만 새로 생성 (수동 설정된 경우 유지)
		if (this.id == null)
		{
			this.id = UUID.randomUUID().toString();
		}
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 조회 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 시스템 그룹 여부 확인
	public boolean isSystemGroup()
	{
		return Boolean.TRUE.equals(this.isSystem);
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 시스템 그룹 생성 팩토리 메서드 (소유자 없음)
	public static GroupEntity createSystemGroup(String groupCode, String name, String description)
	{
		// 새 시스템 그룹 엔티티 생성
		GroupEntity group  = new GroupEntity();
		group.groupCode    = groupCode;
		group.name         = name;
		group.description  = description;
		group.isSystem     = true;
		group.ownerUserId  = null;
		return group;
	}

	// 사용자 그룹 생성 팩토리 메서드 (소유자 필수)
	public static GroupEntity createUserGroup(String groupCode, String name,
	                                          String description, String ownerUserId)
	{
		// 새 사용자 그룹 엔티티 생성
		GroupEntity group  = new GroupEntity();
		group.groupCode    = groupCode;
		group.name         = name;
		group.description  = description;
		group.isSystem     = false;
		group.ownerUserId  = ownerUserId;
		return group;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 비즈니스 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 그룹 정보 수정 (시스템 그룹의 코드는 외부에서 변경 불가 — 서비스에서 검증)
	public void updateInfo(String name, String description)
	{
		// 그룹 표시명 변경
		this.name = name;
		// 그룹 설명 변경
		this.description = description;
	}

	// 그룹 소유자 해제 (소유자 사용자 삭제 시 사용)
	public void clearOwner()
	{
		// 소유자를 null로 설정
		this.ownerUserId = null;
	}
}
