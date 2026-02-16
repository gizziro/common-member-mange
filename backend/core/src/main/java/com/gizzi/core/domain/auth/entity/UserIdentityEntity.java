package com.gizzi.core.domain.auth.entity;

import com.gizzi.core.domain.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 소셜 로그인 연동 엔티티 (tb_user_identities 테이블 매핑)
// 사용자와 소셜 제공자 간의 연결 정보를 관리한다
@Entity
@Table(name = "tb_user_identities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdentityEntity {

	// 식별자 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 사용자 FK (지연 로딩)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	// 인증 제공자 FK (지연 로딩)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "provider_id", nullable = false)
	private AuthProviderEntity provider;

	// 제공자 측 사용자 고유 키 (sub, id 등)
	@Column(name = "provider_subject", nullable = false, length = 255)
	private String providerSubject;

	// 제공자 Access Token (갱신용)
	@Column(name = "access_token", length = 1024)
	private String accessToken;

	// 제공자 Refresh Token (갱신용)
	@Column(name = "refresh_token", length = 1024)
	private String refreshToken;

	// 토큰 만료 일시
	@Column(name = "token_expires_at")
	private LocalDateTime tokenExpiresAt;

	// 생성 일시
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 엔티티 저장 전 UUID PK 자동 생성 + 생성 일시 설정
	@PrePersist
	private void prePersist() {
		// ID가 없을 때만 새로 생성
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
		// 생성 일시 자동 설정
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}

	// 소셜 연동 생성 팩토리 메서드
	public static UserIdentityEntity create(UserEntity user, AuthProviderEntity provider,
	                                        String providerSubject) {
		// 새 소셜 연동 엔티티 생성
		UserIdentityEntity identity = new UserIdentityEntity();
		identity.user               = user;
		identity.provider           = provider;
		identity.providerSubject    = providerSubject;
		return identity;
	}
}
