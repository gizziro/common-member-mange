package com.gizzi.core.domain.sms.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SmsErrorCode;
import com.gizzi.core.domain.audit.AuditAction;
import com.gizzi.core.domain.audit.AuditTarget;
import com.gizzi.core.domain.audit.service.AuditLogService;
import com.gizzi.core.domain.setting.service.SettingService;
import com.gizzi.core.domain.sms.dto.VerifyOtpResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// OTP 생성/검증 서비스 (Redis 기반)
// SMS로 발송되는 인증 코드의 생성, 저장, 검증을 담당한다
// Redis 키 패턴:
//   sms:otp:{phone}        → OTP 코드, TTL: 설정값 (기본 3분)
//   sms:rate:{phone}       → 재발송 방지, TTL: 1분
//   sms:daily:{phone}      → 일일 발송 카운터, TTL: 24시간
//   sms:verified:{phone}   → 인증 완료 토큰, TTL: 10분 (회원가입/셋업에서 사용)
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

	// Redis 문자열 작업 템플릿
	private final StringRedisTemplate redisTemplate;

	// SMS 발송 서비스
	private final SmsService          smsService;

	// 시스템 설정 서비스
	private final SettingService      settingService;

	// 감사 로그 서비스
	private final AuditLogService     auditLogService;

	// Redis 키 접두사
	private static final String OTP_PREFIX      = "sms:otp:";
	private static final String RATE_PREFIX     = "sms:rate:";
	private static final String DAILY_PREFIX    = "sms:daily:";
	private static final String VERIFIED_PREFIX = "sms:verified:";

	// 재발송 방지 TTL (60초)
	private static final long   RATE_LIMIT_TTL  = 60;

	// 일일 한도 TTL (24시간)
	private static final long   DAILY_LIMIT_TTL = 86400;

	// 인증 완료 토큰 TTL (10분)
	private static final long   VERIFIED_TTL    = 600;

	// 보안 난수 생성기
	private final SecureRandom  secureRandom    = new SecureRandom();

	// OTP 생성 → 레이트 체크 → SMS 발송 → Redis 저장
	public void generateAndSend(String phone) {
		// 시스템 설정: SMS 인증 활성화 여부 확인
		boolean smsEnabled = settingService.getSystemBoolean("sms", "enabled");
		if (!smsEnabled) {
			throw new BusinessException(SmsErrorCode.SMS_DISABLED);
		}

		// 1분 내 재발송 차단 확인
		if (isRateLimited(phone)) {
			throw new BusinessException(SmsErrorCode.SMS_RATE_LIMITED);
		}

		// 일일 발송 한도 확인
		if (isDailyLimitExceeded(phone)) {
			throw new BusinessException(SmsErrorCode.SMS_DAILY_LIMIT);
		}

		// OTP 코드 생성 (시스템 설정에서 길이 조회)
		int otpLength = (int) settingService.getSystemNumber("sms", "otp_length");
		String otpCode = generateOtpCode(otpLength);

		// OTP TTL 조회 (초)
		long otpTtl = (long) settingService.getSystemNumber("sms", "otp_ttl_seconds");

		// SMS 발송
		String message = "[인증코드] " + otpCode + " (유효시간 " + (otpTtl / 60) + "분)";
		smsService.send(phone, message);

		// Redis에 OTP 코드 저장
		redisTemplate.opsForValue().set(
			OTP_PREFIX + phone, otpCode, otpTtl, TimeUnit.SECONDS
		);

		// 재발송 방지 키 저장 (1분 TTL)
		redisTemplate.opsForValue().set(
			RATE_PREFIX + phone, "1", RATE_LIMIT_TTL, TimeUnit.SECONDS
		);

		// 일일 발송 카운터 증가
		incrementDailyCount(phone);

		log.info("OTP 발송 완료: phone={}, otpLength={}, ttl={}초", phone, otpLength, otpTtl);
	}

	// OTP 코드 검증 → 성공 시 Redis 키 삭제 + 검증 토큰 발급
	public VerifyOtpResponseDto verify(String phone, String code) {
		// Redis에서 OTP 코드 조회
		String storedCode = redisTemplate.opsForValue().get(OTP_PREFIX + phone);

		// OTP 키가 없으면 만료
		if (storedCode == null) {
			// 인증 실패 감사 로그
			auditLogService.logFailure(null, AuditAction.SMS_OTP_VERIFY, AuditTarget.SMS, null,
				"OTP 만료: " + phone, null);
			throw new BusinessException(SmsErrorCode.SMS_OTP_EXPIRED);
		}

		// OTP 코드 일치 확인
		if (!storedCode.equals(code)) {
			// 인증 실패 감사 로그
			auditLogService.logFailure(null, AuditAction.SMS_OTP_VERIFY, AuditTarget.SMS, null,
				"OTP 불일치: " + phone, null);
			throw new BusinessException(SmsErrorCode.SMS_OTP_INVALID);
		}

		// 인증 성공 → OTP 키 삭제 (1회용)
		redisTemplate.delete(OTP_PREFIX + phone);

		// 검증 토큰 생성 → Redis에 저장 (회원가입/셋업에서 phoneVerified 증명용)
		String verificationToken = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set(
			VERIFIED_PREFIX + phone, verificationToken, VERIFIED_TTL, TimeUnit.SECONDS
		);

		// 인증 성공 감사 로그
		auditLogService.logSuccess(null, AuditAction.SMS_OTP_VERIFY, AuditTarget.SMS, null,
			"OTP 인증 성공: " + phone, Map.of("phone", phone));

		log.info("OTP 인증 성공: phone={}", phone);

		return VerifyOtpResponseDto.success(verificationToken);
	}

	// 전화번호 인증 완료 여부 확인 (검증 토큰 유효성 체크)
	public boolean isPhoneVerified(String phone, String verificationToken) {
		// Redis에서 검증 토큰 조회
		String storedToken = redisTemplate.opsForValue().get(VERIFIED_PREFIX + phone);
		// 저장된 토큰과 입력 토큰이 일치하면 인증 완료
		return storedToken != null && storedToken.equals(verificationToken);
	}

	// 인증 완료 토큰 삭제 (회원가입/셋업 완료 후 호출)
	public void consumeVerification(String phone) {
		redisTemplate.delete(VERIFIED_PREFIX + phone);
	}

	// 1분 내 재발송 여부 확인
	private boolean isRateLimited(String phone) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(RATE_PREFIX + phone));
	}

	// 일일 발송 한도 초과 여부 확인
	private boolean isDailyLimitExceeded(String phone) {
		// 시스템 설정: 일일 발송 한도
		int dailyLimit = (int) settingService.getSystemNumber("sms", "daily_limit");
		// Redis에서 현재 일일 발송 카운터 조회
		String countStr = redisTemplate.opsForValue().get(DAILY_PREFIX + phone);
		if (countStr == null) {
			return false;
		}
		return Integer.parseInt(countStr) >= dailyLimit;
	}

	// 일일 발송 카운터 증가
	private void incrementDailyCount(String phone) {
		String key = DAILY_PREFIX + phone;
		// 카운터 1 증가 (키가 없으면 0에서 시작)
		Long count = redisTemplate.opsForValue().increment(key);
		// 첫 증가 시 TTL 설정 (24시간)
		if (count != null && count == 1) {
			redisTemplate.expire(key, DAILY_LIMIT_TTL, TimeUnit.SECONDS);
		}
	}

	// OTP 코드 생성 (숫자만, 지정 길이)
	private String generateOtpCode(int length) {
		// SecureRandom으로 숫자 코드 생성
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(secureRandom.nextInt(10));
		}
		return sb.toString();
	}
}
