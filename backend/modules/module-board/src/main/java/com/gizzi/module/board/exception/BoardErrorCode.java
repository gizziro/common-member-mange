package com.gizzi.module.board.exception;

import com.gizzi.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 게시판 모듈 관련 에러 코드 (BOARD_*)
@Getter
@AllArgsConstructor
public enum BoardErrorCode implements ErrorCode {

	// 게시판 조회 실패
	BOARD_NOT_FOUND              ("BOARD_NOT_FOUND",              "게시판을 찾을 수 없습니다",              "게시판 인스턴스 ID로 조회 실패",                  HttpStatus.NOT_FOUND),

	// 게시판 접근 권한 없음
	BOARD_ACCESS_DENIED          ("BOARD_ACCESS_DENIED",          "게시판 접근 권한이 없습니다",            "게시판 인스턴스에 대한 접근 권한 부족",              HttpStatus.FORBIDDEN),

	// 게시글 조회 실패
	BOARD_POST_NOT_FOUND         ("BOARD_POST_NOT_FOUND",         "게시글을 찾을 수 없습니다",             "게시글 ID로 조회 실패 또는 삭제된 게시글",            HttpStatus.NOT_FOUND),

	// 게시글 수정 권한 없음
	BOARD_POST_EDIT_DENIED       ("BOARD_POST_EDIT_DENIED",       "게시글 수정 권한이 없습니다",            "본인 작성 게시글이 아니거나 수정 권한 부족",           HttpStatus.FORBIDDEN),

	// 게시글 삭제 권한 없음
	BOARD_POST_DELETE_DENIED     ("BOARD_POST_DELETE_DENIED",     "게시글 삭제 권한이 없습니다",            "본인 작성 게시글이 아니거나 삭제 권한 부족",           HttpStatus.FORBIDDEN),

	// 비밀글 접근 권한 없음
	BOARD_SECRET_ACCESS_DENIED   ("BOARD_SECRET_ACCESS_DENIED",   "비밀글 열람 권한이 없습니다",            "비밀글 접근 시 작성자 또는 관리자만 열람 가능",         HttpStatus.FORBIDDEN),

	// 댓글 조회 실패
	BOARD_COMMENT_NOT_FOUND      ("BOARD_COMMENT_NOT_FOUND",      "댓글을 찾을 수 없습니다",              "댓글 ID로 조회 실패 또는 삭제된 댓글",              HttpStatus.NOT_FOUND),

	// 댓글 수정 권한 없음
	BOARD_COMMENT_EDIT_DENIED    ("BOARD_COMMENT_EDIT_DENIED",    "댓글 수정 권한이 없습니다",             "본인 작성 댓글이 아니거나 수정 권한 부족",            HttpStatus.FORBIDDEN),

	// 댓글 삭제 권한 없음
	BOARD_COMMENT_DELETE_DENIED  ("BOARD_COMMENT_DELETE_DENIED",  "댓글 삭제 권한이 없습니다",             "본인 작성 댓글이 아니거나 삭제 권한 부족",            HttpStatus.FORBIDDEN),

	// 카테고리 조회 실패
	BOARD_CATEGORY_NOT_FOUND     ("BOARD_CATEGORY_NOT_FOUND",     "카테고리를 찾을 수 없습니다",            "카테고리 ID로 조회 실패",                        HttpStatus.NOT_FOUND),

	// 카테고리 슬러그 중복
	BOARD_CATEGORY_DUPLICATE_SLUG("BOARD_CATEGORY_DUPLICATE_SLUG","이미 사용 중인 카테고리 슬러그입니다",     "동일 게시판 내 카테고리 slug 중복",                HttpStatus.CONFLICT),

	// 태그 조회 실패
	BOARD_TAG_NOT_FOUND          ("BOARD_TAG_NOT_FOUND",          "태그를 찾을 수 없습니다",               "태그 ID로 조회 실패",                           HttpStatus.NOT_FOUND),

	// 중복 투표
	BOARD_DUPLICATE_VOTE         ("BOARD_DUPLICATE_VOTE",         "이미 투표하셨습니다",                   "동일 대상에 대한 중복 투표 시도",                  HttpStatus.CONFLICT),

	// 답글 깊이 초과
	BOARD_MAX_REPLY_DEPTH        ("BOARD_MAX_REPLY_DEPTH",        "더 이상 답글을 작성할 수 없습니다",       "게시판 설정의 최대 답글 깊이 초과",                 HttpStatus.BAD_REQUEST),

	// 댓글 깊이 초과
	BOARD_MAX_COMMENT_DEPTH      ("BOARD_MAX_COMMENT_DEPTH",      "더 이상 대댓글을 작성할 수 없습니다",      "게시판 설정의 최대 댓글 깊이 초과",                 HttpStatus.BAD_REQUEST),

	// 파일 타입 불허
	BOARD_FILE_TYPE_NOT_ALLOWED  ("BOARD_FILE_TYPE_NOT_ALLOWED",  "허용되지 않은 파일 형식입니다",           "게시판 설정의 허용 파일 확장자에 미포함",              HttpStatus.BAD_REQUEST),

	// 파일 크기 초과
	BOARD_FILE_SIZE_EXCEEDED     ("BOARD_FILE_SIZE_EXCEEDED",     "파일 크기가 제한을 초과합니다",           "게시판 설정의 최대 파일 크기 초과",                 HttpStatus.BAD_REQUEST),

	// 파일 수 초과
	BOARD_FILE_COUNT_EXCEEDED    ("BOARD_FILE_COUNT_EXCEEDED",    "첨부 파일 수가 제한을 초과합니다",        "게시판 설정의 게시글당 최대 파일 수 초과",             HttpStatus.BAD_REQUEST),

	// 파일 조회 실패
	BOARD_FILE_NOT_FOUND         ("BOARD_FILE_NOT_FOUND",         "파일을 찾을 수 없습니다",               "파일 ID로 조회 실패",                           HttpStatus.NOT_FOUND),

	// 파일 업로드 불가
	BOARD_FILE_UPLOAD_DISABLED   ("BOARD_FILE_UPLOAD_DISABLED",   "파일 업로드가 비활성화되어 있습니다",      "게시판 설정에서 파일 업로드 비활성화",               HttpStatus.BAD_REQUEST),

	// 파일 저장 실패
	BOARD_FILE_STORAGE_ERROR     ("BOARD_FILE_STORAGE_ERROR",     "파일 저장 중 오류가 발생했습니다",        "로컬 파일 시스템 저장 실패",                      HttpStatus.INTERNAL_SERVER_ERROR),

	// 부관리자 이미 존재
	BOARD_ADMIN_ALREADY_EXISTS   ("BOARD_ADMIN_ALREADY_EXISTS",   "이미 등록된 부관리자입니다",             "동일 게시판에 동일 사용자 부관리자 중복 등록",          HttpStatus.CONFLICT),

	// 투표 기능 비활성화
	BOARD_VOTE_DISABLED          ("BOARD_VOTE_DISABLED",          "투표 기능이 비활성화되어 있습니다",        "게시판 설정에서 투표 기능 비활성화",                 HttpStatus.BAD_REQUEST),

	// 자기 게시글/댓글에 투표 불가
	BOARD_SELF_VOTE              ("BOARD_SELF_VOTE",              "본인의 글에는 투표할 수 없습니다",         "본인 작성 콘텐츠에 대한 투표 시도",                 HttpStatus.BAD_REQUEST);

	// 에러 코드 문자열
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
