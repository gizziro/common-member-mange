package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 시스템 초기 설정(Setup Wizard) 관련 에러 코드
// 최초 관리자 계정 생성 과정에서 발생하는 비즈니스 에러
@Getter
@AllArgsConstructor
public enum SetupErrorCode implements ErrorCode {

	// 이미 초기화된 시스템에서 재초기화 시도
	SYSTEM_ALREADY_INITIALIZED ("SETUP_ALREADY_INITIALIZED",    "이미 초기화된 시스템입니다",          "administrator 그룹에 멤버가 이미 존재",   HttpStatus.BAD_REQUEST),

	// 관리자 그룹이 DB에 존재하지 않음 (시드 데이터 누락)
	ADMIN_GROUP_NOT_FOUND      ("SETUP_ADMIN_GROUP_NOT_FOUND",  "관리자 그룹이 존재하지 않습니다",     "administrator 시드 데이터 누락",          HttpStatus.INTERNAL_SERVER_ERROR),

	// 미초기화 상태에서 셋업 외 API 접근 시도
	SETUP_REQUIRED             ("SETUP_REQUIRED",               "시스템 초기 설정이 필요합니다",       "administrator 그룹 멤버 0명 상태",        HttpStatus.FORBIDDEN);

	// 에러 코드 문자열 (예: "SETUP_ALREADY_INITIALIZED")
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
