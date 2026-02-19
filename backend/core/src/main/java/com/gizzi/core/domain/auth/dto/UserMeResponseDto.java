package com.gizzi.core.domain.auth.dto;

import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

//----------------------------------------------------------------------------------------------------------------------
// /auth/me 응답 DTO (현재 로그인 사용자 정보)
//----------------------------------------------------------------------------------------------------------------------
@Getter
@Builder
public class UserMeResponseDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String  id;			// 사용자 PK (UUID)
	private final String  userId;		// 로그인 ID
	private final String  username;		// 사용자 이름
	private final String  email;		// 이메일
	private final String  userStatus;	// 사용자 상태 (PENDING, ACTIVE, SUSPENDED)
	private final String  provider;		// 가입/로그인 제공자 (LOCAL, GOOGLE, KAKAO, NAVER)
	private final String  phone;		// 전화번호
	private final Boolean isSmsAgree;	// SMS 수신 동의 여부

	//----------------------------------------------------------------------------------------------------------------------
	// UserEntity 엔티티로부터 응답 DTO 생성
	//----------------------------------------------------------------------------------------------------------------------
	public static UserMeResponseDto from(UserEntity user)
	{
		return UserMeResponseDto.builder()
			.id(user.getId())
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.userStatus(user.getUserStatus())
			.provider(user.getProvider())
			.phone(user.getPhone())
			.isSmsAgree(user.getIsSmsAgree())
			.build();
	}
}
