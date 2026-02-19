package com.gizzi.core.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

// 그룹별 SMS 수신 가능 회원 응답 DTO
// 각 그룹의 ID, 이름, 코드와 해당 그룹 내 수신 가능 회원 수를 포함한다
@Getter
@Builder
public class GroupRecipientsDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String groupId;			// 그룹 ID
	private final String groupName;			// 그룹 이름
	private final String groupCode;			// 그룹 코드
	private final long   recipientCount;	// 해당 그룹 내 수신 가능 회원 수
}
