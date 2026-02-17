package com.gizzi.core.domain.setting.entity;

// 설정 값의 데이터 타입
// 관리자 UI에서 적절한 입력 컴포넌트를 렌더링하는 데 사용
public enum SettingValueType {

	// 문자열 (기본 텍스트 입력)
	STRING,

	// 숫자 (숫자 입력)
	NUMBER,

	// 불리언 (토글 스위치)
	BOOLEAN,

	// JSON (텍스트 영역)
	JSON
}
