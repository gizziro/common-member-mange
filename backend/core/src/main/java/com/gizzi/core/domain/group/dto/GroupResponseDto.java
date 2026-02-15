package com.gizzi.core.domain.group.dto;

import com.gizzi.core.domain.group.entity.GroupEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 그룹 응답 DTO
@Getter
@Builder
public class GroupResponseDto {

	// 그룹 PK
	private final String        id;

	// 그룹 코드
	private final String        groupCode;

	// 그룹 표시명
	private final String        name;

	// 그룹 설명
	private final String        description;

	// 시스템 그룹 여부
	private final Boolean       isSystem;

	// 그룹 소유자 사용자 PK
	private final String        ownerUserId;

	// 그룹 소속 멤버 수
	private final long          memberCount;

	// 생성 일시
	private final LocalDateTime createdAt;

	// 엔티티 + 멤버 수로 응답 DTO 생성
	public static GroupResponseDto from(GroupEntity entity, long memberCount) {
		return GroupResponseDto.builder()
			.id(entity.getId())
			.groupCode(entity.getGroupCode())
			.name(entity.getName())
			.description(entity.getDescription())
			.isSystem(entity.getIsSystem())
			.ownerUserId(entity.getOwnerUserId())
			.memberCount(memberCount)
			.createdAt(entity.getCreatedAt())
			.build();
	}
}
