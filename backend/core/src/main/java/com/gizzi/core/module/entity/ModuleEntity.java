package com.gizzi.core.module.entity;

import com.gizzi.core.common.entity.BaseEntity;
import com.gizzi.core.module.ModuleType;
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

// 모듈 엔티티 (tb_modules 테이블 매핑)
// 시스템에 등록된 기능 모듈의 메타데이터를 저장한다
// ModuleRegistry에 의해 앱 시작 시 자동으로 INSERT/UPDATE된다
@Entity
@Table(name = "tb_modules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModuleEntity extends BaseEntity {

	// 모듈 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 모듈 코드 (영소문자, 프로그래밍 식별자)
	@Column(name = "code", nullable = false, length = 50)
	private String code;

	// 모듈 표시명
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// URL 슬러그 (영소문자+숫자+하이픈, 프론트엔드 라우팅에 사용)
	@Column(name = "slug", nullable = false, length = 50)
	private String slug;

	// 모듈 설명
	@Column(name = "description", length = 500)
	private String description;

	// 모듈 유형 (SINGLE / MULTI)
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private ModuleType type;

	// 활성화 여부
	@Column(name = "is_enabled", nullable = false)
	private Boolean isEnabled;

	// 엔티티 저장 전 UUID PK 자동 생성
	@PrePersist
	private void generateId() {
		// ID가 없을 때만 새로 생성 (수동 설정된 경우 유지)
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}

	// 모듈 생성 팩토리 메서드 (ModuleRegistry에서 사용)
	public static ModuleEntity create(String code, String name, String slug,
	                                  String description, ModuleType type) {
		ModuleEntity entity = new ModuleEntity();
		entity.code        = code;
		entity.name        = name;
		entity.slug        = slug;
		entity.description = description;
		entity.type        = type;
		entity.isEnabled   = true;
		return entity;
	}

	// 모듈 메타데이터 갱신 (ModuleDefinition에서 변경된 정보 동기화)
	public void updateMetadata(String name, String slug, String description, ModuleType type) {
		this.name        = name;
		this.slug        = slug;
		this.description = description;
		this.type        = type;
	}

	// 모듈 활성화/비활성화 토글
	public void setEnabled(boolean enabled) {
		this.isEnabled = enabled;
	}
}
