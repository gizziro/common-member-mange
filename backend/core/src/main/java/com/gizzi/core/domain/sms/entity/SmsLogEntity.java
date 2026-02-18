package com.gizzi.core.domain.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// SMS 발송 이력 엔티티 — tb_sms_logs 테이블 매핑
// AuditLogEntity와 동일하게 BaseEntity를 상속하지 않는다 (updated_at 컬럼 없음)
@Entity
@Table(name = "tb_sms_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsLogEntity {

	// SMS 로그 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 발송 유형 (MANUAL: 관리자 수동 / AUTO: 시스템 자동)
	@Column(name = "send_type", nullable = false, length = 20)
	private String sendType;

	// AUTO 전용: 트리거 유형 (PASSWORD_RESET 등)
	@Column(name = "trigger_type", length = 50)
	private String triggerType;

	// 발송자(관리자) PK (AUTO 발송 시 null 가능)
	@Column(name = "sender_user_id", length = 36)
	private String senderUserId;

	// 수신 전화번호
	@Column(name = "recipient_phone", nullable = false, length = 20)
	private String recipientPhone;

	// 수신자 PK (비회원 수신 시 null)
	@Column(name = "recipient_user_id", length = 36)
	private String recipientUserId;

	// 메시지 내용
	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	// 사용 프로바이더 코드
	@Column(name = "provider_code", length = 50)
	private String providerCode;

	// 발송 상태 (PENDING / SUCCESS / FAILED)
	@Column(name = "send_status", nullable = false, length = 20)
	private String sendStatus;

	// 실패 시 에러 메시지
	@Column(name = "error_message", length = 500)
	private String errorMessage;

	// 대량 발송 묶음 ID
	@Column(name = "batch_id", length = 36)
	private String batchId;

	// 발송 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 영속화 직전 UUID + 생성 일시 자동 설정
	@PrePersist
	protected void onCreate() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}

	// 수동 발송 SMS 로그 생성 팩토리 메서드
	public static SmsLogEntity createManual(String senderUserId, String recipientPhone,
	                                        String recipientUserId, String message,
	                                        String providerCode, String batchId) {
		SmsLogEntity entity    = new SmsLogEntity();
		entity.sendType        = "MANUAL";
		entity.senderUserId    = senderUserId;
		entity.recipientPhone  = recipientPhone;
		entity.recipientUserId = recipientUserId;
		entity.message         = message;
		entity.providerCode    = providerCode;
		entity.sendStatus      = "PENDING";
		entity.batchId         = batchId;
		return entity;
	}

	// 자동 발송 SMS 로그 생성 팩토리 메서드
	public static SmsLogEntity createAuto(String triggerType, String senderUserId,
	                                      String recipientPhone, String recipientUserId,
	                                      String message, String providerCode) {
		SmsLogEntity entity    = new SmsLogEntity();
		entity.sendType        = "AUTO";
		entity.triggerType     = triggerType;
		entity.senderUserId    = senderUserId;
		entity.recipientPhone  = recipientPhone;
		entity.recipientUserId = recipientUserId;
		entity.message         = message;
		entity.providerCode    = providerCode;
		entity.sendStatus      = "PENDING";
		return entity;
	}

	// 발송 성공 처리
	public void markSuccess() {
		this.sendStatus = "SUCCESS";
	}

	// 발송 실패 처리
	public void markFailed(String errorMessage) {
		this.sendStatus   = "FAILED";
		this.errorMessage = errorMessage;
	}
}
