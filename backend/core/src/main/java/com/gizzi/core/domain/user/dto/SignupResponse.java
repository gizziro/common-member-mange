package com.gizzi.core.domain.user.dto;

import com.gizzi.core.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 회원가입 응답 DTO
@Getter
@Builder
public class SignupResponse {

	// 사용자 PK
	private final String id;

	// 로그인 ID
	private final String userId;

	// 사용자 이름
	private final String username;

	// 이메일 주소
	private final String email;

	// 사용자 상태 (PENDING 등)
	private final String userStatus;

	// 가입 일시
	private final LocalDateTime createdAt;

	// User 엔티티로부터 응답 DTO를 생성하는 정적 팩토리 메서드
	public static SignupResponse from(User user) {
		return SignupResponse.builder()
			.id(user.getId())
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.userStatus(user.getUserStatus())
			.createdAt(user.getCreatedAt())
			.build();
	}
}
