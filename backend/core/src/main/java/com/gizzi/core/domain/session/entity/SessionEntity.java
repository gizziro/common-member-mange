package com.gizzi.core.domain.session.entity;

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

// 세션 엔티티 (tb_sessions 테이블 매핑)
// 사용자 로그인 시 발급된 토큰 정보와 접속 메타데이터를 감사 기록으로 저장한다
@Entity
@Table(name = "tb_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionEntity
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ PK ]
	//----------------------------------------------------------------------------------------------------------------------
	@Id
	@Column(name = "id", length = 36)
	private String id;								// 세션 PK (UUID)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 사용자 및 인증 정보 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Column(name = "user_id", nullable = false, length = 36)
	private String userId;							// 사용자 FK (tb_users.id)

	@Column(name = "login_provider", nullable = false, length = 20)
	private String loginProvider;					// 로그인 제공자 (LOCAL, GOOGLE 등)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 토큰 해시 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Column(name = "access_token_hash", nullable = false, length = 255)
	private String accessTokenHash;					// Access Token 해시값 (SHA-256)

	@Column(name = "refresh_token_hash", nullable = false, length = 255)
	private String refreshTokenHash;				// Refresh Token 해시값 (SHA-256)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 토큰 만료 일시 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Column(name = "access_expires_at", nullable = false)
	private LocalDateTime accessExpiresAt;			// Access Token 만료 일시

	@Column(name = "refresh_expires_at", nullable = false)
	private LocalDateTime refreshExpiresAt;			// Refresh Token 만료 일시

	//----------------------------------------------------------------------------------------------------------------------
	// [ 접속 메타데이터 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Column(name = "ip_address", length = 45)
	private String ipAddress;						// 클라이언트 IP 주소

	@Column(name = "user_agent", length = 255)
	private String userAgent;						// 클라이언트 User-Agent

	@Column(name = "device_id", length = 100)
	private String deviceId;						// 디바이스 식별자

	@Column(name = "device_name", length = 100)
	private String deviceName;						// 디바이스 이름

	//----------------------------------------------------------------------------------------------------------------------
	// [ 세션 상태 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;					// 토큰 발급 일시

	@Column(name = "last_seen_at")
	private LocalDateTime lastSeenAt;				// 최근 접속 일시

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;				// 토큰 폐기 일시 (NULL이면 활성 세션)

	@Column(name = "revoked_reason", length = 100)
	private String revokedReason;					// 토큰 폐기 사유

	//----------------------------------------------------------------------------------------------------------------------
	// 엔티티 저장 전 UUID PK 자동 생성
	//----------------------------------------------------------------------------------------------------------------------
	@PrePersist
	private void generateId()
	{
		// ID가 없을 때만 새로 생성
		if (this.id == null)
		{
			this.id = UUID.randomUUID().toString();
		}
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 새 세션 생성 팩토리 메서드
	//----------------------------------------------------------------------------------------------------------------------
	public static SessionEntity create(String userId, String loginProvider,
	                                    String accessTokenHash, String refreshTokenHash,
	                                    LocalDateTime accessExpiresAt, LocalDateTime refreshExpiresAt,
	                                    String ipAddress, String userAgent)
	{
		// 세션 엔티티 초기화
		SessionEntity session		= new SessionEntity();
		session.userId				= userId;
		session.loginProvider		= loginProvider;
		session.accessTokenHash		= accessTokenHash;
		session.refreshTokenHash	= refreshTokenHash;
		session.accessExpiresAt		= accessExpiresAt;
		session.refreshExpiresAt	= refreshExpiresAt;
		session.ipAddress			= ipAddress;
		session.userAgent			= userAgent;
		session.issuedAt			= LocalDateTime.now();
		return session;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 세션 폐기 (로그아웃 / 토큰 갱신 시 기존 세션 무효화)
	//----------------------------------------------------------------------------------------------------------------------
	public void revoke(String reason)
	{
		// 폐기 시각과 사유를 기록
		this.revokedAt		= LocalDateTime.now();
		this.revokedReason	= reason;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 토큰 갱신 시 해시값과 만료 일시 업데이트
	//----------------------------------------------------------------------------------------------------------------------
	public void updateTokens(String accessTokenHash, String refreshTokenHash,
	                          LocalDateTime accessExpiresAt, LocalDateTime refreshExpiresAt)
	{
		// 새로 발급된 토큰의 해시값과 만료 일시로 갱신
		this.accessTokenHash	= accessTokenHash;
		this.refreshTokenHash	= refreshTokenHash;
		this.accessExpiresAt	= accessExpiresAt;
		this.refreshExpiresAt	= refreshExpiresAt;
	}
}
