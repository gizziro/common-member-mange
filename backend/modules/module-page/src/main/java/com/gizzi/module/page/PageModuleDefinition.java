package com.gizzi.module.page;

import com.gizzi.core.module.ModuleDefinition;
import com.gizzi.core.module.ModuleType;
import com.gizzi.core.module.dto.ResourcePermissionDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

// 페이지 모듈 정의 — SINGLE 타입
// 시스템 전체에서 하나의 "페이지 관리" 모듈로 운영된다
// 내부적으로 여러 페이지(tb_page_pages)를 관리하며
// URL 패턴: /page/{page-slug} (예: /page/about, /page/terms)
@Component
public class PageModuleDefinition implements ModuleDefinition {

	@Override
	public String getCode() {
		return "page";
	}

	@Override
	public String getName() {
		return "페이지";
	}

	@Override
	public String getSlug() {
		return "page";
	}

	@Override
	public String getDescription() {
		return "텍스트/HTML 페이지 관리";
	}

	@Override
	public ModuleType getType() {
		return ModuleType.SINGLE;
	}

	// 페이지 모듈 권한 정의
	// 런타임 권한 문자열: PAGE_PAGE_READ, PAGE_PAGE_WRITE, PAGE_PAGE_DELETE
	@Override
	public List<ResourcePermissionDefinition> getPermissions() {
		return List.of(
				new ResourcePermissionDefinition("page", "read",   "페이지 읽기"),
				new ResourcePermissionDefinition("page", "write",  "페이지 작성/수정"),
				new ResourcePermissionDefinition("page", "delete", "페이지 삭제")
		);
	}

	// 스키마 존재 확인용 대표 테이블
	@Override
	public List<String> getRequiredTables() {
		return List.of("tb_page_pages");
	}
}
