package com.gizzi.core.domain.group.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 그룹 멤버 복합 키 클래스 (group_id + user_id)
// @IdClass 방식으로 복합 PK를 정의한다
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupMemberId implements Serializable
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 복합 키 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private String groupId;		// 그룹 PK
	private String userId;		// 사용자 PK
}
