"use client";

import { Suspense, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "sonner";

// 마이페이지 소셜 연동 성공 처리 컴포넌트
// 연동 콜백 후 toast 표시 → 프로필 페이지로 리다이렉트
function LinkSuccessHandler() {
	const searchParams = useSearchParams();

	useEffect(() => {
		// URL 쿼리에서 Provider 추출
		const provider = searchParams.get("provider") ?? "소셜";

		// 성공 toast 표시
		toast.success("소셜 계정 연동 완료", {
			description: `${provider} 계정이 연동되었습니다.`,
		});

		// 프로필 페이지로 이동
		window.location.href = "/profile";
	}, [searchParams]);

	return null;
}

// 마이페이지 소셜 연동 성공 콜백 페이지
// useSearchParams()는 Suspense boundary 내에서만 사용 가능
export default function LinkSuccessPage() {
	return (
		<div className="flex min-h-screen items-center justify-center">
			<div className="text-center">
				<div className="mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent mx-auto" />
				<p className="text-sm text-muted-foreground">연동 처리 중...</p>
			</div>
			<Suspense>
				<LinkSuccessHandler />
			</Suspense>
		</div>
	);
}
