package com.gizzi.core.domain.sms.dto;

import com.gizzi.core.domain.sms.entity.SmsProviderEntity;
import lombok.Builder;
import lombok.Getter;

// SMS 프로바이더 응답 DTO (관리자용 — 전체 정보 포함)
@Getter
@Builder
public class SmsProviderResponseDto {

	// 프로바이더 PK
	private final String  id;

	// 프로바이더 코드
	private final String  code;

	// 프로바이더 표시명
	private final String  name;

	// 활성화 여부
	private final Boolean isEnabled;

	// API Key
	private final String  apiKey;

	// API Secret (마스킹 처리)
	private final String  apiSecret;

	// 발신 번호
	private final String  senderNumber;

	// 추가 설정 JSON
	private final String  configJson;

	// 표시 순서
	private final Integer displayOrder;

	// 엔티티 → 응답 DTO 변환
	public static SmsProviderResponseDto from(SmsProviderEntity entity) {
		return SmsProviderResponseDto.builder()
			.id(entity.getId())
			.code(entity.getCode())
			.name(entity.getName())
			.isEnabled(entity.getIsEnabled())
			.apiKey(entity.getApiKey())
			// API Secret은 앞 4자만 표시하고 나머지 마스킹
			.apiSecret(maskSecret(entity.getApiSecret()))
			.senderNumber(entity.getSenderNumber())
			.configJson(entity.getConfigJson())
			.displayOrder(entity.getDisplayOrder())
			.build();
	}

	// API Secret 마스킹 (앞 4자만 보이고 나머지 ****)
	private static String maskSecret(String secret) {
		// null 또는 빈 문자열이면 그대로 반환
		if (secret == null || secret.isEmpty()) {
			return secret;
		}
		// 4자 이하이면 전체 마스킹
		if (secret.length() <= 4) {
			return "****";
		}
		// 앞 4자만 표시
		return secret.substring(0, 4) + "****";
	}
}
