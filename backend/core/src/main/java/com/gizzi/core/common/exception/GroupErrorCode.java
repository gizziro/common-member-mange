package com.gizzi.core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 그룹 관련 에러 코드 (GROUP_*)
// 그룹 CRUD, 멤버 관리에서 발생하는 비즈니스 에러
@Getter
@AllArgsConstructor
public enum GroupErrorCode implements ErrorCode
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 그룹 조회/생성 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	GROUP_NOT_FOUND              ("GROUP_NOT_FOUND",                  "그룹을 찾을 수 없습니다",                       "PK 또는 groupCode로 조회 실패",          HttpStatus.NOT_FOUND),		// 그룹 조회 실패
	DUPLICATE_GROUP_CODE         ("GROUP_DUPLICATE_CODE",             "이미 사용 중인 그룹 코드입니다",                 "groupCode 중복 검증 실패",               HttpStatus.CONFLICT),		// 그룹 코드 중복
	DUPLICATE_GROUP_NAME         ("GROUP_DUPLICATE_NAME",             "이미 사용 중인 그룹 이름입니다",                 "name 중복 검증 실패",                    HttpStatus.CONFLICT),		// 그룹 이름 중복

	//----------------------------------------------------------------------------------------------------------------------
	// [ 시스템 그룹 보호 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	SYSTEM_GROUP_UNDELETABLE     ("GROUP_SYSTEM_UNDELETABLE",         "시스템 그룹은 삭제할 수 없습니다",               "시스템 그룹 삭제 시도",                   HttpStatus.BAD_REQUEST),	// 시스템 그룹 삭제 불가
	SYSTEM_GROUP_IMMUTABLE       ("GROUP_SYSTEM_IMMUTABLE",           "시스템 그룹의 코드는 변경할 수 없습니다",         "시스템 그룹 코드 변경 시도",               HttpStatus.BAD_REQUEST),	// 시스템 그룹 코드 변경 불가

	//----------------------------------------------------------------------------------------------------------------------
	// [ 멤버 관리 에러 ]
	//----------------------------------------------------------------------------------------------------------------------
	MEMBER_ALREADY_EXISTS        ("GROUP_MEMBER_EXISTS",              "이미 그룹에 소속된 회원입니다",                  "그룹 멤버 중복 추가 시도",                 HttpStatus.CONFLICT),		// 멤버 이미 존재
	MEMBER_NOT_FOUND             ("GROUP_MEMBER_NOT_FOUND",           "그룹에 소속되지 않은 회원입니다",                "그룹 멤버 조회 실패",                     HttpStatus.NOT_FOUND),		// 멤버 조회 실패
	SYSTEM_GROUP_MEMBER_PROTECTED("GROUP_SYSTEM_MEMBER_PROTECTED",   "시스템 그룹의 기본 멤버는 제거할 수 없습니다",     "시스템 그룹 멤버 제거 시도",               HttpStatus.BAD_REQUEST);	// 시스템 그룹 멤버 보호

	//----------------------------------------------------------------------------------------------------------------------
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final String     code;			// 에러 코드 문자열 (예: "GROUP_NOT_FOUND")
	private final String     message;		// 사용자에게 표시할 에러 메시지
	private final String     description;	// 개발자용 상세 설명 (로깅/디버깅 용도)
	private final HttpStatus httpStatus;	// HTTP 상태 코드
}
