package com.gizzi.core.domain.auth.dto;

import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

// 로그인 성공 응답 DTO
@Getter
@Builder
public class LoginResponseDto {

	// JWT Access Token
	private final String accessToken;

	// JWT Refresh Token
	private final String refreshToken;

	// 로그인 ID
	private final String userId;

	// 사용자 이름
	private final String username;

	// 이메일
	private final String email;

	// UserEntity + 토큰으로 응답 DTO 생성
	public static LoginResponseDto of(UserEntity user, String accessToken, String refreshToken) {
		return LoginResponseDto.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.build();
	}
}
