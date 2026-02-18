"use client";

import { useEffect, useState } from "react";
import DOMPurify from "dompurify";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { apiGet } from "@/lib/api";

/* ===========================
 * 타입 정의
 * =========================== */

/** 페이지 상세 응답 */
interface PageDetail {
	/** 페이지 PK */
	id: string;
	/** URL slug */
	slug: string;
	/** 페이지 제목 */
	title: string;
	/** 페이지 본문 */
	content: string | null;
	/** 콘텐츠 유형 (HTML/MARKDOWN/TEXT) */
	contentType: string;
	/** 공개 여부 */
	isPublished: boolean;
	/** 작성자 */
	createdBy: string;
	/** 작성일 */
	createdAt: string;
	/** 수정일 */
	updatedAt: string;
}

/** 컴포넌트 Props */
interface PageViewerProps {
	/** 조회할 페이지 slug */
	slug: string;
}

/* ===========================
 * 페이지 콘텐츠 뷰어
 * =========================== */

/** 페이지 모듈의 콘텐츠를 렌더링하는 뷰어 컴포넌트 */
export function PageViewer({ slug }: PageViewerProps) {
	const [page, setPage] = useState<PageDetail | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	// 페이지 데이터 로드
	useEffect(() => {
		const loadPage = async () => {
			setLoading(true);
			setError(null);

			try {
				// 인증 토큰 (권한 기반 페이지 접근에 필요)
				const token = localStorage.getItem("accessToken") ?? undefined;

				// 페이지 slug 조회 (인증 토큰 포함 — 권한 체크용)
				const res = await apiGet<PageDetail>(`/pages/${slug}`, token);

				if (res.success && res.data) {
					setPage(res.data);
				} else {
					setError(res.error?.message ?? "페이지를 찾을 수 없습니다.");
				}
			} catch {
				setError("서버에 연결할 수 없습니다.");
			} finally {
				setLoading(false);
			}
		};

		loadPage();
	}, [slug]);

	// 로딩 상태
	if (loading) {
		return (
			<div className="flex items-center justify-center py-24">
				<div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
			</div>
		);
	}

	// 에러 상태 — 권한 없음/존재하지 않음 모두 동일하게 "없는 페이지" 표시
	if (error || !page) {
		return (
			<div className="mx-auto max-w-3xl px-6 py-24 text-center">
				<h1 className="text-4xl font-bold text-muted-foreground">404</h1>
				<p className="mt-4 text-muted-foreground">
					없는 페이지입니다.
				</p>
			</div>
		);
	}

	return (
		<div className="mx-auto max-w-3xl px-6 py-12">
			{/* 페이지 제목 */}
			<h1 className="text-3xl font-bold">{page.title}</h1>

			{/* 메타 정보 (수정일) */}
			<p className="mt-2 text-sm text-muted-foreground">
				마지막 수정: {new Date(page.updatedAt).toLocaleDateString("ko-KR")}
			</p>

			{/* 구분선 */}
			<hr className="my-6" />

			{/* 콘텐츠 렌더링 (유형에 따라 분기) */}
			<div className="prose prose-gray max-w-none">
				{renderContent(page.content, page.contentType)}
			</div>
		</div>
	);
}

/**
 * 콘텐츠 유형에 따라 적절한 렌더링 방식 선택
 * - HTML: DOMPurify로 XSS 위험 태그/속성 제거 후 렌더링
 * - MARKDOWN: react-markdown + remark-gfm으로 GFM 렌더링
 * - TEXT: <pre> 태그로 렌더링
 */
function renderContent(content: string | null, contentType: string) {
	// 콘텐츠 없음
	if (!content) {
		return (
			<p className="text-muted-foreground">콘텐츠가 없습니다.</p>
		);
	}

	switch (contentType) {
		case "HTML":
			// DOMPurify로 XSS 위험 요소 제거 후 안전한 HTML만 렌더링
			return (
				<div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(content) }} />
			);

		case "MARKDOWN":
			// GFM(GitHub Flavored Markdown) 지원 Markdown 렌더링
			return (
				<ReactMarkdown remarkPlugins={[remarkGfm]}>
					{content}
				</ReactMarkdown>
			);

		case "TEXT":
		default:
			// 텍스트 원본 표시
			return (
				<pre className="whitespace-pre-wrap rounded-lg bg-muted p-4 text-sm">
					{content}
				</pre>
			);
	}
}
