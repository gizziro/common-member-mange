package com.gizzi.core.domain.audit;

// 감사 로그 대상 유형 상수 클래스
// 기능 모듈에서도 자체 대상 유형을 정의하여 확장 가능하다
public final class AuditTarget {

	// 인스턴스화 방지
	private AuditTarget() {}

	// 사용자
	public static final String USER     = "USER";

	// 그룹
	public static final String GROUP    = "GROUP";

	// 그룹 멤버
	public static final String MEMBER   = "MEMBER";

	// 시스템/모듈 설정
	public static final String SETTING  = "SETTING";

	// 소셜 연동 (UserIdentity)
	public static final String IDENTITY = "IDENTITY";

	// SMS 관련
	public static final String SMS      = "SMS";
}
