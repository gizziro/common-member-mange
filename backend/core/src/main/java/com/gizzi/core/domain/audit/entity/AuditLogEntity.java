package com.gizzi.core.domain.audit.entity;

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

// 감사 로그 엔티티 — tb_audit_logs 테이블 매핑
// BaseEntity를 상속하지 않음 (updated_at 컬럼 없음, created_at만 존재)
@Entity
@Table(name = "tb_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntity
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ PK ]
	//----------------------------------------------------------------------------------------------------------------------

	// 감사 로그 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 행위자 정보 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 행위자 사용자 PK (null 가능 — SYSTEM 행위자)
	@Column(name = "actor_user_id", length = 36)
	private String actorUserId;

	// 행위자 유형 (USER/SYSTEM)
	@Column(name = "actor_type", nullable = false, length = 20)
	private String actorType;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 액션 정보 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 액션 유형 (LOGIN, LOGOUT, USER_SIGNUP 등)
	@Column(name = "action_type", nullable = false, length = 50)
	private String actionType;

	// 대상 유형 (USER, GROUP, SETTING 등)
	@Column(name = "target_type", length = 50)
	private String targetType;

	// 대상 ID (대상 엔티티의 PK)
	@Column(name = "target_id", length = 36)
	private String targetId;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 결과 정보 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 결과 상태 (SUCCESS/FAILURE)
	@Column(name = "result_status", nullable = false, length = 20)
	private String resultStatus;

	// 요약 메시지
	@Column(name = "message", length = 500)
	private String message;

	// 상세 메타데이터 (JSON 문자열)
	@Column(name = "metadata_json", columnDefinition = "JSON")
	private String metadataJson;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 요청 컨텍스트 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 요청 IP 주소
	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	// 요청 User-Agent
	@Column(name = "user_agent", length = 255)
	private String userAgent;

	//----------------------------------------------------------------------------------------------------------------------
	// [ 타임스탬프 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 발생 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	//----------------------------------------------------------------------------------------------------------------------
	// [ JPA 콜백 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 영속화 직전 UUID + 생성 일시 자동 설정
	@PrePersist
	protected void onCreate()
	{
		// PK가 없으면 UUID 자동 생성
		if (this.id == null)
		{
			this.id = UUID.randomUUID().toString();
		}
		// 생성 일시가 없으면 현재 시간 설정
		if (this.createdAt == null)
		{
			this.createdAt = LocalDateTime.now();
		}
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 정적 팩토리 메서드 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 감사 로그 생성 정적 팩토리 메서드
	public static AuditLogEntity create(String actorUserId, String actorType,
	                                    String actionType, String targetType,
	                                    String targetId, String resultStatus,
	                                    String message, String metadataJson,
	                                    String ipAddress, String userAgent)
	{
		// 새 엔티티 생성 후 각 필드 직접 할당
		AuditLogEntity entity	= new AuditLogEntity();
		entity.actorUserId		= actorUserId;
		entity.actorType		= actorType;
		entity.actionType		= actionType;
		entity.targetType		= targetType;
		entity.targetId			= targetId;
		entity.resultStatus		= resultStatus;
		entity.message			= message;
		entity.metadataJson		= metadataJson;
		entity.ipAddress		= ipAddress;
		entity.userAgent		= userAgent;
		return entity;
	}
}
