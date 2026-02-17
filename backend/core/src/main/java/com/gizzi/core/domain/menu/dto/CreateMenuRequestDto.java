package com.gizzi.core.domain.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// 메뉴 생성 요청 DTO
@Getter
public class CreateMenuRequestDto {

	// 메뉴 표시명 (필수)
	@NotBlank(message = "메뉴 이름은 필수입니다")
	@Size(max = 100, message = "메뉴 이름은 100자 이내여야 합니다")
	private String name;

	// 아이콘 식별자 (선택)
	@Size(max = 50, message = "아이콘 식별자는 50자 이내여야 합니다")
	private String icon;

	// 메뉴 유형 (MODULE / LINK / SEPARATOR)
	@NotBlank(message = "메뉴 유형은 필수입니다")
	private String menuType;

	// 부모 메뉴 ID (NULL이면 최상위)
	private String parentId;

	// 모듈 인스턴스 ID (MODULE 타입 필수)
	private String moduleInstanceId;

	// 커스텀 URL (LINK 타입 사용)
	@Size(max = 500, message = "URL은 500자 이내여야 합니다")
	private String customUrl;

	// LINK 타입 가시성 제어용 역할
	@Size(max = 50, message = "역할 코드는 50자 이내여야 합니다")
	private String requiredRole;

	// 정렬 순서
	private Integer sortOrder;
}
