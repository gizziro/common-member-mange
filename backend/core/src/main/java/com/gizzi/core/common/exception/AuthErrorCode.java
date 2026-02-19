package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 인증/인가 관련 에러 코드 (AUTH_*)
// 로그인, 토큰, 계정 상태 등 인증 흐름에서 발생하는 에러
@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 인증 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	UNAUTHORIZED         ("AUTH_UNAUTHORIZED",          "인증이 필요합니다",                        "Authorization 헤더 누락 또는 무효",             HttpStatus.UNAUTHORIZED),		// 인증되지 않은 요청
	ACCESS_DENIED        ("AUTH_ACCESS_DENIED",         "접근 권한이 없습니다",                     "인증은 되었으나 해당 리소스에 대한 권한 없음",   HttpStatus.FORBIDDEN),			// 접근 권한 부족
	INVALID_CREDENTIALS  ("AUTH_INVALID_CREDENTIALS",   "아이디 또는 비밀번호가 올바르지 않습니다", "로그인 자격증명 검증 실패",                      HttpStatus.UNAUTHORIZED),		// 로그인 자격증명 불일치

	//----------------------------------------------------------------------------------------------------------------------
	// [ 계정 상태 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	ACCOUNT_LOCKED       ("AUTH_ACCOUNT_LOCKED",        "계정이 잠겨있습니다",                     "로그인 실패 5회 초과로 계정 잠금됨",             HttpStatus.FORBIDDEN),			// 계정 잠금 상태
	ACCOUNT_PENDING      ("AUTH_ACCOUNT_PENDING",       "계정 인증이 필요합니다",                  "계정 상태가 PENDING으로 아직 활성화되지 않음",   HttpStatus.FORBIDDEN),			// 계정 인증 대기

	//----------------------------------------------------------------------------------------------------------------------
	// [ 토큰 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	TOKEN_EXPIRED        ("AUTH_TOKEN_EXPIRED",         "토큰이 만료되었습니다",                   "Access Token의 exp 클레임이 현재 시각을 초과",  HttpStatus.UNAUTHORIZED),		// Access Token 만료
	INVALID_TOKEN        ("AUTH_INVALID_TOKEN",         "유효하지 않은 토큰입니다",                "JWT 서명 검증 실패 또는 형식 오류",              HttpStatus.UNAUTHORIZED),		// 유효하지 않은 토큰
	REFRESH_TOKEN_EXPIRED("AUTH_REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다",          "Refresh Token의 exp 클레임이 현재 시각을 초과", HttpStatus.UNAUTHORIZED);		// Refresh Token 만료

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String     code;			// 에러 코드 문자열 (예: "AUTH_UNAUTHORIZED")
	private final String     message;		// 사용자에게 표시할 에러 메시지
	private final String     description;	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final HttpStatus httpStatus;	// HTTP 상태 코드
}
