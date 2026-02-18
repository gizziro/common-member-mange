package com.gizzi.module.board;

import com.gizzi.core.domain.setting.entity.SettingValueType;
import com.gizzi.core.module.ModuleDefinition;
import com.gizzi.core.module.ModuleType;
import com.gizzi.core.module.dto.ResourcePermissionDefinition;
import com.gizzi.core.module.dto.SettingDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

// 게시판 모듈 정의 — MULTI 타입
// 게시판 인스턴스(공지사항, 자유게시판 등)를 독립적으로 생성할 수 있으며
// 인스턴스별로 그룹/사용자 권한을 부여할 수 있다
// URL 패턴: /board/{board-slug} (예: /board/notice, /board/free)
@Component
public class BoardModuleDefinition implements ModuleDefinition {

	@Override
	public String getCode() {
		return "board";
	}

	@Override
	public String getName() {
		return "게시판";
	}

	@Override
	public String getSlug() {
		return "board";
	}

	@Override
	public String getDescription() {
		return "게시글, 댓글, 파일 첨부, 추천/비추천 기능을 제공하는 게시판 모듈";
	}

	@Override
	public ModuleType getType() {
		return ModuleType.MULTI;
	}

	// 게시판 모듈 권한 정의 (17개)
	// 런타임 권한 문자열: BOARD_{RESOURCE}_{ACTION}
	@Override
	public List<ResourcePermissionDefinition> getPermissions() {
		return List.of(
				// 게시판 접근 권한
				new ResourcePermissionDefinition("board",   "access",       "게시판 접근"),

				// 게시글 관련 권한
				new ResourcePermissionDefinition("post",    "read",         "게시글 읽기"),
				new ResourcePermissionDefinition("post",    "write",        "게시글 작성"),
				new ResourcePermissionDefinition("post",    "edit_own",     "본인 게시글 수정"),
				new ResourcePermissionDefinition("post",    "delete_own",   "본인 게시글 삭제"),
				new ResourcePermissionDefinition("post",    "edit_others",  "타인 게시글 수정"),
				new ResourcePermissionDefinition("post",    "secret_write", "비밀글 작성"),
				new ResourcePermissionDefinition("post",    "secret_read",  "비밀글 열람"),
				new ResourcePermissionDefinition("post",    "vote",         "게시글 추천/비추천"),
				new ResourcePermissionDefinition("post",    "report",       "게시글 신고"),

				// 파일 관련 권한
				new ResourcePermissionDefinition("file",    "upload",       "파일 업로드"),

				// 댓글 관련 권한
				new ResourcePermissionDefinition("comment", "read",         "댓글 읽기"),
				new ResourcePermissionDefinition("comment", "write",        "댓글 작성"),
				new ResourcePermissionDefinition("comment", "edit_own",     "본인 댓글 수정"),
				new ResourcePermissionDefinition("comment", "delete_own",   "본인 댓글 삭제"),
				new ResourcePermissionDefinition("comment", "edit_others",  "타인 댓글 수정"),

				// 검색 권한
				new ResourcePermissionDefinition("board",   "search",       "게시판 검색")
		);
	}

	// 스키마 존재 확인용 대표 테이블
	@Override
	public List<String> getRequiredTables() {
		return List.of("tb_board_posts");
	}

	// 게시판 모듈 기본 설정 정의
	@Override
	public List<SettingDefinition> getDefaultSettings() {
		return List.of(
				new SettingDefinition("general", "default_editor_type", "MARKDOWN",
						SettingValueType.STRING, "기본 에디터 타입",
						"새 게시판 생성 시 기본 에디터 타입 (PLAIN_TEXT/MARKDOWN)", 0),
				new SettingDefinition("general", "default_posts_per_page", "20",
						SettingValueType.NUMBER, "기본 페이지당 게시글 수",
						"새 게시판 생성 시 기본 페이지당 게시글 수", 1)
		);
	}
}
