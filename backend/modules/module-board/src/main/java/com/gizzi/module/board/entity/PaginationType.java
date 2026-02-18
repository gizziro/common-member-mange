package com.gizzi.module.board.entity;

// 페이지네이션 유형 enum
public enum PaginationType {
	// 오프셋 기반 페이지네이션 (번호 기반 페이지 이동)
	OFFSET,
	// 커서 기반 페이지네이션
	CURSOR,
	// 무한 스크롤 (스크롤 하단 도달 시 자동 로드)
	INFINITE_SCROLL,
	// 더보기 버튼 (클릭 시 다음 페이지 로드)
	LOAD_MORE
}
