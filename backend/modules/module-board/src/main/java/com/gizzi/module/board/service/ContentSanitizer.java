package com.gizzi.module.board.service;

import com.gizzi.module.board.entity.PostContentType;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

// 서버사이드 HTML 콘텐츠 위생 처리 유틸리티
// XSS 공격을 방어하기 위해 허용된 태그/속성만 남기고 나머지를 제거한다
// HTML 타입 콘텐츠에만 적용되며, MARKDOWN/PLAIN_TEXT는 그대로 반환한다
@Component
public class ContentSanitizer {

	// OWASP 기반 HTML 허용 정책 (화이트리스트 방식)
	// 게시판에서 일반적으로 사용하는 HTML 태그와 속성만 허용
	private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
			// 제목 태그 (h1 ~ h6)
			.allowElements("h1", "h2", "h3", "h4", "h5", "h6")
			// 문단, 줄바꿈, 수평선
			.allowElements("p", "br", "hr")
			// 텍스트 서식 (굵게, 기울임, 취소선, 밑줄)
			.allowElements("strong", "b", "em", "i", "del", "u", "s", "mark", "sup", "sub")
			// 인라인/블록 컨테이너
			.allowElements("span", "div")
			// 링크 — href 속성만 허용, javascript: 프로토콜 차단
			.allowElements("a")
			.allowAttributes("href").onElements("a")
			.allowAttributes("target").onElements("a")
			.allowAttributes("rel").onElements("a")
			.requireRelNofollowOnLinks()
			// 이미지 — src, alt, width, height 속성만 허용
			.allowElements("img")
			.allowAttributes("src", "alt", "width", "height").onElements("img")
			// 리스트
			.allowElements("ul", "ol", "li")
			// 테이블
			.allowElements("table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption", "colgroup", "col")
			.allowAttributes("colspan", "rowspan").onElements("th", "td")
			// 코드 블록
			.allowElements("code", "pre", "kbd", "samp", "var")
			// 인용
			.allowElements("blockquote")
			// 세부 정보
			.allowElements("details", "summary")
			// 정의 목록
			.allowElements("dl", "dt", "dd")
			// 그림
			.allowElements("figure", "figcaption")
			// 공통 스타일/클래스 속성 허용 (CSS 인라인 스타일은 제한적 허용)
			.allowAttributes("class").globally()
			.allowAttributes("id").globally()
			// 정책 빌드
			.toFactory();

	// 콘텐츠 살균 처리
	// HTML 타입일 때만 OWASP 정책으로 위생 처리하고, 나머지 타입은 그대로 반환한다
	public String sanitize(String content, PostContentType contentType) {
		// null이나 빈 문자열은 그대로 반환
		if (content == null || content.isEmpty()) {
			return content;
		}

		// HTML 타입일 때만 살균 처리
		if (contentType == PostContentType.HTML) {
			return POLICY.sanitize(content);
		}

		// MARKDOWN, PLAIN_TEXT 등은 그대로 반환
		return content;
	}
}
