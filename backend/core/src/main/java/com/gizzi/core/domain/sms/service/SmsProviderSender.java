package com.gizzi.core.domain.sms.service;

import com.gizzi.core.domain.sms.entity.SmsProviderEntity;

// SMS 발송 프로바이더 인터페이스
// OAuth2UserInfoExtractor 패턴과 동일하게 멀티 프로바이더를 지원한다
// 각 구현체는 Spring Bean으로 등록되며, SmsService에서 코드 기반으로 매핑한다
public interface SmsProviderSender {

	// 지원하는 프로바이더 코드 (예: "solapi", "aws_sns")
	String getProviderCode();

	// SMS 발송
	// provider: DB에서 조회한 프로바이더 엔티티 (API Key, Secret 등 자격증명 포함)
	// to: 수신 전화번호
	// message: 발송 메시지 내용
	void send(SmsProviderEntity provider, String to, String message);
}
