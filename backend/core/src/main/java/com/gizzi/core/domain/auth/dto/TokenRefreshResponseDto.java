package com.gizzi.core.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

//----------------------------------------------------------------------------------------------------------------------
// 토큰 갱신 성공 응답 DTO
//----------------------------------------------------------------------------------------------------------------------
@Getter
@Builder
public class TokenRefreshResponseDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String accessToken;	// 새로 발급된 Access Token
	private final String refreshToken;	// 새로 발급된 Refresh Token (Rotation 방식)
}
