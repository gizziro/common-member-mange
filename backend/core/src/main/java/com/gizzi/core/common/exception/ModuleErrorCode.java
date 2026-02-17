package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 모듈 시스템 관련 에러 코드 (MODULE_*)
// 모듈/인스턴스 CRUD, 슬러그 검증, 상태 관리에서 발생하는 비즈니스 에러
@Getter
@AllArgsConstructor
public enum ModuleErrorCode implements ErrorCode {

	// 모듈 조회 실패 (코드 또는 슬러그로 조회 시 존재하지 않음)
	MODULE_NOT_FOUND                ("MODULE_NOT_FOUND",                  "모듈을 찾을 수 없습니다",                       "모듈 코드 또는 슬러그로 조회 실패",          HttpStatus.NOT_FOUND),

	// 모듈 코드 중복 (모듈 등록 시 code 중복 검증 실패)
	MODULE_DUPLICATE_CODE           ("MODULE_DUPLICATE_CODE",             "이미 사용 중인 모듈 코드입니다",                 "module code 중복 검증 실패",               HttpStatus.CONFLICT),

	// 모듈 슬러그 중복 (모듈 등록 시 slug 중복 검증 실패)
	MODULE_DUPLICATE_SLUG           ("MODULE_DUPLICATE_SLUG",             "이미 사용 중인 모듈 슬러그입니다",               "module slug 중복 검증 실패",               HttpStatus.CONFLICT),

	// 모듈 인스턴스 조회 실패
	MODULE_INSTANCE_NOT_FOUND       ("MODULE_INSTANCE_NOT_FOUND",         "모듈 인스턴스를 찾을 수 없습니다",               "인스턴스 PK 또는 슬러그로 조회 실패",        HttpStatus.NOT_FOUND),

	// 인스턴스 슬러그 중복 (동일 모듈 내에서 slug 중복)
	MODULE_INSTANCE_DUPLICATE_SLUG  ("MODULE_INSTANCE_DUPLICATE_SLUG",    "이미 사용 중인 인스턴스 슬러그입니다",           "동일 모듈 내 instance slug 중복",           HttpStatus.CONFLICT),

	// 인스턴스 이름 중복 (동일 모듈 내에서 instance_name 중복)
	MODULE_INSTANCE_DUPLICATE_NAME  ("MODULE_INSTANCE_DUPLICATE_NAME",    "이미 사용 중인 인스턴스 이름입니다",             "동일 모듈 내 instance_name 중복",           HttpStatus.CONFLICT),

	// SINGLE 타입 모듈에서 인스턴스 생성 시도
	MODULE_SINGLE_NO_INSTANCE       ("MODULE_SINGLE_NO_INSTANCE",         "SINGLE 타입 모듈은 인스턴스를 생성할 수 없습니다", "SINGLE 모듈에 인스턴스 생성 시도",          HttpStatus.BAD_REQUEST),

	// 비활성 모듈 접근 시도
	MODULE_DISABLED                 ("MODULE_DISABLED",                   "비활성 상태인 모듈입니다",                      "is_enabled=false인 모듈 접근 시도",         HttpStatus.BAD_REQUEST),

	// 슬러그 형식 오류 (영소문자+숫자+하이픈 규칙 위반)
	MODULE_INVALID_SLUG             ("MODULE_INVALID_SLUG",               "올바르지 않은 슬러그 형식입니다",                "slug 정규식 검증 실패",                    HttpStatus.BAD_REQUEST);

	// 에러 코드 문자열 (예: "MODULE_NOT_FOUND")
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
