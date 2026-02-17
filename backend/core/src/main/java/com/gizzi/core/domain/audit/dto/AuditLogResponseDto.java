package com.gizzi.core.domain.audit.dto;

import com.gizzi.core.domain.audit.entity.AuditLogEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 감사 로그 응답 DTO
@Getter
@Builder
public class AuditLogResponseDto {

	// 감사 로그 PK
	private final String        id;

	// 행위자 사용자 PK
	private final String        actorUserId;

	// 행위자 유형 (USER/SYSTEM)
	private final String        actorType;

	// 액션 유형
	private final String        actionType;

	// 대상 유형
	private final String        targetType;

	// 대상 ID
	private final String        targetId;

	// 결과 상태
	private final String        resultStatus;

	// 요약 메시지
	private final String        message;

	// 상세 메타데이터 (JSON 문자열)
	private final String        metadataJson;

	// 요청 IP 주소
	private final String        ipAddress;

	// 요청 User-Agent
	private final String        userAgent;

	// 발생 일시
	private final LocalDateTime createdAt;

	// 엔티티 → DTO 정적 팩토리 메서드
	public static AuditLogResponseDto from(AuditLogEntity entity) {
		return AuditLogResponseDto.builder()
			.id(entity.getId())
			.actorUserId(entity.getActorUserId())
			.actorType(entity.getActorType())
			.actionType(entity.getActionType())
			.targetType(entity.getTargetType())
			.targetId(entity.getTargetId())
			.resultStatus(entity.getResultStatus())
			.message(entity.getMessage())
			.metadataJson(entity.getMetadataJson())
			.ipAddress(entity.getIpAddress())
			.userAgent(entity.getUserAgent())
			.createdAt(entity.getCreatedAt())
			.build();
	}
}
