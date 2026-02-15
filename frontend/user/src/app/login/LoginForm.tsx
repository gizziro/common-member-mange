"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { apiPost } from "@/lib/api";
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
	const [serverError, setServerError] = useState("");

	// 폼 제출 핸들러
	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setFieldErrors({});
		setServerError("");

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

				// 홈으로 이동 (새로고침으로 인증 상태 반영)
				window.location.href = "/";
				return;
			}

			// 4. 백엔드 에러 응답 처리
			const errorCode    = json.error?.code || "";
			const errorMessage = json.error?.message || "로그인에 실패했습니다";
			setServerError(getErrorMessage(errorCode, errorMessage));
		} catch {
			setServerError("서버에 연결할 수 없습니다");
		} finally {
			setPending(false);
		}
	}

	return (
		<form onSubmit={handleSubmit} className="space-y-5">
			{/* 서버 에러 메시지 */}
			{serverError && (
				<div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
					{serverError}
				</div>
			)}

			{/* 아이디 */}
			<div>
				<label
					htmlFor="userId"
					className="mb-1.5 block text-sm font-medium text-gray-700"
				>
					아이디
				</label>
				<input
					id="userId"
					type="text"
					value={userId}
					onChange={(e) => setUserId(e.target.value)}
					required
					placeholder="아이디를 입력하세요"
					className={`w-full rounded-lg border px-3.5 py-2.5 text-sm outline-none transition-colors
						placeholder:text-gray-400
						focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20
						${fieldErrors.userId ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"}`}
				/>
				{fieldErrors.userId && (
					<p className="mt-1 text-xs text-red-500">{fieldErrors.userId}</p>
				)}
			</div>

			{/* 비밀번호 */}
			<div>
				<label
					htmlFor="password"
					className="mb-1.5 block text-sm font-medium text-gray-700"
				>
					비밀번호
				</label>
				<input
					id="password"
					type="password"
					value={password}
					onChange={(e) => setPassword(e.target.value)}
					required
					placeholder="비밀번호를 입력하세요"
					className={`w-full rounded-lg border px-3.5 py-2.5 text-sm outline-none transition-colors
						placeholder:text-gray-400
						focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20
						${fieldErrors.password ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"}`}
				/>
				{fieldErrors.password && (
					<p className="mt-1 text-xs text-red-500">{fieldErrors.password}</p>
				)}
			</div>

			{/* 제출 버튼 */}
			<button
				type="submit"
				disabled={pending}
				className="w-full rounded-lg bg-blue-600 py-2.5 text-sm font-medium text-white
					hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-400
					transition-colors"
			>
				{pending ? "로그인 중..." : "로그인"}
			</button>

			{/* 회원가입 링크 */}
			<p className="text-center text-sm text-gray-500">
				계정이 없으신가요?{" "}
				<Link
					href="/sign-up"
					className="font-medium text-blue-600 hover:text-blue-700"
				>
					회원가입
				</Link>
			</p>
		</form>
	);
}
