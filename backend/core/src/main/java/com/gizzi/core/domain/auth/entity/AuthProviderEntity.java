package com.gizzi.core.domain.auth.entity;

import com.gizzi.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 인증 제공자 엔티티 (tb_auth_providers 테이블 매핑)
// local, google, kakao, naver 등 OAuth2 제공자 정보를 관리한다
@Entity
@Table(name = "tb_auth_providers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthProviderEntity extends BaseEntity {

	// 제공자 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 제공자 코드 (local, google, kakao, naver)
	@Column(name = "code", nullable = false, length = 50)
	private String code;

	// 제공자 표시명
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// 사용 여부
	@Column(name = "is_enabled", nullable = false)
	private Boolean isEnabled;

	// OAuth2 Client ID
	@Column(name = "client_id", length = 255)
	private String clientId;

	// OAuth2 Client Secret
	@Column(name = "client_secret", length = 255)
	private String clientSecret;

	// 인가 코드 콜백 URL
	@Column(name = "redirect_uri", length = 500)
	private String redirectUri;

	// 인가 엔드포인트 URL
	@Column(name = "authorization_uri", length = 500)
	private String authorizationUri;

	// 토큰 교환 엔드포인트 URL
	@Column(name = "token_uri", length = 500)
	private String tokenUri;

	// 사용자 정보 조회 엔드포인트 URL
	@Column(name = "userinfo_uri", length = 500)
	private String userinfoUri;

	// 요청 Scope (공백 구분)
	@Column(name = "scope", length = 500)
	private String scope;

	// 로그인 버튼 아이콘 URL
	@Column(name = "icon_url", length = 500)
	private String iconUrl;

	// 로그인 버튼 표시 순서
	@Column(name = "display_order", nullable = false)
	private Integer displayOrder;

	// 엔티티 저장 전 UUID PK 자동 생성
	@PrePersist
	private void generateId() {
		// ID가 없을 때만 새로 생성
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}

	// 관리자에 의한 Provider 설정 수정
	public void update(String clientId, String clientSecret, String redirectUri,
	                   String scope, String iconUrl, Boolean isEnabled) {
		// Client ID 설정
		this.clientId     = clientId;
		// clientSecret이 null이면 기존 값 유지 (프론트에서 빈 문자열 → null 전환)
		if (clientSecret != null) {
			this.clientSecret = clientSecret;
		}
		// 콜백 URL 설정
		this.redirectUri  = redirectUri;
		// Scope 설정
		this.scope        = scope;
		// 아이콘 URL 설정
		this.iconUrl      = iconUrl;
		// 활성화 여부 설정
		this.isEnabled    = isEnabled;
	}
}
