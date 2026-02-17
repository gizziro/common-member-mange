package com.gizzi.core.domain.setting.entity;

import com.gizzi.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 통합 설정 엔티티 — 시스템 설정과 모듈 설정을 하나의 테이블(tb_settings)로 관리
// module_code='system'이면 전역 설정, 그 외는 해당 모듈의 설정
// (module_code, setting_group, setting_key)의 복합 유니크로 3/4단 계층 지원
@Entity
@Table(name = "tb_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettingEntity extends BaseEntity {

	// 설정 PK (UUID, DB에서 자동 생성)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 모듈 코드 ('system' = 전역 설정, 그 외 = 모듈 코드)
	@Column(name = "module_code", nullable = false, length = 50)
	private String moduleCode;

	// 설정 그룹 ('general' = 기본 그룹, 'signup', 'auth', 'session' 등)
	@Column(name = "setting_group", nullable = false, length = 50)
	private String settingGroup;

	// 설정 키 (예: 'site_name', 'enabled', 'max_login_fail')
	@Column(name = "setting_key", nullable = false, length = 100)
	private String settingKey;

	// 설정 값 (문자열로 저장, valueType에 따라 클라이언트에서 파싱)
	@Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
	private String settingValue;

	// 값 타입 (STRING/NUMBER/BOOLEAN/JSON — 관리자 UI 렌더링 용도)
	@Enumerated(EnumType.STRING)
	@Column(name = "value_type", nullable = false, length = 20)
	private SettingValueType valueType;

	// 표시명 (관리자 UI에 표시되는 한국어 이름)
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// 설명 (관리자 UI 툴팁/부가 설명)
	@Column(name = "description", length = 500)
	private String description;

	// 읽기 전용 여부 (true면 관리자 UI에서 수정 불가, 코드에서만 변경)
	@Column(name = "is_readonly", nullable = false)
	private boolean readonly;

	// 그룹 내 정렬 순서
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	// 최종 수정자 (관리자 ID 또는 'SYSTEM')
	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	// 정적 팩토리: 새 설정 생성
	public static SettingEntity create(
			String moduleCode,
			String settingGroup,
			String settingKey,
			String settingValue,
			SettingValueType valueType,
			String name,
			String description,
			boolean readonly,
			int sortOrder
	) {
		SettingEntity entity  = new SettingEntity();
		entity.moduleCode     = moduleCode;
		entity.settingGroup   = settingGroup;
		entity.settingKey     = settingKey;
		entity.settingValue   = settingValue;
		entity.valueType      = valueType;
		entity.name           = name;
		entity.description    = description;
		entity.readonly       = readonly;
		entity.sortOrder      = sortOrder;
		entity.updatedBy      = "SYSTEM";
		return entity;
	}

	// 엔티티 저장 전 UUID PK 자동 생성
	@PrePersist
	protected void onCreate() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}

	// 설정 값 변경 (비즈니스 메서드)
	public void updateValue(String newValue, String updatedBy) {
		this.settingValue = newValue;
		this.updatedBy    = updatedBy;
	}
}
