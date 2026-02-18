package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// SMS 관련 에러 코드
@Getter
@AllArgsConstructor
public enum SmsErrorCode implements ErrorCode {

	// SMS 프로바이더를 찾을 수 없음
	SMS_PROVIDER_NOT_FOUND  ("SMS_PROVIDER_NOT_FOUND",   "SMS 프로바이더를 찾을 수 없습니다",       "요청한 ID에 해당하는 SMS 프로바이더가 없음",          HttpStatus.NOT_FOUND),

	// SMS 프로바이더 비활성화 상태
	SMS_PROVIDER_DISABLED   ("SMS_PROVIDER_DISABLED",    "SMS 프로바이더가 비활성화되어 있습니다",   "해당 프로바이더가 is_enabled=false 상태",             HttpStatus.FORBIDDEN),

	// 활성화된 SMS 프로바이더가 없음
	SMS_NO_ACTIVE_PROVIDER  ("SMS_NO_ACTIVE_PROVIDER",   "활성화된 SMS 프로바이더가 없습니다",       "is_enabled=true인 프로바이더가 존재하지 않음",        HttpStatus.SERVICE_UNAVAILABLE),

	// SMS 발송 실패
	SMS_SEND_FAILED         ("SMS_SEND_FAILED",          "SMS 발송에 실패했습니다",                  "SMS 프로바이더 API 호출 실패",                        HttpStatus.INTERNAL_SERVER_ERROR),

	// OTP 코드 만료
	SMS_OTP_EXPIRED         ("SMS_OTP_EXPIRED",          "인증 코드가 만료되었습니다",               "Redis에서 OTP 키가 만료/삭제됨",                      HttpStatus.BAD_REQUEST),

	// OTP 코드 불일치
	SMS_OTP_INVALID         ("SMS_OTP_INVALID",          "인증 코드가 올바르지 않습니다",            "입력한 OTP 코드가 저장된 코드와 불일치",              HttpStatus.BAD_REQUEST),

	// 재발송 대기 시간 (1분 내 재발송 차단)
	SMS_RATE_LIMITED        ("SMS_RATE_LIMITED",          "잠시 후 다시 시도해 주세요",               "동일 번호 1분 내 재발송 차단",                        HttpStatus.TOO_MANY_REQUESTS),

	// 일일 발송 한도 초과
	SMS_DAILY_LIMIT         ("SMS_DAILY_LIMIT",          "일일 발송 한도를 초과했습니다",            "동일 번호 하루 최대 발송 건수 초과",                  HttpStatus.TOO_MANY_REQUESTS),

	// SMS 인증 기능 비활성화
	SMS_DISABLED            ("SMS_DISABLED",             "SMS 인증이 비활성화되어 있습니다",         "시스템 설정(sms/enabled)이 false 상태",               HttpStatus.FORBIDDEN),

	// OTP 검증 토큰 무효
	SMS_VERIFICATION_INVALID("SMS_VERIFICATION_INVALID", "전화번호 인증이 완료되지 않았습니다",      "Redis에 검증 토큰이 없거나 만료됨",                   HttpStatus.BAD_REQUEST);

	// 에러 코드 문자열
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
