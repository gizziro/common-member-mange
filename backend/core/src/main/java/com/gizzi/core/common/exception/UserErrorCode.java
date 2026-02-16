package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 사용자 관련 에러 코드 (USER_*)
// 회원가입, 프로필 등 사용자 도메인에서 발생하는 비즈니스 에러
@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

	// 아이디 중복 (회원가입 시 userId 중복 검증 실패)
	DUPLICATE_USER_ID("USER_DUPLICATE_ID",    "이미 사용 중인 아이디입니다", "userId 중복 검증 실패",          HttpStatus.CONFLICT),

	// 이메일 중복 (회원가입 시 email 중복 검증 실패)
	DUPLICATE_EMAIL  ("USER_DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다", "email 중복 검증 실패",           HttpStatus.CONFLICT),

	// 사용자 조회 실패 (PK 또는 userId로 조회 시 존재하지 않음)
	USER_NOT_FOUND   ("USER_NOT_FOUND",       "사용자를 찾을 수 없습니다",   "PK 또는 userId로 조회 실패",    HttpStatus.NOT_FOUND),

	// 잠금 상태가 아닌 계정에 대한 잠금 해제 시도
	USER_NOT_LOCKED  ("USER_NOT_LOCKED",      "잠금 상태가 아닌 계정입니다", "잠금 해제 대상이 아닌 사용자",  HttpStatus.BAD_REQUEST),

	// 자기 자신 삭제 시도 방지
	SELF_DELETE      ("USER_SELF_DELETE",     "자기 자신은 삭제할 수 없습니다",          "로그인한 사용자 본인 삭제 시도",          HttpStatus.BAD_REQUEST),

	// 관리자 그룹 소속 사용자 삭제 시도 방지
	ADMIN_USER_UNDELETABLE("USER_ADMIN_UNDELETABLE", "관리자 그룹 소속 사용자는 삭제할 수 없습니다", "administrator 그룹 소속 사용자 삭제 시도", HttpStatus.BAD_REQUEST);

	// 에러 코드 문자열 (예: "USER_DUPLICATE_ID")
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
