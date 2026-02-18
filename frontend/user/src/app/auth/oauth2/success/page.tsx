"use client";

import { Suspense, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { setTokens } from "@/lib/api";
import { toast } from "sonner";

// 소셜 로그인 콜백 처리 컴포넌트
// URL 쿼리에서 토큰을 추출하여 localStorage에 저장 후 홈으로 이동한다
function OAuth2SuccessHandler() {
	const searchParams = useSearchParams();

	useEffect(() => {
		// URL 쿼리 파라미터에서 토큰 추출
		const token    = searchParams.get("token");
		const refresh  = searchParams.get("refresh");
		const username = searchParams.get("username");

		if (token && refresh) {
			// 토큰 저장
			setTokens(token, refresh);

			// 성공 toast 표시
			toast.success("소셜 로그인 성공", {
				description: username ? `${username}님, 환영합니다!` : "환영합니다!",
			});

			// 홈으로 이동 (새로고침으로 인증 상태 반영)
			window.location.href = "/";
		} else {
			// 토큰이 없으면 에러 → 로그인 페이지로 이동
			toast.error("소셜 로그인 실패", {
				description: "인증 정보를 받지 못했습니다",
			});
			window.location.href = "/login";
		}
	}, [searchParams]);

	return null;
}

// 소셜 로그인 성공 콜백 페이지
// useSearchParams()는 Suspense boundary 내에서만 사용 가능
export default function OAuth2SuccessPage() {
	return (
		<div className="flex min-h-screen items-center justify-center">
			<div className="text-center">
				<div className="mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent mx-auto" />
				<p className="text-sm text-muted-foreground">로그인 처리 중...</p>
			</div>
			<Suspense>
				<OAuth2SuccessHandler />
			</Suspense>
		</div>
	);
}
