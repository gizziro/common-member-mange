package com.gizzi.core.domain.auth.service.oauth2;

import com.gizzi.core.domain.auth.dto.OAuth2UserInfo;

import java.util.Map;

//----------------------------------------------------------------------------------------------------------------------
// OAuth2 사용자 정보 파싱 인터페이스
// 각 소셜 Provider별로 응답 구조가 다르므로 구현체에서 파싱 로직을 정의한다
//----------------------------------------------------------------------------------------------------------------------
public interface OAuth2UserInfoExtractor
{
	// 지원하는 Provider 코드 반환 (google, kakao, naver)
	String getProviderCode();

	// Provider 응답 Map → 공통 OAuth2UserInfo로 변환
	OAuth2UserInfo extract(Map<String, Object> attributes);

	// Map에서 String 값 안전 추출 (공통 유틸리티)
	default String getString(Map<String, Object> map, String key)
	{
		// 키가 존재하면 String으로 변환, 없으면 null 반환
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}
}
