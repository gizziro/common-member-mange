package com.gizzi.core.domain.auth.service.oauth2;

import com.gizzi.core.domain.auth.dto.OAuth2UserInfo;

import java.util.Map;

// OAuth2 사용자 정보 파싱 인터페이스
// 각 소셜 Provider별로 응답 구조가 다르므로 구현체에서 파싱 로직을 정의한다
public interface OAuth2UserInfoExtractor {

	// 지원하는 Provider 코드 반환 (google, kakao, naver)
	String getProviderCode();

	// Provider 응답 Map → 공통 OAuth2UserInfo로 변환
	OAuth2UserInfo extract(Map<String, Object> attributes);
}
