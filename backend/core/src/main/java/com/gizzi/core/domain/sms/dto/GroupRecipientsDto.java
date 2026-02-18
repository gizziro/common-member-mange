package com.gizzi.core.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

// 그룹별 SMS 수신 가능 회원 응답 DTO
@Getter
@Builder
public class GroupRecipientsDto {

	// 그룹 ID
	private final String groupId;

	// 그룹 이름
	private final String groupName;

	// 그룹 코드
	private final String groupCode;

	// 해당 그룹 내 수신 가능 회원 수
	private final long   recipientCount;
}
