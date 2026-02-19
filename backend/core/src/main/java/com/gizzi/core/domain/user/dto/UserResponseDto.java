package com.gizzi.core.domain.user.dto;

import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 사용자 상세 조회 응답 DTO
// 관리자가 사용자 전체 정보를 확인할 때 사용
@Getter
@Builder
public class UserResponseDto
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String        id;              // 사용자 PK (UUID)
	private final String        userId;          // 로그인 ID
	private final String        username;        // 사용자 이름
	private final String        email;           // 이메일 주소
	private final String        provider;        // 가입/로그인 제공자 (LOCAL, GOOGLE 등)
	private final Boolean       emailVerified;   // 이메일 인증 여부
	private final String        phone;           // 전화번호
	private final Boolean       phoneVerified;   // 전화번호 인증 여부
	private final Boolean       isSmsAgree;      // SMS 수신 동의 여부
	private final Boolean       isOtpUse;        // OTP 사용 여부
	private final Integer       loginFailCount;  // 로그인 실패 횟수
	private final Boolean       isLocked;        // 계정 잠금 여부
	private final LocalDateTime lockedAt;        // 계정 잠금 일시
	private final String        userStatus;      // 사용자 상태 (PENDING, ACTIVE, SUSPENDED)
	private final LocalDateTime createdAt;       // 생성 일시
	private final LocalDateTime updatedAt;       // 수정 일시

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// UserEntity로부터 응답 DTO를 생성하는 정적 팩토리 메서드
	public static UserResponseDto from(UserEntity user)
	{
		return UserResponseDto.builder()
			.id(user.getId())
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.provider(user.getProvider())
			.emailVerified(user.getEmailVerified())
			.phone(user.getPhone())
			.phoneVerified(user.getPhoneVerified())
			.isSmsAgree(user.getIsSmsAgree())
			.isOtpUse(user.getIsOtpUse())
			.loginFailCount(user.getLoginFailCount())
			.isLocked(user.getIsLocked())
			.lockedAt(user.getLockedAt())
			.userStatus(user.getUserStatus())
			.createdAt(user.getCreatedAt())
			.updatedAt(user.getUpdatedAt())
			.build();
	}
}
