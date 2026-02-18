package com.gizzi.core.domain.auth.service.oauth2;

import com.gizzi.core.domain.auth.dto.OAuth2UserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

// 카카오 OAuth2 사용자 정보 파서
// 카카오 사용자 정보 API 응답 구조:
// { id, kakao_account: { email, profile: { nickname, profile_image_url } } }
@Component
public class KakaoUserInfoExtractor implements OAuth2UserInfoExtractor {

	// Provider 코드 반환
	@Override
	public String getProviderCode() {
		return "kakao";
	}

	// 카카오 응답 → 공통 OAuth2UserInfo 변환
	@SuppressWarnings("unchecked")
	@Override
	public OAuth2UserInfo extract(Map<String, Object> attributes) {
		// 카카오 사용자 고유 ID (Long → String)
		String id = String.valueOf(attributes.get("id"));

		// kakao_account 객체에서 이메일, 프로필 정보 추출
		Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
		String email    = null;
		String nickname = null;
		String profileImage = null;

		if (kakaoAccount != null) {
			// 이메일 추출
			email = getString(kakaoAccount, "email");

			// profile 객체에서 닉네임, 프로필 이미지 추출
			Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
			if (profile != null) {
				nickname     = getString(profile, "nickname");
				profileImage = getString(profile, "profile_image_url");
			}
		}

		return OAuth2UserInfo.builder()
			.providerCode("kakao")
			.providerSubject(id)
			.email(email)
			.name(nickname)
			.profileImageUrl(profileImage)
			.build();
	}
}
