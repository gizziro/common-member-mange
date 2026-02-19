package com.gizzi.core.domain.sms.dto;

import com.gizzi.core.domain.sms.entity.SmsLogEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// SMS 발송 이력 응답 DTO
// 발송 유형, 수신자, 상태, 에러 정보 등 이력 전체 필드를 포함한다
@Getter
@Builder
public class SmsLogResponseDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String        id;					// SMS 로그 PK
	private final String        sendType;			// 발송 유형 (MANUAL / AUTO)
	private final String        triggerType;		// AUTO 전용: 트리거 유형 (PASSWORD_RESET 등)
	private final String        senderUserId;		// 발송자(관리자) PK
	private final String        recipientPhone;		// 수신 전화번호
	private final String        recipientUserId;	// 수신자 PK
	private final String        message;			// 메시지 내용
	private final String        providerCode;		// 사용 프로바이더 코드
	private final String        sendStatus;			// 발송 상태 (PENDING / SUCCESS / FAILED)
	private final String        errorMessage;		// 실패 시 에러 메시지
	private final String        batchId;			// 대량 발송 묶음 ID
	private final LocalDateTime createdAt;			// 발송 일시

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 엔티티 → 응답 DTO 변환 팩토리 메서드
	public static SmsLogResponseDto from(SmsLogEntity entity)
	{
		// 빌더 패턴으로 모든 필드를 엔티티에서 복사
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
