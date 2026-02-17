package com.gizzi.core.module;

import com.gizzi.core.module.dto.ResourcePermissionDefinition;
import com.gizzi.core.module.dto.SettingDefinition;

import java.util.List;

// 기능 모듈이 구현해야 하는 핵심 인터페이스
// Spring Bean으로 등록된 ModuleDefinition 구현체를 앱 시작 시 자동 발견하여
// tb_modules 테이블에 메타데이터를 동기화한다 (ModuleRegistry 참고)
//
// 사용 예:
//   @Component
//   public class BoardModuleDefinition implements ModuleDefinition { ... }
public interface ModuleDefinition {

	// 모듈 코드 (영소문자, tb_modules.code와 일치)
	// 예: "board", "blog", "accounting"
	String getCode();

	// 모듈 표시명 (관리자 화면에 표시)
	// 예: "게시판", "블로그", "가계부"
	String getName();

	// URL 슬러그 (영소문자+숫자+하이픈, 프론트엔드 라우팅에 사용)
	// 예: "board", "blog", "accounting"
	String getSlug();

	// 모듈 설명 (관리자 화면에 표시)
	// 예: "게시글, 댓글 관리 기능을 제공합니다"
	String getDescription();

	// 모듈 유형 (SINGLE: 인스턴스 없음, MULTI: 인스턴스 생성 가능)
	ModuleType getType();

	// 모듈이 제공하는 리소스별 권한 액션 목록
	// 예: [{resource: "post", action: "read", name: "게시글 읽기"}, ...]
	List<ResourcePermissionDefinition> getPermissions();

	// 모듈 스키마에서 존재 여부를 확인할 대표 테이블명 목록
	// ModuleSchemaInitializer가 이 테이블의 존재 여부로 스키마 적용 여부를 판단한다
	// 예: ["tb_board_posts", "tb_board_comments"]
	default List<String> getRequiredTables() {
		return List.of();
	}

	// 모듈 기본 설정 정의 목록
	// SettingsRegistry가 앱 시작 시 이 목록을 기반으로 tb_settings에 기본 설정을 등록한다
	// DB에 이미 존재하면 스킵하여 관리자가 수정한 값을 보존한다
	// 예: [SettingDefinition("general", "default_content_type", "HTML", STRING, "기본 콘텐츠 타입", "...", 0)]
	default List<SettingDefinition> getDefaultSettings() {
		return List.of();
	}
}
