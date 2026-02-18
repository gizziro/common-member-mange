package com.gizzi.module.board.service;

import com.gizzi.core.domain.group.repository.GroupMemberRepository;
import com.gizzi.core.module.PermissionChecker;
import com.gizzi.module.board.entity.BoardSettingsEntity;
import com.gizzi.module.board.repository.BoardAdminRepository;
import com.gizzi.module.board.repository.BoardSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 게시판 전용 권한 체크 헬퍼
// core의 PermissionChecker를 감싸며 게시판 특화 로직(부관리자, 익명 접근 등)을 추가한다
//
// 권한 우선순위:
//   1. administrator 그룹 멤버 → 전체 권한
//   2. 게시판 부관리자 (tb_board_admins) → 해당 게시판 전체 권한
//   3. 인스턴스 소유자 (tb_module_instances.owner_id) → 해당 게시판 전체 권한
//   4. 개별 사용자 권한 (tb_user_module_permissions)
//   5. 소속 그룹 권한 (tb_group_module_permissions)
//   6. 게시판 기본 설정 (allow_anonymous_access 등)
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPermissionHelper {

	// administrator 그룹 ID 상수 (시드 데이터)
	private static final String ADMIN_GROUP_ID = "administrator";

	// core 권한 체커
	private final PermissionChecker        permissionChecker;

	// 게시판 부관리자 리포지토리
	private final BoardAdminRepository     boardAdminRepository;

	// 게시판 설정 리포지토리
	private final BoardSettingsRepository  boardSettingsRepository;

	// 그룹 멤버 리포지토리 (administrator 그룹 확인용)
	private final GroupMemberRepository    groupMemberRepository;

	// ─── 시스템 관리자 확인 ───

	// administrator 그룹 소속 여부 확인
	public boolean isAdministrator(String userId) {
		if (userId == null) {
			return false;
		}
		return groupMemberRepository.existsByGroupIdAndUserId(ADMIN_GROUP_ID, userId);
	}

	// ─── 게시판 관리자 확인 ───

	// 게시판 부관리자 여부 확인 (tb_board_admins)
	public boolean isBoardAdmin(String userId, String boardInstanceId) {
		if (userId == null) {
			return false;
		}
		return boardAdminRepository.existsByBoardInstanceIdAndUserId(boardInstanceId, userId);
	}

	// 게시판에 대한 전체 관리 권한 보유 여부
	// administrator 그룹 또는 게시판 부관리자이면 true
	public boolean hasAdminAccess(String userId, String boardInstanceId) {
		if (userId == null) {
			return false;
		}
		// 시스템 관리자 확인
		if (isAdministrator(userId)) {
			return true;
		}
		// 게시판 부관리자 확인
		return isBoardAdmin(userId, boardInstanceId);
	}

	// ─── 게시판 접근 권한 ───

	// 게시판 접근 가능 여부 확인
	// 비로그인 사용자는 allow_anonymous_access 설정에 따라 접근 결정
	public boolean canAccessBoard(String userId, String boardInstanceId) {
		// 관리자는 항상 접근 가능
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}

		// 비로그인 사용자 — 익명 접근 허용 여부 확인
		if (userId == null) {
			return isAnonymousAccessAllowed(boardInstanceId);
		}

		// 로그인 사용자 — 권한이 설정되지 않은 게시판은 전체 공개
		if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
			return true;
		}

		// BOARD_BOARD_ACCESS 권한 체크
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_BOARD_ACCESS");
	}

	// ─── 게시글 권한 ───

	// 게시글 읽기 권한
	public boolean canReadPost(String userId, String boardInstanceId) {
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		// 익명 접근 허용 시 비로그인도 읽기 가능
		if (userId == null) {
			return isAnonymousAccessAllowed(boardInstanceId);
		}
		// 권한 미설정 게시판은 전체 공개
		if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
			return true;
		}
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_POST_READ");
	}

	// 게시글 작성 권한
	public boolean canWritePost(String userId, String boardInstanceId) {
		if (userId == null) {
			return false;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		// 권한 미설정 게시판은 로그인 사용자 전체 허용
		if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
			return true;
		}
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_POST_WRITE");
	}

	// 게시글 수정 권한 (본인 글 또는 타인 글 수정 권한)
	public boolean canEditPost(String userId, String boardInstanceId, String authorId) {
		if (userId == null) {
			return false;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		// 본인 글 수정
		if (userId.equals(authorId)) {
			if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
				return true;
			}
			return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_POST_EDIT_OWN");
		}
		// 타인 글 수정
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_POST_EDIT_OTHERS");
	}

	// 게시글 삭제 권한 (본인 글 또는 타인 글 삭제 권한)
	public boolean canDeletePost(String userId, String boardInstanceId, String authorId) {
		if (userId == null) {
			return false;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		// 본인 글 삭제
		if (userId.equals(authorId)) {
			if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
				return true;
			}
			return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_POST_DELETE_OWN");
		}
		// 타인 글 삭제 (관리 권한 필요)
		return false;
	}

	// 비밀글 열람 권한 (작성자, 관리자만)
	public boolean canReadSecret(String userId, String boardInstanceId, String authorId) {
		if (userId == null) {
			return false;
		}
		// 작성자 본인은 항상 열람 가능
		if (userId.equals(authorId)) {
			return true;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_POST_SECRET_READ");
	}

	// ─── 댓글 권한 ───

	// 댓글 작성 권한
	public boolean canWriteComment(String userId, String boardInstanceId) {
		if (userId == null) {
			return false;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
			return true;
		}
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_COMMENT_WRITE");
	}

	// 댓글 수정 권한
	public boolean canEditComment(String userId, String boardInstanceId, String authorId) {
		if (userId == null) {
			return false;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		if (userId.equals(authorId)) {
			if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
				return true;
			}
			return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_COMMENT_EDIT_OWN");
		}
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_COMMENT_EDIT_OTHERS");
	}

	// 댓글 삭제 권한
	public boolean canDeleteComment(String userId, String boardInstanceId, String authorId) {
		if (userId == null) {
			return false;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		if (userId.equals(authorId)) {
			if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
				return true;
			}
			return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_COMMENT_DELETE_OWN");
		}
		return false;
	}

	// ─── 파일 권한 ───

	// 파일 업로드 권한
	public boolean canUploadFile(String userId, String boardInstanceId) {
		if (userId == null) {
			return false;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
			return true;
		}
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_FILE_UPLOAD");
	}

	// ─── 투표 권한 ───

	// 투표(추천/비추천) 권한
	public boolean canVote(String userId, String boardInstanceId) {
		if (userId == null) {
			return false;
		}
		if (hasAdminAccess(userId, boardInstanceId)) {
			return true;
		}
		if (!permissionChecker.hasAnyPermissionGranted(boardInstanceId)) {
			return true;
		}
		return permissionChecker.hasPermission(userId, boardInstanceId, "BOARD_POST_VOTE");
	}

	// ─── 내부 헬퍼 ───

	// 익명(비로그인) 접근 허용 여부 (게시판 설정 확인)
	private boolean isAnonymousAccessAllowed(String boardInstanceId) {
		return boardSettingsRepository.findById(boardInstanceId)
				.map(BoardSettingsEntity::getAllowAnonymousAccess)
				.orElse(false);
	}
}
