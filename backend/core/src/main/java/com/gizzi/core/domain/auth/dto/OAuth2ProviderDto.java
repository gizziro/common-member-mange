package com.gizzi.core.domain.auth.dto;

import com.gizzi.core.domain.auth.entity.AuthProviderEntity;
import lombok.Builder;
import lombok.Getter;

//----------------------------------------------------------------------------------------------------------------------
// 활성 소셜 Provider 정보 DTO (사용자 로그인 페이지용)
// Client ID/Secret 등 민감 정보를 제외한 최소 정보만 포함
//----------------------------------------------------------------------------------------------------------------------
@Getter
@Builder
public class OAuth2ProviderDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String code;		// 제공자 코드 (google, kakao, naver)
	private final String name;		// 제공자 표시명
	private final String iconUrl;	// 아이콘 URL

	//----------------------------------------------------------------------------------------------------------------------
	// 엔티티 → 공개 DTO 변환
	//----------------------------------------------------------------------------------------------------------------------
	public static OAuth2ProviderDto from(AuthProviderEntity entity)
	{
		return OAuth2ProviderDto.builder()
			.code(entity.getCode())
			.name(entity.getName())
			.iconUrl(entity.getIconUrl())
			.build();
	}
}
