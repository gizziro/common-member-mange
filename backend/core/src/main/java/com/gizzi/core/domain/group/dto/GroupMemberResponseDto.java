package com.gizzi.core.domain.group.dto;

import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 그룹 멤버 응답 DTO
@Getter
@Builder
public class GroupMemberResponseDto {

	// 사용자 PK
	private final String        userId;

	// 사용자 이름
	private final String        username;

	// 이메일 주소
	private final String        email;

	// 그룹 가입 일시
	private final LocalDateTime joinedAt;

	// 사용자 엔티티 + 가입 일시로 응답 DTO 생성
	public static GroupMemberResponseDto from(UserEntity user, LocalDateTime joinedAt) {
		return GroupMemberResponseDto.builder()
			.userId(user.getId())
			.username(user.getUsername())
			.email(user.getEmail())
			.joinedAt(joinedAt)
			.build();
	}
}
