"use client";

import { Suspense, useState, type FormEvent } from "react";
import { useSearchParams } from "next/navigation";
import { apiPost, setTokens } from "@/lib/api";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import type { LoginResponse } from "@/types/api";

// 소셜 연동 확인 폼 컴포넌트
// 기존 계정이 발견되었을 때 로컬 ID/PW로 본인 확인 후 연동을 완료한다
function LinkConfirmForm() {
	const searchParams = useSearchParams();

	// URL 쿼리에서 연동 정보 추출
	const pendingId    = searchParams.get("pendingId") ?? "";
	const email        = searchParams.get("email") ?? "";
	const provider     = searchParams.get("provider") ?? "";
	const providerName = searchParams.get("providerName") ?? provider;

	// 폼 입력 상태
	const [userId, setUserId]     = useState("");
	const [password, setPassword] = useState("");
	const [pending, setPending]   = useState(false);

	// pendingId가 없으면 잘못된 접근
	if (!pendingId) {
		return (
			<Card className="w-full max-w-md mx-auto">
				<CardContent className="pt-6 text-center">
					<p className="text-sm text-muted-foreground">
						잘못된 접근입니다. 로그인 페이지로 이동해주세요.
					</p>
					<Button
						className="mt-4"
						onClick={() => (window.location.href = "/login")}
					>
						로그인으로 이동
					</Button>
				</CardContent>
			</Card>
		);
	}

	// 폼 제출 핸들러
	async function handleSubmit(e: FormEvent) {
		e.preventDefault();

		if (!userId.trim() || !password) {
			toast.error("아이디와 비밀번호를 입력해주세요.");
			return;
		}

		setPending(true);

		try {
			// 연동 확인 API 호출
			const json = await apiPost<LoginResponse>("/auth/oauth2/link-confirm", {
				pendingId,
				userId,
				password,
			});

			if (json.success && json.data) {
				// 토큰 저장
				setTokens(json.data.accessToken, json.data.refreshToken);

				toast.success("계정 연동 완료!", {
					description: `${providerName} 계정이 연동되었습니다.`,
				});

				// 홈으로 이동
				window.location.href = "/";
			} else {
				// 에러 메시지 표시
				const msg = json.error?.message ?? "연동에 실패했습니다.";
				toast.error("연동 실패", { description: msg });
			}
		} catch {
			toast.error("서버 연결 오류", {
				description: "서버에 연결할 수 없습니다.",
			});
		} finally {
			setPending(false);
		}
	}

	return (
		<Card className="w-full max-w-md mx-auto">
			<CardHeader className="text-center">
				<CardTitle className="text-xl">기존 계정이 발견되었습니다</CardTitle>
				<CardDescription>
					<span className="font-medium text-foreground">{email}</span> 이메일로
					등록된 계정이 있습니다.
					<br />
					<span className="font-medium text-foreground">{providerName}</span> 계정을
					기존 계정에 연동하려면 로그인 정보를 입력하세요.
				</CardDescription>
			</CardHeader>
			<CardContent>
				<form onSubmit={handleSubmit} className="space-y-4">
					{/* 아이디 */}
					<div className="space-y-2">
						<Label htmlFor="userId">아이디</Label>
						<Input
							id="userId"
							type="text"
							value={userId}
							onChange={(e) => setUserId(e.target.value)}
							placeholder="기존 계정 아이디 입력"
							required
						/>
					</div>

					{/* 비밀번호 */}
					<div className="space-y-2">
						<Label htmlFor="password">비밀번호</Label>
						<Input
							id="password"
							type="password"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
							placeholder="기존 계정 비밀번호 입력"
							required
						/>
					</div>

					{/* 연동하기 버튼 */}
					<Button type="submit" disabled={pending} className="w-full">
						{pending ? "연동 중..." : "연동하기"}
					</Button>

					{/* 취소 버튼 */}
					<Button
						type="button"
						variant="outline"
						className="w-full"
						onClick={() => (window.location.href = "/login")}
					>
						취소
					</Button>
				</form>
			</CardContent>
		</Card>
	);
}

// 소셜 연동 확인 페이지
// useSearchParams()는 Suspense boundary 내에서만 사용 가능
export default function LinkConfirmPage() {
	return (
		<div className="flex min-h-screen items-center justify-center p-4">
			<Suspense
				fallback={
					<div className="text-center">
						<div className="mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent mx-auto" />
						<p className="text-sm text-muted-foreground">로딩 중...</p>
					</div>
				}
			>
				<LinkConfirmForm />
			</Suspense>
		</div>
	);
}
