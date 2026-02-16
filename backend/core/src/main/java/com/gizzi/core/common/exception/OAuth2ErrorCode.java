package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// OAuth2 소셜 로그인 관련 에러 코드
@Getter
@AllArgsConstructor
public enum OAuth2ErrorCode implements ErrorCode {

	// 지원하지 않는 소셜 제공자
	PROVIDER_NOT_FOUND    ("OAUTH2_PROVIDER_NOT_FOUND",     "지원하지 않는 소셜 로그인 제공자입니다",     "요청한 provider 코드에 해당하는 제공자가 없음",                  HttpStatus.NOT_FOUND),

	// 비활성화된 소셜 제공자
	PROVIDER_DISABLED     ("OAUTH2_PROVIDER_DISABLED",      "비활성화된 소셜 로그인 제공자입니다",       "해당 provider가 is_enabled=false 상태",                         HttpStatus.BAD_REQUEST),

	// Provider 설정 미완료 (client_id/secret 미설정)
	PROVIDER_NOT_CONFIGURED("OAUTH2_PROVIDER_NOT_CONFIGURED", "소셜 로그인 제공자 설정이 완료되지 않았습니다", "client_id 또는 client_secret이 미설정",                      HttpStatus.BAD_REQUEST),

	// OAuth2 state 검증 실패 (CSRF 방지)
	INVALID_STATE         ("OAUTH2_INVALID_STATE",          "유효하지 않은 OAuth2 상태값입니다",         "Redis에 저장된 state와 콜백의 state가 불일치",                  HttpStatus.BAD_REQUEST),

	// 토큰 교환 실패
	TOKEN_EXCHANGE_FAILED ("OAUTH2_TOKEN_EXCHANGE_FAILED",  "소셜 로그인 토큰 교환에 실패했습니다",      "인가 코드로 액세스 토큰 교환 시 에러 발생",                     HttpStatus.BAD_GATEWAY),

	// 사용자 정보 조회 실패
	USERINFO_FAILED       ("OAUTH2_USERINFO_FAILED",        "소셜 사용자 정보를 가져올 수 없습니다",     "사용자 정보 API 호출 실패 또는 파싱 에러",                      HttpStatus.BAD_GATEWAY),

	// 이메일 정보 없음 (소셜 Provider에서 이메일 미제공)
	EMAIL_NOT_PROVIDED    ("OAUTH2_EMAIL_NOT_PROVIDED",     "소셜 계정에 이메일 정보가 없습니다",        "소셜 제공자가 이메일을 제공하지 않아 계정 생성 불가",           HttpStatus.BAD_REQUEST),

	// 연동 대기 정보가 만료되었거나 존재하지 않음
	LINK_PENDING_NOT_FOUND("OAUTH2_LINK_PENDING_NOT_FOUND", "연동 요청이 만료되었습니다",                "Redis에 link-pending 정보가 없음 (만료 또는 잘못된 pendingId)", HttpStatus.BAD_REQUEST),

	// 연동 확인 시 로컬 ID/PW 불일치
	LINK_CONFIRM_FAILED   ("OAUTH2_LINK_CONFIRM_FAILED",   "아이디 또는 비밀번호가 올바르지 않습니다",   "연동 확인 시 제공된 로컬 ID/PW 검증 실패",                     HttpStatus.UNAUTHORIZED),

	// 이미 다른 계정에 연동된 소셜 계정
	ALREADY_LINKED        ("OAUTH2_ALREADY_LINKED",         "이미 연동된 소셜 계정입니다",               "다른 사용자에게 이미 연동된 Provider+Subject 조합",            HttpStatus.CONFLICT),

	// 최소 인증 수단 유지 위반 (연동 해제 시)
	LAST_AUTH_METHOD      ("OAUTH2_LAST_AUTH_METHOD",       "최소 1개의 인증 수단이 필요합니다",         "소셜 연동 해제 시 다른 인증 수단이 없으면 해제 불가",          HttpStatus.BAD_REQUEST),

	// 연동 해제 대상을 찾을 수 없음
	IDENTITY_NOT_FOUND    ("OAUTH2_IDENTITY_NOT_FOUND",     "연동 정보를 찾을 수 없습니다",             "해제하려는 Identity가 존재하지 않거나 본인 것이 아님",         HttpStatus.NOT_FOUND),

	// 소셜 전용 사용자가 비밀번호 미설정 상태에서 연동 추가/해제 시도
	PASSWORD_REQUIRED     ("OAUTH2_PASSWORD_REQUIRED",      "비밀번호 설정이 필요합니다",               "소셜 전용 사용자는 로컬 ID/PW 설정 후 연동 관리 가능",        HttpStatus.BAD_REQUEST),

	// 이미 로컬 자격증명이 있는 사용자가 다시 설정 시도
	ALREADY_LOCAL         ("OAUTH2_ALREADY_LOCAL",          "이미 비밀번호가 설정되어 있습니다",         "provider가 이미 LOCAL인 사용자의 set-password 시도",           HttpStatus.CONFLICT);

	// 에러 코드 문자열
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
