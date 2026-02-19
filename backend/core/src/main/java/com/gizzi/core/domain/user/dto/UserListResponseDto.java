package com.gizzi.core.domain.user.dto;

import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 사용자 목록 조회 응답 DTO (축약 필드)
// 관리자 회원 목록 테이블에서 사용
@Getter
@Builder
public class UserListResponseDto
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String        id;              // 사용자 PK (UUID)
	private final String        userId;          // 로그인 ID
	private final String        username;        // 사용자 이름
	private final String        email;           // 이메일 주소
	private final String        provider;        // 가입/로그인 제공자 (LOCAL, GOOGLE 등)
	private final String        userStatus;      // 사용자 상태 (PENDING, ACTIVE, SUSPENDED)
	private final Boolean       isLocked;        // 계정 잠금 여부
	private final LocalDateTime createdAt;       // 생성 일시

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// UserEntity로부터 목록용 DTO를 생성하는 정적 팩토리 메서드
	public static UserListResponseDto from(UserEntity user)
	{
		return UserListResponseDto.builder()
			.id(user.getId())
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.provider(user.getProvider())
			.userStatus(user.getUserStatus())
			.isLocked(user.getIsLocked())
			.createdAt(user.getCreatedAt())
			.build();
	}
}
