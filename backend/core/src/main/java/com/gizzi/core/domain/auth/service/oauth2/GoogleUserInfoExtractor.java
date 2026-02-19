package com.gizzi.core.domain.auth.service.oauth2;

import com.gizzi.core.domain.auth.dto.OAuth2UserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

//----------------------------------------------------------------------------------------------------------------------
// Google OAuth2 사용자 정보 파서
// Google userinfo API 응답: { sub, email, name, picture, ... }
//----------------------------------------------------------------------------------------------------------------------
@Component
public class GoogleUserInfoExtractor implements OAuth2UserInfoExtractor
{
	//----------------------------------------------------------------------------------------------------------------------
	// Provider 코드 반환
	//----------------------------------------------------------------------------------------------------------------------
	@Override
	public String getProviderCode()
	{
		return "google";
	}

	//----------------------------------------------------------------------------------------------------------------------
	// Google 응답 → 공통 OAuth2UserInfo 변환
	//----------------------------------------------------------------------------------------------------------------------
	@Override
	public OAuth2UserInfo extract(Map<String, Object> attributes)
	{
		return OAuth2UserInfo.builder()
			.providerCode("google")
			// Google의 사용자 고유 식별자
			.providerSubject(getString(attributes, "sub"))
			// 이메일 주소
			.email(getString(attributes, "email"))
			// 사용자 표시 이름
			.name(getString(attributes, "name"))
			// 프로필 이미지 URL
			.profileImageUrl(getString(attributes, "picture"))
			.build();
	}
}
