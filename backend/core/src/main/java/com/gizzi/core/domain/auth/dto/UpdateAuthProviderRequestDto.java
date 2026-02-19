package com.gizzi.core.domain.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

//----------------------------------------------------------------------------------------------------------------------
// 인증 제공자 설정 수정 요청 DTO
//----------------------------------------------------------------------------------------------------------------------
@Getter
@NoArgsConstructor
public class UpdateAuthProviderRequestDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private String  clientId;		// OAuth2 Client ID
	private String  clientSecret;	// OAuth2 Client Secret
	private String  redirectUri;	// 인가 코드 콜백 URL
	private String  scope;			// 요청 Scope
	private String  iconUrl;		// 아이콘 URL

	@NotNull(message = "활성화 여부는 필수입니다")
	private Boolean isEnabled;		// 활성화 여부 (필수)
}
