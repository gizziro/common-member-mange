"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

// 에러 코드별 사용자 친화적 메시지 매핑
const ERROR_MESSAGES: Record<string, { title: string; description: string }> = {
	// 이미 다른 계정에 연동된 소셜 계정
	OAUTH2_ALREADY_LINKED: {
		title: "이미 연동된 계정",
		description: "이 소셜 계정은 이미 다른 계정에 연동되어 있습니다. 기존 연동을 해제한 후 다시 시도해주세요.",
	},
	// state 검증 실패 (세션 만료 등)
	OAUTH2_INVALID_STATE: {
		title: "인증 세션 만료",
		description: "인증 세션이 만료되었습니다. 다시 시도해주세요.",
	},
	// 토큰 교환 실패
	OAUTH2_TOKEN_EXCHANGE_FAILED: {
		title: "인증 처리 실패",
		description: "소셜 로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
	},
	// 사용자 정보 조회 실패
	OAUTH2_USERINFO_FAILED: {
		title: "정보 조회 실패",
		description: "소셜 계정 정보를 가져올 수 없습니다. 잠시 후 다시 시도해주세요.",
	},
	// 이메일 미제공
	OAUTH2_EMAIL_NOT_PROVIDED: {
		title: "이메일 정보 없음",
		description: "소셜 계정에 이메일 정보가 없어 처리할 수 없습니다. 소셜 계정 설정에서 이메일을 확인해주세요.",
	},
	// 비활성화된 Provider
	OAUTH2_PROVIDER_DISABLED: {
		title: "비활성화된 로그인 수단",
		description: "현재 이 소셜 로그인 수단은 사용할 수 없습니다.",
	},
};

// 기본 에러 메시지 (매핑되지 않은 코드용)
const DEFAULT_ERROR = {
	title: "오류 발생",
	description: "소셜 로그인 처리 중 오류가 발생했습니다.",
};

// OAuth2 콜백 에러 표시 컴포넌트
// URL 쿼리에서 mode, code, message를 읽어 에러 정보를 표시한다
function OAuth2ErrorContent() {
	// URL 쿼리 파라미터 읽기
	const searchParams = useSearchParams();

	// 에러 정보 추출
	const mode    = searchParams.get("mode") ?? "login";
	const code    = searchParams.get("code") ?? "";
	const message = searchParams.get("message") ?? "";

	// 에러 코드에 해당하는 사용자 친화적 메시지 결정
	const errorInfo = ERROR_MESSAGES[code] ?? DEFAULT_ERROR;

	// 모드에 따라 돌아갈 페이지 결정
	const isLinkMode  = mode === "link";
	const backUrl     = isLinkMode ? "/profile" : "/login";
	const backLabel   = isLinkMode ? "프로필로 돌아가기" : "로그인 페이지로 이동";

	return (
		<div className="flex min-h-screen items-center justify-center p-4">
			<Card className="w-full max-w-md">
				<CardHeader className="text-center">
					{/* 에러 아이콘 */}
					<div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10">
						<svg
							className="h-6 w-6 text-destructive"
							fill="none"
							viewBox="0 0 24 24"
							strokeWidth={1.5}
							stroke="currentColor"
						>
							<path
								strokeLinecap="round"
								strokeLinejoin="round"
								d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
							/>
						</svg>
					</div>
					<CardTitle>{errorInfo.title}</CardTitle>
				</CardHeader>
				<CardContent className="text-center space-y-3">
					{/* 사용자 친화적 설명 */}
					<p className="text-sm text-muted-foreground">
						{errorInfo.description}
					</p>

					{/* 백엔드 원본 메시지 (코드와 다를 때만 표시) */}
					{message && message !== errorInfo.description && (
						<p className="text-xs text-muted-foreground/70 bg-muted rounded-md p-2">
							{message}
						</p>
					)}
				</CardContent>
				<CardFooter className="flex justify-center">
					{/* 돌아가기 버튼 */}
					<Button
						variant="default"
						onClick={() => { window.location.href = backUrl; }}
					>
						{backLabel}
					</Button>
				</CardFooter>
			</Card>
		</div>
	);
}

// OAuth2 콜백 에러 페이지
// useSearchParams()는 Suspense boundary 내에서만 사용 가능
export default function OAuth2ErrorPage() {
	return (
		<Suspense
			fallback={
				<div className="flex min-h-screen items-center justify-center">
					<div className="text-center">
						<div className="mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent mx-auto" />
					</div>
				</div>
			}
		>
			<OAuth2ErrorContent />
		</Suspense>
	);
}
