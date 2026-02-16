package com.gizzi.core.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

// OAuth2 소셜 로그인 사용자 정보 통합 DTO
// 각 Provider(Google, Kakao, Naver)별 응답을 이 공통 형태로 변환한다
@Getter
@Builder
public class OAuth2UserInfo {

	// 소셜 제공자 코드 (google, kakao, naver)
	private final String providerCode;

	// 소셜 제공자 사용자 고유 키 (sub, id 등)
	private final String providerSubject;

	// 이메일 주소
	private final String email;

	// 사용자 이름 (닉네임)
	private final String name;

	// 프로필 이미지 URL
	private final String profileImageUrl;
}
