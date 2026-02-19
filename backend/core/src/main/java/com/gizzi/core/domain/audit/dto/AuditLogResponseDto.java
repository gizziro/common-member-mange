package com.gizzi.core.domain.audit.dto;

import com.gizzi.core.domain.audit.entity.AuditLogEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 감사 로그 응답 DTO
// 클라이언트에 반환할 감사 로그 데이터를 담는 불변 객체
@Getter
@Builder
public class AuditLogResponseDto
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final String        id;				// 감사 로그 PK
	private final String        actorUserId;	// 행위자 사용자 PK
	private final String        actorType;		// 행위자 유형 (USER/SYSTEM)
	private final String        actionType;		// 액션 유형
	private final String        targetType;		// 대상 유형
	private final String        targetId;		// 대상 ID
	private final String        resultStatus;	// 결과 상태
	private final String        message;		// 요약 메시지
	private final String        metadataJson;	// 상세 메타데이터 (JSON 문자열)
	private final String        ipAddress;		// 요청 IP 주소
	private final String        userAgent;		// 요청 User-Agent
	private final LocalDateTime createdAt;		// 발생 일시

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 엔티티 → DTO 변환
	public static AuditLogResponseDto from(AuditLogEntity entity)
	{
		// 빌더 패턴으로 모든 필드를 엔티티에서 복사
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
