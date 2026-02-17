"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { apiGet } from "@/lib/api";
import { PageViewer } from "@/components/PageViewer";

/* ===========================
 * 타입 정의
 * =========================== */

/** 모듈 정보 (resolve 응답) */
interface ModuleInfo {
	/** 모듈 코드 */
	code: string;
	/** 모듈 이름 */
	name: string;
	/** 모듈 slug */
	slug: string;
	/** 모듈 타입 */
	type: "SINGLE" | "MULTI";
}

/** 인스턴스 정보 (resolve 응답) */
interface InstanceInfo {
	/** 인스턴스 ID */
	instanceId: string;
	/** 인스턴스 이름 */
	name: string;
	/** 인스턴스 slug */
	slug: string;
	/** 인스턴스 설명 */
	description: string;
}

/** 모듈 해석 응답 */
interface ResolveResponse {
	/** 모듈 정보 */
	module: ModuleInfo;
	/** 인스턴스 정보 */
	instance: InstanceInfo;
	/** 리소스별 권한 목록 */
	permissions: Record<string, string[]>;
	/** 별칭 해석 시 콘텐츠 하위 경로 */
	subPath: string | null;
}

/* ===========================
 * 동적 라우팅 페이지
 * catch-all: /[...slug] → 모듈 slug 해석 → 모듈별 컴포넌트 렌더링
 * =========================== */

export default function DynamicPage() {
	const params = useParams();
	// slug 세그먼트 배열 (예: ["page", "about"] 또는 ["board", "notice"])
	const slugParts = params.slug as string[];

	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [resolved, setResolved] = useState<ResolveResponse | null>(null);
	// 모듈의 하위 경로 (예: "about" in /page/about, 또는 별칭 해석 결과)
	const [subPath, setSubPath] = useState<string | null>(null);

	// slug 변경 시 모듈 해석
	useEffect(() => {
		const resolveModule = async () => {
			setLoading(true);
			setError(null);
			setResolved(null);
			setSubPath(null);

			// 인증 토큰 (권한 기반 해석에 필요)
			const token = localStorage.getItem("accessToken") ?? undefined;

			try {
				if (slugParts.length >= 2) {
					// 2개 이상 slug: MULTI 모듈 시도 (또는 별칭+하위경로)
					const multiRes = await apiGet<ResolveResponse>(
						`/resolve/${slugParts[0]}/${slugParts[1]}`,
						token
					);

					if (multiRes.success && multiRes.data) {
						// 해석 성공 — subPath 통합
						setResolved(multiRes.data);
						// 응답의 subPath + 남은 slug 세그먼트(3번째 이후) 조합
						const responseSub = multiRes.data.subPath;
						const remainingParts = slugParts.slice(2);
						const combinedSub = [responseSub, ...remainingParts]
							.filter(Boolean)
							.join("/");
						setSubPath(combinedSub || null);
					} else {
						// MULTI 실패 → SINGLE 모듈로 재시도 (나머지는 하위 경로)
						const singleRes = await apiGet<ResolveResponse>(
							`/resolve/${slugParts[0]}`,
							token
						);

						if (singleRes.success && singleRes.data) {
							setResolved(singleRes.data);
							// 응답의 subPath + 나머지 slug 세그먼트 조합
							const responseSub = singleRes.data.subPath;
							const remainingParts = slugParts.slice(1);
							const combinedSub = [responseSub, ...remainingParts]
								.filter(Boolean)
								.join("/");
							setSubPath(combinedSub || null);
						} else {
							setError(singleRes.error?.message ?? "페이지를 찾을 수 없습니다.");
						}
					}
				} else if (slugParts.length === 1) {
					// 1개 slug: SINGLE 모듈 또는 별칭 해석
					const res = await apiGet<ResolveResponse>(
						`/resolve/${slugParts[0]}`,
						token
					);

					if (res.success && res.data) {
						setResolved(res.data);
						// 별칭 해석 시 subPath가 설정됨
						setSubPath(res.data.subPath || null);
					} else {
						setError(res.error?.message ?? "페이지를 찾을 수 없습니다.");
					}
				} else {
					setError("페이지를 찾을 수 없습니다.");
				}
			} catch {
				setError("서버에 연결할 수 없습니다.");
			} finally {
				setLoading(false);
			}
		};

		if (slugParts && slugParts.length > 0) {
			resolveModule();
		}
		// slugParts는 매 렌더마다 새 배열이므로 join으로 비교
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [slugParts?.join("/")]);

	// 로딩 상태
	if (loading) {
		return (
			<div className="flex items-center justify-center py-24">
				<div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
			</div>
		);
	}

	// 에러 상태 (404)
	if (error || !resolved) {
		return (
			<div className="mx-auto max-w-3xl px-6 py-24 text-center">
				<h1 className="text-4xl font-bold text-muted-foreground">404</h1>
				<p className="mt-4 text-muted-foreground">
					{error ?? "페이지를 찾을 수 없습니다."}
				</p>
			</div>
		);
	}

	// 모듈 코드별 컴포넌트 분기
	return renderModule(resolved, subPath);
}

/**
 * 모듈 코드에 따라 적절한 뷰 컴포넌트 렌더링
 * - page: PageViewer (SINGLE, subPath = 페이지 slug)
 * - 기타: 기본 모듈 정보 표시 (향후 모듈별 컴포넌트 추가)
 */
function renderModule(resolved: ResolveResponse, subPath: string | null) {
	const { module: mod, instance } = resolved;

	// 페이지 모듈 — subPath를 페이지 slug로 사용
	if (mod.code === "page") {
		if (subPath) {
			// /page/{page-slug} 또는 별칭 경로 → 개별 페이지 뷰어
			return <PageViewer slug={subPath} />;
		}

		// /page (하위 경로 없음) → 모듈 랜딩
		return (
			<div className="mx-auto max-w-3xl px-6 py-12">
				<h1 className="text-2xl font-bold">{mod.name}</h1>
				<p className="mt-2 text-muted-foreground">
					{instance.description || "페이지 목록"}
				</p>
			</div>
		);
	}

	// 미구현 모듈 — 기본 정보 표시
	return (
		<div className="mx-auto max-w-3xl px-6 py-12">
			<h1 className="text-2xl font-bold">{instance.name}</h1>
			<p className="mt-2 text-muted-foreground">{instance.description}</p>
			<p className="mt-4 text-sm text-muted-foreground">
				모듈: {mod.name} ({mod.code}) · 타입: {mod.type}
			</p>
		</div>
	);
}
