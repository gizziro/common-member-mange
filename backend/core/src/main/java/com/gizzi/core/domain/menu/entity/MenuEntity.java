package com.gizzi.core.domain.menu.entity;

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

// 메뉴 엔티티 (tb_menus 테이블 매핑)
// 트리 구조의 네비게이션 메뉴를 관리한다
// parent_id로 부모-자식 관계를 형성하여 다단계 메뉴를 지원한다
@Entity
@Table(name = "tb_menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuEntity extends BaseEntity {

	// 메뉴 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 부모 메뉴 FK (NULL이면 최상위)
	@Column(name = "parent_id", length = 36)
	private String parentId;

	// 메뉴 표시명
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// 아이콘 식별자 (예: "megaphone", "file-text")
	@Column(name = "icon", length = 50)
	private String icon;

	// 메뉴 유형 (MODULE / LINK / SEPARATOR)
	@Enumerated(EnumType.STRING)
	@Column(name = "menu_type", nullable = false, length = 20)
	private MenuType menuType;

	// 연결된 모듈 인스턴스 FK (MODULE 타입에서만 사용)
	@Column(name = "module_instance_id", length = 50)
	private String moduleInstanceId;

	// 커스텀 링크 URL (LINK 타입에서만 사용)
	@Column(name = "custom_url", length = 500)
	private String customUrl;

	// LINK 타입 가시성 제어용 역할 (NULL이면 전체 공개)
	@Column(name = "required_role", length = 50)
	private String requiredRole;

	// 정렬 순서 (같은 부모 내에서의 순서)
	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	// 관리자 수동 노출 제어 (false면 숨김)
	@Column(name = "is_visible", nullable = false)
	private Boolean isVisible;

	// 엔티티 저장 전 UUID PK 자동 생성
	@PrePersist
	private void generateId() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}

	// 메뉴 생성 팩토리 메서드
	public static MenuEntity create(String name, String icon, MenuType menuType,
	                                 String moduleInstanceId, String customUrl,
	                                 String requiredRole, String parentId,
	                                 Integer sortOrder) {
		MenuEntity entity = new MenuEntity();
		entity.name             = name;
		entity.icon             = icon;
		entity.menuType         = menuType;
		entity.moduleInstanceId = moduleInstanceId;
		entity.customUrl        = customUrl;
		entity.requiredRole     = requiredRole;
		entity.parentId         = parentId;
		entity.sortOrder        = sortOrder != null ? sortOrder : 0;
		entity.isVisible        = true;
		return entity;
	}

	// 메뉴 정보 수정
	public void updateInfo(String name, String icon, MenuType menuType,
	                        String moduleInstanceId, String customUrl,
	                        String requiredRole) {
		this.name             = name;
		this.icon             = icon;
		this.menuType         = menuType;
		this.moduleInstanceId = moduleInstanceId;
		this.customUrl        = customUrl;
		this.requiredRole     = requiredRole;
	}

	// 정렬 순서 및 부모 변경
	public void updateOrder(Integer sortOrder, String parentId) {
		this.sortOrder = sortOrder;
		this.parentId  = parentId;
	}

	// 가시성 토글
	public void toggleVisibility() {
		this.isVisible = !this.isVisible;
	}
}
