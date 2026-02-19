package com.gizzi.core.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

//----------------------------------------------------------------------------------------------------------------------
// OAuth2 로그인 결과 DTO
// type에 따라 즉시 로그인 또는 연동 대기(기존 계정 발견) 상태를 나타낸다
//----------------------------------------------------------------------------------------------------------------------
@Getter
@Builder
public class OAuth2LoginResultDto
{
	// [ 공통 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String type;				// 결과 타입 (SUCCESS: 바로 로그인 / LINK_PENDING: 연동 대기)

	// [ SUCCESS 시에만 사용 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String accessToken;		// JWT Access Token
	private final String refreshToken;		// JWT Refresh Token
	private final String username;			// 사용자 이름

	// [ LINK_PENDING 시에만 사용 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String pendingId;			// 연동 대기 식별자 (Redis 키에 사용)
	private final String email;				// 기존 계정의 이메일
	private final String providerCode;		// 소셜 제공자 코드
	private final String providerName;		// 소셜 제공자 표시명

	//----------------------------------------------------------------------------------------------------------------------
	// SUCCESS 결과 팩토리
	//----------------------------------------------------------------------------------------------------------------------
	public static OAuth2LoginResultDto success(LoginResponseDto loginResponse)
	{
		return OAuth2LoginResultDto.builder()
			.type("SUCCESS")
			.accessToken(loginResponse.getAccessToken())
			.refreshToken(loginResponse.getRefreshToken())
			.username(loginResponse.getUsername())
			.build();
	}

	//----------------------------------------------------------------------------------------------------------------------
	// LINK_PENDING 결과 팩토리
	//----------------------------------------------------------------------------------------------------------------------
	public static OAuth2LoginResultDto linkPending(String pendingId, String email,
	                                               String providerCode, String providerName)
	{
		return OAuth2LoginResultDto.builder()
			.type("LINK_PENDING")
			.pendingId(pendingId)
			.email(email)
			.providerCode(providerCode)
			.providerName(providerName)
			.build();
	}
}
