package com.gizzi.core.domain.sms.dto;

import com.gizzi.core.domain.sms.entity.SmsLogEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// SMS 발송 이력 응답 DTO
@Getter
@Builder
public class SmsLogResponseDto {

	// SMS 로그 PK
	private final String        id;

	// 발송 유형 (MANUAL / AUTO)
	private final String        sendType;

	// AUTO 전용: 트리거 유형 (PASSWORD_RESET 등)
	private final String        triggerType;

	// 발송자(관리자) PK
	private final String        senderUserId;

	// 수신 전화번호
	private final String        recipientPhone;

	// 수신자 PK
	private final String        recipientUserId;

	// 메시지 내용
	private final String        message;

	// 사용 프로바이더 코드
	private final String        providerCode;

	// 발송 상태 (PENDING / SUCCESS / FAILED)
	private final String        sendStatus;

	// 실패 시 에러 메시지
	private final String        errorMessage;

	// 대량 발송 묶음 ID
	private final String        batchId;

	// 발송 일시
	private final LocalDateTime createdAt;

	// 엔티티 → 응답 DTO 변환 팩토리 메서드
	public static SmsLogResponseDto from(SmsLogEntity entity) {
		return SmsLogResponseDto.builder()
			.id(entity.getId())
			.sendType(entity.getSendType())
			.triggerType(entity.getTriggerType())
			.senderUserId(entity.getSenderUserId())
			.recipientPhone(entity.getRecipientPhone())
			.recipientUserId(entity.getRecipientUserId())
			.message(entity.getMessage())
			.providerCode(entity.getProviderCode())
			.sendStatus(entity.getSendStatus())
			.errorMessage(entity.getErrorMessage())
			.batchId(entity.getBatchId())
			.createdAt(entity.getCreatedAt())
			.build();
	}
}
