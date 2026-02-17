package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 메뉴 관리 관련 에러 코드 (MENU_*)
// 메뉴 CRUD, 트리 구조 관리에서 발생하는 비즈니스 에러
@Getter
@AllArgsConstructor
public enum MenuErrorCode implements ErrorCode {

	// 메뉴 조회 실패
	MENU_NOT_FOUND                  ("MENU_NOT_FOUND",                  "메뉴를 찾을 수 없습니다",                   "메뉴 ID로 조회 실패",                       HttpStatus.NOT_FOUND),

	// 상위 메뉴 조회 실패
	MENU_PARENT_NOT_FOUND           ("MENU_PARENT_NOT_FOUND",           "상위 메뉴를 찾을 수 없습니다",              "parent_id로 지정된 상위 메뉴 없음",           HttpStatus.NOT_FOUND),

	// 순환 참조 감지 (자기 자신을 부모로 지정 등)
	MENU_CIRCULAR_REFERENCE         ("MENU_CIRCULAR_REFERENCE",         "순환 참조가 감지되었습니다",                "메뉴 트리에서 순환 참조 발생",                HttpStatus.BAD_REQUEST),

	// 잘못된 메뉴 유형
	MENU_INVALID_TYPE               ("MENU_INVALID_TYPE",               "올바르지 않은 메뉴 유형입니다",              "menuType 값이 유효하지 않음",                HttpStatus.BAD_REQUEST),

	// MODULE 타입인데 인스턴스 미지정
	MENU_MODULE_INSTANCE_REQUIRED   ("MENU_MODULE_INSTANCE_REQUIRED",   "MODULE 타입은 모듈 인스턴스 연결이 필요합니다", "MODULE 타입 메뉴에 instanceId 미지정",        HttpStatus.BAD_REQUEST),

	// 최대 메뉴 깊이 초과
	MENU_MAX_DEPTH_EXCEEDED         ("MENU_MAX_DEPTH_EXCEEDED",         "최대 메뉴 깊이를 초과했습니다",              "3단계 이상의 메뉴 중첩 시도",                HttpStatus.BAD_REQUEST),

	// 단축 경로 중복
	MENU_ALIAS_DUPLICATE            ("MENU_ALIAS_DUPLICATE",            "이미 사용 중인 단축 경로입니다",             "alias_path 중복",                          HttpStatus.CONFLICT),

	// 단축 경로가 모듈 slug와 충돌
	MENU_ALIAS_CONFLICTS_MODULE     ("MENU_ALIAS_CONFLICTS_MODULE",     "모듈 slug와 충돌하는 단축 경로입니다",        "alias_path가 기존 모듈 slug와 동일",         HttpStatus.BAD_REQUEST),

	// 단축 경로가 예약어와 충돌
	MENU_ALIAS_RESERVED             ("MENU_ALIAS_RESERVED",             "예약된 경로로는 단축 경로를 설정할 수 없습니다",  "alias_path가 시스템 예약어와 동일",           HttpStatus.BAD_REQUEST),

	// 단축 경로 형식 오류
	MENU_ALIAS_INVALID_FORMAT       ("MENU_ALIAS_INVALID_FORMAT",       "단축 경로 형식이 올바르지 않습니다",           "소문자+숫자+하이픈만 허용 (1자 이상)",        HttpStatus.BAD_REQUEST);

	// 에러 코드 문자열 (예: "MENU_NOT_FOUND")
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
