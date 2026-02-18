package com.gizzi.module.board.entity;

// 게시글 콘텐츠 유형 enum
public enum PostContentType {
	// 일반 텍스트
	PLAIN_TEXT,
	// 마크다운
	MARKDOWN,
	// HTML (서버사이드 살균 처리 적용)
	HTML
}
