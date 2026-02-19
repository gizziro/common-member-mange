package com.gizzi.core.domain.auth.service.oauth2;

import com.gizzi.core.domain.auth.dto.OAuth2UserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

//----------------------------------------------------------------------------------------------------------------------
// 네이버 OAuth2 사용자 정보 파서
// 네이버 사용자 정보 API 응답 구조:
// { resultcode, message, response: { id, email, name, profile_image } }
//----------------------------------------------------------------------------------------------------------------------
@Component
public class NaverUserInfoExtractor implements OAuth2UserInfoExtractor
{
	//----------------------------------------------------------------------------------------------------------------------
	// Provider 코드 반환
	//----------------------------------------------------------------------------------------------------------------------
	@Override
	public String getProviderCode()
	{
		return "naver";
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 네이버 응답 → 공통 OAuth2UserInfo 변환
	//----------------------------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Override
	public OAuth2UserInfo extract(Map<String, Object> attributes)
	{
		// 네이버는 "response" 객체 안에 실제 사용자 정보가 있다
		Map<String, Object> response = (Map<String, Object>) attributes.get("response");

		// response가 null인 경우 빈 정보 반환
		if (response == null)
		{
			return OAuth2UserInfo.builder()
				.providerCode("naver")
				.build();
		}

		return OAuth2UserInfo.builder()
			.providerCode("naver")
			// 네이버 사용자 고유 ID
			.providerSubject(getString(response, "id"))
			// 이메일 주소
			.email(getString(response, "email"))
			// 사용자 이름
			.name(getString(response, "name"))
			// 프로필 이미지 URL
			.profileImageUrl(getString(response, "profile_image"))
			.build();
	}
}
