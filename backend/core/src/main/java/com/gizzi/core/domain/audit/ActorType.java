package com.gizzi.core.domain.audit;

// 감사 로그 행위자 유형
public enum ActorType {

	// 로그인 사용자에 의한 행위
	USER,

	// 시스템 자동 처리에 의한 행위 (스케줄러, 자동 잠금 해제 등)
	SYSTEM
}
