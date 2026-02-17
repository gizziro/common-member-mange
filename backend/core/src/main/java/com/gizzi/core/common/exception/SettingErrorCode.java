package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 설정 관련 에러 코드 (SETTING_*)
// 설정 CRUD에서 발생하는 비즈니스 에러
@Getter
@AllArgsConstructor
public enum SettingErrorCode implements ErrorCode {

	// 설정 조회 실패 (module_code + setting_group + setting_key로 조회 시 존재하지 않음)
	SETTING_NOT_FOUND      ("SETTING_NOT_FOUND",       "설정을 찾을 수 없습니다",             "설정 키 조회 실패",        HttpStatus.NOT_FOUND),

	// 읽기 전용 설정 수정 시도 (is_readonly=true인 설정 변경 시도)
	SETTING_READONLY       ("SETTING_READONLY",        "읽기 전용 설정은 변경할 수 없습니다",   "읽기 전용 설정 수정 시도",   HttpStatus.BAD_REQUEST),

	// 설정 값 형식 오류 (NUMBER 타입에 문자열 등 잘못된 값 입력)
	SETTING_INVALID_VALUE  ("SETTING_INVALID_VALUE",   "유효하지 않은 설정 값입니다",          "설정 값 타입 불일치",       HttpStatus.BAD_REQUEST);

	// 에러 코드 문자열 (예: "SETTING_NOT_FOUND")
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
