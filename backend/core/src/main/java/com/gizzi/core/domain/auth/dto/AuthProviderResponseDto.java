package com.gizzi.core.domain.auth.dto;

import com.gizzi.core.domain.auth.entity.AuthProviderEntity;
import lombok.Builder;
import lombok.Getter;

//----------------------------------------------------------------------------------------------------------------------
// 인증 제공자 응답 DTO (관리자용 — 전체 정보 포함)
//----------------------------------------------------------------------------------------------------------------------
@Getter
@Builder
public class AuthProviderResponseDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String  id;				// 제공자 PK
	private final String  code;				// 제공자 코드
	private final String  name;				// 제공자 표시명
	private final Boolean isEnabled;		// 활성화 여부
	private final String  clientId;			// OAuth2 Client ID
	private final String  clientSecret;		// OAuth2 Client Secret (마스킹 처리)
	private final String  redirectUri;		// 인가 코드 콜백 URL
	private final String  authorizationUri;	// 인가 엔드포인트 URL
	private final String  tokenUri;			// 토큰 교환 엔드포인트 URL
	private final String  userinfoUri;		// 사용자 정보 조회 엔드포인트 URL
	private final String  scope;			// 요청 Scope
	private final String  iconUrl;			// 아이콘 URL
	private final Integer displayOrder;		// 표시 순서

	//----------------------------------------------------------------------------------------------------------------------
	// 엔티티 → 응답 DTO 변환
	//----------------------------------------------------------------------------------------------------------------------
	public static AuthProviderResponseDto from(AuthProviderEntity entity)
	{
		return AuthProviderResponseDto.builder()
			.id(entity.getId())
			.code(entity.getCode())
			.name(entity.getName())
			.isEnabled(entity.getIsEnabled())
			.clientId(entity.getClientId())
			// Client Secret은 앞 4자만 표시하고 나머지 마스킹
			.clientSecret(maskSecret(entity.getClientSecret()))
			.redirectUri(entity.getRedirectUri())
			.authorizationUri(entity.getAuthorizationUri())
			.tokenUri(entity.getTokenUri())
			.userinfoUri(entity.getUserinfoUri())
			.scope(entity.getScope())
			.iconUrl(entity.getIconUrl())
			.displayOrder(entity.getDisplayOrder())
			.build();
	}

	//----------------------------------------------------------------------------------------------------------------------
	// Client Secret 마스킹 (앞 4자만 보이고 나머지 ****)
	//----------------------------------------------------------------------------------------------------------------------
	private static String maskSecret(String secret)
	{
		// null 또는 빈 문자열이면 그대로 반환
		if (secret == null || secret.isEmpty())
		{
			return secret;
		}
		// 4자 이하이면 전체 마스킹
		if (secret.length() <= 4)
		{
			return "****";
		}
		// 앞 4자만 표시
		return secret.substring(0, 4) + "****";
	}
}
