package com.gizzi.core.domain.audit;

// 감사 로그 액션 유형 상수 클래스
// 기능 모듈에서도 자체 액션 상수를 정의하여 확장 가능하다
public final class AuditAction {

	// 인스턴스화 방지
	private AuditAction() {}

	// ─── 인증 관련 ───

	// 로그인
	public static final String LOGIN         = "LOGIN";

	// 로그아웃
	public static final String LOGOUT        = "LOGOUT";

	// 토큰 갱신
	public static final String TOKEN_REFRESH = "TOKEN_REFRESH";

	// ─── 사용자 관련 ───

	// 회원가입
	public static final String USER_SIGNUP       = "USER_SIGNUP";

	// 사용자 정보 수정
	public static final String USER_UPDATE       = "USER_UPDATE";

	// 사용자 삭제/탈퇴
	public static final String USER_DELETE       = "USER_DELETE";

	// 계정 잠금 해제
	public static final String USER_UNLOCK       = "USER_UNLOCK";

	// 비밀번호 변경
	public static final String PASSWORD_CHANGE   = "PASSWORD_CHANGE";

	// ─── 그룹 관련 ───

	// 그룹 생성
	public static final String GROUP_CREATE  = "GROUP_CREATE";

	// 그룹 수정
	public static final String GROUP_UPDATE  = "GROUP_UPDATE";

	// 그룹 삭제
	public static final String GROUP_DELETE  = "GROUP_DELETE";

	// 그룹 멤버 추가
	public static final String MEMBER_ADD    = "MEMBER_ADD";

	// 그룹 멤버 제거
	public static final String MEMBER_REMOVE = "MEMBER_REMOVE";

	// ─── 설정 관련 ───

	// 시스템/모듈 설정 변경
	public static final String SETTING_CHANGE = "SETTING_CHANGE";

	// ─── OAuth2 관련 ───

	// 소셜 로그인 성공
	public static final String OAUTH2_LOGIN  = "OAUTH2_LOGIN";

	// 소셜 계정 연동
	public static final String OAUTH2_LINK   = "OAUTH2_LINK";

	// 소셜 계정 연동 해제
	public static final String OAUTH2_UNLINK = "OAUTH2_UNLINK";

	// ─── SMS 관련 ───

	// SMS 발송
	public static final String SMS_SEND       = "SMS_SEND";

	// SMS OTP 인증 검증
	public static final String SMS_OTP_VERIFY = "SMS_OTP_VERIFY";
}
