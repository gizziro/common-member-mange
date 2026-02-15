package com.gizzi.core.domain.auth.dto;

import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

// /auth/me 응답 DTO (현재 로그인 사용자 정보)
@Getter
@Builder
public class UserMeResponseDto {

	// 사용자 PK (UUID)
	private final String id;

	// 로그인 ID
	private final String userId;

	// 사용자 이름
	private final String username;

	// 이메일
	private final String email;

	// 사용자 상태 (PENDING, ACTIVE, SUSPENDED)
	private final String userStatus;

	// UserEntity 엔티티로부터 응답 DTO 생성
	public static UserMeResponseDto from(UserEntity user) {
		return UserMeResponseDto.builder()
			.id(user.getId())
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.userStatus(user.getUserStatus())
			.build();
	}
}
