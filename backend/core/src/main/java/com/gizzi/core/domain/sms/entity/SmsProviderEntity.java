package com.gizzi.core.domain.sms.entity;

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

// SMS 발송 프로바이더 엔티티 (tb_sms_providers 테이블 매핑)
// SOLAPI, AWS SNS 등 SMS 발송 서비스 제공자 정보를 관리한다
@Entity
@Table(name = "tb_sms_providers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsProviderEntity extends BaseEntity {

	// 프로바이더 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 프로바이더 코드 (solapi, aws_sns)
	@Column(name = "code", nullable = false, length = 50)
	private String code;

	// 프로바이더 표시명
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// 사용 여부
	@Column(name = "is_enabled", nullable = false)
	private Boolean isEnabled;

	// API Key
	@Column(name = "api_key", length = 255)
	private String apiKey;

	// API Secret
	@Column(name = "api_secret", length = 255)
	private String apiSecret;

	// 발신 번호
	@Column(name = "sender_number", length = 20)
	private String senderNumber;

	// 추가 설정 JSON
	@Column(name = "config_json", columnDefinition = "TEXT")
	private String configJson;

	// 표시 순서
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

	// 관리자에 의한 프로바이더 설정 수정
	public void update(String apiKey, String apiSecret, String senderNumber,
	                   Boolean isEnabled, String configJson) {
		// API Key 설정
		this.apiKey       = apiKey;
		// apiSecret이 null이면 기존 값 유지 (프론트에서 빈 문자열 → null 전환)
		if (apiSecret != null) {
			this.apiSecret = apiSecret;
		}
		// 발신 번호 설정
		this.senderNumber = senderNumber;
		// 활성화 여부 설정
		this.isEnabled    = isEnabled;
		// 추가 설정 JSON
		this.configJson   = configJson;
	}
}
