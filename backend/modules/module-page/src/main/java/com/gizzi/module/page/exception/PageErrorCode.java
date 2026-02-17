package com.gizzi.module.page.exception;

import com.gizzi.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 페이지 모듈 관련 에러 코드 (PAGE_*)
@Getter
@AllArgsConstructor
public enum PageErrorCode implements ErrorCode {

	// 페이지 조회 실패
	PAGE_NOT_FOUND        ("PAGE_NOT_FOUND",        "페이지를 찾을 수 없습니다",              "페이지 ID 또는 slug로 조회 실패",       HttpStatus.NOT_FOUND),

	// 페이지 슬러그 중복
	PAGE_DUPLICATE_SLUG   ("PAGE_DUPLICATE_SLUG",   "이미 사용 중인 페이지 슬러그입니다",       "page slug 중복 검증 실패",            HttpStatus.CONFLICT),

	// 비공개 페이지 접근
	PAGE_NOT_PUBLISHED    ("PAGE_NOT_PUBLISHED",    "비공개 상태인 페이지입니다",              "is_published=false인 페이지 접근",     HttpStatus.NOT_FOUND),

	// 페이지 접근 권한 없음 (보안: 404로 반환하여 페이지 존재 여부 노출 방지)
	PAGE_ACCESS_DENIED    ("PAGE_NOT_FOUND",        "페이지를 찾을 수 없습니다",              "권한 설정된 페이지에 대한 비인가 접근",    HttpStatus.NOT_FOUND);

	// 에러 코드 문자열
	private final String     code;

	// 사용자에게 표시할 에러 메시지
	private final String     message;

	// 개발자용 상세 설명
	private final String     description;

	// HTTP 상태 코드
	private final HttpStatus httpStatus;
}
