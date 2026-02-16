"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { apiPost } from "@/lib/api";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { LoginResponse } from "@/types/api";

// 필드별 유효성 검증 에러
interface FieldErrors {
	userId?: string;
	password?: string;
}

// 클라이언트 유효성 검증
function validate(fields: {
	userId: string;
	password: string;
}): FieldErrors | null {
	const errors: FieldErrors = {};

	// 아이디 검증 (필수)
	if (!fields.userId.trim()) {
		errors.userId = "아이디는 필수입니다";
	}

	// 비밀번호 검증 (필수)
	if (!fields.password) {
		errors.password = "비밀번호는 필수입니다";
	}

	return Object.keys(errors).length > 0 ? errors : null;
}

// 백엔드 에러 코드 → 사용자 메시지 매핑
function getErrorMessage(errorCode: string, fallback: string): string {
	switch (errorCode) {
		case "AUTH_INVALID_CREDENTIALS":
			return "아이디 또는 비밀번호가 올바르지 않습니다";
		case "AUTH_ACCOUNT_LOCKED":
			return "계정이 잠겨있습니다. 관리자에게 문의하세요";
		case "AUTH_ACCOUNT_PENDING":
			return "계정 인증이 필요합니다";
		default:
			return fallback;
	}
}

export default function LoginForm() {
	// 폼 입력 상태
	const [userId, setUserId]     = useState("");
	const [password, setPassword] = useState("");

	// UI 상태
	const [pending, setPending]         = useState(false);
	const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

	// 폼 제출 핸들러
	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setFieldErrors({});

		// 1. 클라이언트 유효성 검증
		const errors = validate({ userId, password });
		if (errors) {
			setFieldErrors(errors);
			return;
		}

		setPending(true);

		try {
			// 2. 백엔드 API 호출 (JSON 요청)
			const json = await apiPost<LoginResponse>("/auth/login", {
				userId,
				password,
			});

			// 3. 성공 응답 처리
			if (json.success && json.data) {
				// localStorage에 토큰 저장
				localStorage.setItem("accessToken", json.data.accessToken);
				localStorage.setItem("refreshToken", json.data.refreshToken);

				// 성공 toast
				toast.success("로그인 성공", {
					description: `${json.data.username}님, 환영합니다!`,
				});

				// 홈으로 이동 (새로고침으로 인증 상태 반영)
				window.location.href = "/";
				return;
			}

			// 4. 백엔드 에러 응답 처리
			const errorCode    = json.error?.code || "";
			const errorMessage = json.error?.message || "로그인에 실패했습니다";
			const msg = getErrorMessage(errorCode, errorMessage);
			toast.error("로그인 실패", { description: msg });
		} catch {
			toast.error("서버 연결 오류", {
				description: "서버에 연결할 수 없습니다",
			});
		} finally {
			setPending(false);
		}
	}

	return (
		<form onSubmit={handleSubmit} className="space-y-5">
			{/* 아이디 */}
			<div className="space-y-2">
				<Label htmlFor="userId">아이디</Label>
				<Input
					id="userId"
					type="text"
					value={userId}
					onChange={(e) => setUserId(e.target.value)}
					required
					placeholder="아이디를 입력하세요"
					aria-invalid={!!fieldErrors.userId}
				/>
				{fieldErrors.userId && (
					<p className="text-xs text-destructive">{fieldErrors.userId}</p>
				)}
			</div>

			{/* 비밀번호 */}
			<div className="space-y-2">
				<Label htmlFor="password">비밀번호</Label>
				<Input
					id="password"
					type="password"
					value={password}
					onChange={(e) => setPassword(e.target.value)}
					required
					placeholder="비밀번호를 입력하세요"
					aria-invalid={!!fieldErrors.password}
				/>
				{fieldErrors.password && (
					<p className="text-xs text-destructive">{fieldErrors.password}</p>
				)}
			</div>

			{/* 제출 버튼 */}
			<Button type="submit" disabled={pending} className="w-full">
				{pending ? "로그인 중..." : "로그인"}
			</Button>

			{/* 회원가입 링크 */}
			<p className="text-center text-sm text-muted-foreground">
				계정이 없으신가요?{" "}
				<Link
					href="/sign-up"
					className="font-medium text-primary hover:underline"
				>
					회원가입
				</Link>
			</p>
		</form>
	);
}
