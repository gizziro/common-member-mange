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
public class MenuEntity extends BaseEntity
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ PK ]
	//----------------------------------------------------------------------------------------------------------------------
	@Id
	@Column(name = "id", length = 36)
	private String id;								// 메뉴 PK (UUID)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 메뉴 기본 정보 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Column(name = "parent_id", length = 36)
	private String parentId;						// 부모 메뉴 FK (NULL이면 최상위)

	@Column(name = "name", nullable = false, length = 100)
	private String name;							// 메뉴 표시명

	@Column(name = "icon", length = 50)
	private String icon;							// 아이콘 식별자 (예: "megaphone", "file-text")

	@Enumerated(EnumType.STRING)
	@Column(name = "menu_type", nullable = false, length = 20)
	private MenuType menuType;						// 메뉴 유형 (MODULE / LINK / SEPARATOR)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 모듈 연결 정보 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Column(name = "module_instance_id", length = 50)
	private String moduleInstanceId;				// 연결된 모듈 인스턴스 FK (MODULE 타입에서만 사용)

	@Column(name = "custom_url", length = 500)
	private String customUrl;						// 커스텀 링크 URL (LINK 타입에서만 사용)

	@Column(name = "alias_path", length = 100)
	private String aliasPath;						// 외부 단축 경로 (예: "test", "free")

	@Column(name = "content_path", length = 200)
	private String contentPath;						// SINGLE 모듈 콘텐츠 경로 (예: "test" → /page/test)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 표시 제어 ]
	//----------------------------------------------------------------------------------------------------------------------
	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;						// 정렬 순서 (같은 부모 내에서의 순서)

	@Column(name = "is_visible", nullable = false)
	private Boolean isVisible;						// 관리자 수동 노출 제어 (false면 숨김)

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
	// 메뉴 생성 팩토리 메서드
	//----------------------------------------------------------------------------------------------------------------------
	public static MenuEntity create(String name, String icon, MenuType menuType,
	                                 String moduleInstanceId, String customUrl,
	                                 String aliasPath, String contentPath,
	                                 String parentId, Integer sortOrder)
	{
		// 새 메뉴 엔티티 생성 및 필드 설정
		MenuEntity entity		= new MenuEntity();
		entity.name				= name;
		entity.icon				= icon;
		entity.menuType			= menuType;
		entity.moduleInstanceId	= moduleInstanceId;
		entity.customUrl		= customUrl;
		entity.aliasPath		= aliasPath;
		entity.contentPath		= contentPath;
		entity.parentId			= parentId;
		entity.sortOrder		= sortOrder != null ? sortOrder : 0;
		entity.isVisible		= true;
		return entity;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 메뉴 정보 수정
	//----------------------------------------------------------------------------------------------------------------------
	public void updateInfo(String name, String icon, MenuType menuType,
	                        String moduleInstanceId, String customUrl,
	                        String aliasPath, String contentPath)
	{
		// 메뉴 기본 정보 및 모듈 연결 정보 갱신
		this.name				= name;
		this.icon				= icon;
		this.menuType			= menuType;
		this.moduleInstanceId	= moduleInstanceId;
		this.customUrl			= customUrl;
		this.aliasPath			= aliasPath;
		this.contentPath		= contentPath;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 정렬 순서 및 부모 변경
	//----------------------------------------------------------------------------------------------------------------------
	public void updateOrder(Integer sortOrder, String parentId)
	{
		// 정렬 순서와 부모 메뉴 ID 갱신
		this.sortOrder	= sortOrder;
		this.parentId	= parentId;
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 가시성 토글
	//----------------------------------------------------------------------------------------------------------------------
	public void toggleVisibility()
	{
		// 현재 가시성 상태를 반전
		this.isVisible = !this.isVisible;
	}
}
