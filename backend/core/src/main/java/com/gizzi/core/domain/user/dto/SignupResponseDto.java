package com.gizzi.core.domain.user.dto;

import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 회원가입 응답 DTO
// 가입 완료된 사용자의 기본 정보를 반환한다
@Getter
@Builder
public class SignupResponseDto
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

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

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// UserEntity 엔티티로부터 응답 DTO를 생성하는 정적 팩토리 메서드
	public static SignupResponseDto from(UserEntity user)
	{
		return SignupResponseDto.builder()
			.id(user.getId())
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.userStatus(user.getUserStatus())
			.createdAt(user.getCreatedAt())
			.build();
	}
}
