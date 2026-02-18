package com.gizzi.core.domain.sms.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SmsErrorCode;
import com.gizzi.core.domain.audit.AuditAction;
import com.gizzi.core.domain.audit.AuditTarget;
import com.gizzi.core.domain.audit.service.AuditLogService;
import com.gizzi.core.domain.setting.service.SettingService;
import com.gizzi.core.domain.sms.entity.SmsProviderEntity;
import com.gizzi.core.domain.sms.repository.SmsProviderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// SMS 발송 오케스트레이션 서비스
// DB에서 활성 프로바이더를 조회하고, 해당 구현체로 SMS를 발송한다
// OAuth2Service와 동일한 멀티 프로바이더 패턴 적용
@Slf4j
@Service
@Transactional(readOnly = true)
public class SmsService {

	// 프로바이더 코드 → SmsProviderSender 구현체 매핑 (OAuth2UserInfoExtractor 패턴)
	private final Map<String, SmsProviderSender> senderMap;

	// SMS 프로바이더 리포지토리
	private final SmsProviderRepository          smsProviderRepository;

	// 시스템 설정 서비스
	private final SettingService                 settingService;

	// 감사 로그 서비스
	private final AuditLogService                auditLogService;

	// 수동 생성자 — SmsProviderSender 리스트를 코드 기반 Map으로 변환
	// OAuth2Service와 동일한 패턴 (List → Map 변환 때문에 @RequiredArgsConstructor 미사용)
	public SmsService(List<SmsProviderSender> senders,
	                  SmsProviderRepository smsProviderRepository,
	                  SettingService settingService,
	                  AuditLogService auditLogService) {
		// 프로바이더 코드 기반 Map 생성
		this.senderMap             = senders.stream()
			.collect(Collectors.toMap(SmsProviderSender::getProviderCode, Function.identity()));
		this.smsProviderRepository = smsProviderRepository;
		this.settingService        = settingService;
		this.auditLogService       = auditLogService;

		log.info("SMS 프로바이더 Sender 등록: {}", senderMap.keySet());
	}

	// SMS 발송 (활성 프로바이더 중 첫 번째 사용)
	public void send(String to, String message) {
		// 시스템 설정: SMS 인증 활성화 여부 확인
		boolean smsEnabled = settingService.getSystemBoolean("sms", "enabled");
		if (!smsEnabled) {
			throw new BusinessException(SmsErrorCode.SMS_DISABLED);
		}

		// 활성화된 프로바이더 목록 조회 (표시 순서 정렬)
		List<SmsProviderEntity> activeProviders = smsProviderRepository
			.findByIsEnabledTrueOrderByDisplayOrder();

		if (activeProviders.isEmpty()) {
			throw new BusinessException(SmsErrorCode.SMS_NO_ACTIVE_PROVIDER);
		}

		// 첫 번째 활성 프로바이더 선택
		SmsProviderEntity provider = activeProviders.get(0);

		// 프로바이더 코드로 Sender 구현체 찾기
		SmsProviderSender sender = senderMap.get(provider.getCode());
		if (sender == null) {
			log.error("SMS Sender 구현체를 찾을 수 없음: code={}", provider.getCode());
			throw new BusinessException(SmsErrorCode.SMS_SEND_FAILED);
		}

		// SMS 발송
		sender.send(provider, to, message);

		// 발송 감사 로그
		auditLogService.logSuccess(null, AuditAction.SMS_SEND, AuditTarget.SMS, null,
			"SMS 발송: " + to, Map.of("provider", provider.getCode()));
	}

	// 특정 프로바이더로 테스트 SMS 발송
	public void sendTest(String providerId, String to) {
		// 프로바이더 조회
		SmsProviderEntity provider = smsProviderRepository.findById(providerId)
			.orElseThrow(() -> new BusinessException(SmsErrorCode.SMS_PROVIDER_NOT_FOUND));

		// 비활성화 상태 확인
		if (!Boolean.TRUE.equals(provider.getIsEnabled())) {
			throw new BusinessException(SmsErrorCode.SMS_PROVIDER_DISABLED);
		}

		// Sender 구현체 찾기
		SmsProviderSender sender = senderMap.get(provider.getCode());
		if (sender == null) {
			log.error("SMS Sender 구현체를 찾을 수 없음: code={}", provider.getCode());
			throw new BusinessException(SmsErrorCode.SMS_SEND_FAILED);
		}

		// 테스트 메시지 발송
		String testMessage = "[테스트] SMS 발송 테스트입니다.";
		sender.send(provider, to, testMessage);

		log.info("SMS 테스트 발송 성공: provider={}, to={}", provider.getCode(), to);
	}
}
