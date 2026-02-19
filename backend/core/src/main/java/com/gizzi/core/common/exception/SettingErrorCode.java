package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 설정 관련 에러 코드 (SETTING_*)
// 설정 CRUD에서 발생하는 비즈니스 에러
@Getter
@AllArgsConstructor
public enum SettingErrorCode implements ErrorCode
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 설정 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	SETTING_NOT_FOUND     ("SETTING_NOT_FOUND",      "설정을 찾을 수 없습니다",             "설정 키 조회 실패",        HttpStatus.NOT_FOUND),		// 설정 조회 실패
	SETTING_READONLY      ("SETTING_READONLY",       "읽기 전용 설정은 변경할 수 없습니다",   "읽기 전용 설정 수정 시도",   HttpStatus.BAD_REQUEST),	// 읽기 전용 설정 수정 불가
	SETTING_INVALID_VALUE ("SETTING_INVALID_VALUE",  "유효하지 않은 설정 값입니다",          "설정 값 타입 불일치",       HttpStatus.BAD_REQUEST);	// 설정 값 형식 오류

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String     code;			// 에러 코드 문자열 (예: "SETTING_NOT_FOUND")
	private final String     message;		// 사용자에게 표시할 에러 메시지
	private final String     description;	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final HttpStatus httpStatus;	// HTTP 상태 코드
}
