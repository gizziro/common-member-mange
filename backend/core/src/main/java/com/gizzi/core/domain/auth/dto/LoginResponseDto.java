package com.gizzi.core.domain.auth.dto;

import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

//----------------------------------------------------------------------------------------------------------------------
// 로그인 성공 응답 DTO
// OTP 필요 시 requireOtp=true, otpSessionId 포함 (토큰 미발급)
//----------------------------------------------------------------------------------------------------------------------
@Getter
@Builder
public class LoginResponseDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String  accessToken;		// JWT Access Token (OTP 필요 시 null)
	private final String  refreshToken;		// JWT Refresh Token (OTP 필요 시 null)
	private final String  userId;			// 로그인 ID
	private final String  username;			// 사용자 이름
	private final String  email;			// 이메일
	private final Boolean requireOtp;		// OTP 인증 필요 여부 (2FA 활성화 시 true)
	private final String  otpSessionId;		// OTP 세션 ID (OTP 필요 시에만 포함)

	//----------------------------------------------------------------------------------------------------------------------
	// UserEntity + 토큰으로 응답 DTO 생성 (일반 로그인 성공)
	//----------------------------------------------------------------------------------------------------------------------
	public static LoginResponseDto of(UserEntity user, String accessToken, String refreshToken)
	{
		return LoginResponseDto.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.build();
	}

	//----------------------------------------------------------------------------------------------------------------------
	// OTP 필요 응답 생성 (JWT 미발급, 세션 ID만 반환)
	//----------------------------------------------------------------------------------------------------------------------
	public static LoginResponseDto requireOtp(UserEntity user, String otpSessionId)
	{
		return LoginResponseDto.builder()
			.userId(user.getUserId())
			.username(user.getUsername())
			.email(user.getEmail())
			.requireOtp(true)
			.otpSessionId(otpSessionId)
			.build();
	}
}
