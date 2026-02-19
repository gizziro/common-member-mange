package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// SMS 관련 에러 코드 (SMS_*)
// SMS 발송, OTP 인증, 프로바이더 관리에서 발생하는 비즈니스 에러
@Getter
@AllArgsConstructor
public enum SmsErrorCode implements ErrorCode
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 프로바이더 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	SMS_PROVIDER_NOT_FOUND    ("SMS_PROVIDER_NOT_FOUND",    "SMS 프로바이더를 찾을 수 없습니다",       "요청한 ID에 해당하는 SMS 프로바이더가 없음",          HttpStatus.NOT_FOUND),				// 프로바이더 조회 실패
	SMS_PROVIDER_DISABLED     ("SMS_PROVIDER_DISABLED",     "SMS 프로바이더가 비활성화되어 있습니다",   "해당 프로바이더가 is_enabled=false 상태",             HttpStatus.FORBIDDEN),				// 프로바이더 비활성화
	SMS_NO_ACTIVE_PROVIDER    ("SMS_NO_ACTIVE_PROVIDER",    "활성화된 SMS 프로바이더가 없습니다",       "is_enabled=true인 프로바이더가 존재하지 않음",        HttpStatus.SERVICE_UNAVAILABLE),	// 활성 프로바이더 없음
	SMS_SEND_FAILED           ("SMS_SEND_FAILED",           "SMS 발송에 실패했습니다",                  "SMS 프로바이더 API 호출 실패",                        HttpStatus.INTERNAL_SERVER_ERROR),	// 발송 실패

	//----------------------------------------------------------------------------------------------------------------------
	// [ OTP 인증 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	SMS_OTP_EXPIRED           ("SMS_OTP_EXPIRED",           "인증 코드가 만료되었습니다",               "Redis에서 OTP 키가 만료/삭제됨",                      HttpStatus.BAD_REQUEST),			// OTP 만료
	SMS_OTP_INVALID           ("SMS_OTP_INVALID",           "인증 코드가 올바르지 않습니다",            "입력한 OTP 코드가 저장된 코드와 불일치",              HttpStatus.BAD_REQUEST),			// OTP 불일치

	//----------------------------------------------------------------------------------------------------------------------
	// [ 발송 제한 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	SMS_RATE_LIMITED           ("SMS_RATE_LIMITED",           "잠시 후 다시 시도해 주세요",               "동일 번호 1분 내 재발송 차단",                        HttpStatus.TOO_MANY_REQUESTS),		// 재발송 대기
	SMS_DAILY_LIMIT            ("SMS_DAILY_LIMIT",            "일일 발송 한도를 초과했습니다",            "동일 번호 하루 최대 발송 건수 초과",                  HttpStatus.TOO_MANY_REQUESTS),		// 일일 한도 초과

	//----------------------------------------------------------------------------------------------------------------------
	// [ 기능 상태 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	SMS_DISABLED               ("SMS_DISABLED",               "SMS 인증이 비활성화되어 있습니다",         "시스템 설정(sms/enabled)이 false 상태",               HttpStatus.FORBIDDEN),				// SMS 기능 비활성화
	SMS_VERIFICATION_INVALID   ("SMS_VERIFICATION_INVALID",   "전화번호 인증이 완료되지 않았습니다",      "Redis에 검증 토큰이 없거나 만료됨",                   HttpStatus.BAD_REQUEST),			// 인증 미완료

	//----------------------------------------------------------------------------------------------------------------------
	// [ 수동 발송 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	SMS_NO_RECIPIENTS          ("SMS_NO_RECIPIENTS",          "SMS 수신 대상이 없습니다",                 "조건에 맞는 수신자가 0명",                            HttpStatus.BAD_REQUEST),			// 수신 대상 없음
	SMS_INVALID_RECIPIENT_TYPE ("SMS_INVALID_RECIPIENT_TYPE", "잘못된 수신 대상 유형입니다",               "recipientType 값이 유효하지 않음",                    HttpStatus.BAD_REQUEST);			// 수신 유형 오류

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String     code;			// 에러 코드 문자열
	private final String     message;		// 사용자에게 표시할 에러 메시지
	private final String     description;	// 개발자용 상세 설명
	private final HttpStatus httpStatus;	// HTTP 상태 코드
}
