"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { apiPost } from "@/lib/api";
import type { SignupResponse } from "@/types/api";

// 필드별 유효성 검증 에러
interface FieldErrors {
	userId?: string;
	password?: string;
	username?: string;
	email?: string;
}

// 클라이언트 유효성 검증
function validate(fields: {
	userId: string;
	password: string;
	username: string;
	email: string;
}): FieldErrors | null {
	const errors: FieldErrors = {};

	// 아이디 검증 (4~50자)
	if (!fields.userId.trim()) {
		errors.userId = "아이디는 필수입니다";
	} else if (fields.userId.length < 4 || fields.userId.length > 50) {
		errors.userId = "아이디는 4~50자여야 합니다";
	}

	// 비밀번호 검증 (8~100자)
	if (!fields.password) {
		errors.password = "비밀번호는 필수입니다";
	} else if (fields.password.length < 8 || fields.password.length > 100) {
		errors.password = "비밀번호는 8~100자여야 합니다";
	}

	// 이름 검증 (2~100자)
	if (!fields.username.trim()) {
		errors.username = "이름은 필수입니다";
	} else if (fields.username.length < 2 || fields.username.length > 100) {
		errors.username = "이름은 2~100자여야 합니다";
	}

	// 이메일 검증
	if (!fields.email.trim()) {
		errors.email = "이메일은 필수입니다";
	} else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(fields.email)) {
		errors.email = "올바른 이메일 형식이 아닙니다";
	}

	return Object.keys(errors).length > 0 ? errors : null;
}

export default function SignupForm() {
	// 폼 입력 상태
	const [userId, setUserId] = useState("");
	const [password, setPassword] = useState("");
	const [username, setUsername] = useState("");
	const [email, setEmail] = useState("");

	// UI 상태
	const [pending, setPending] = useState(false);
	const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
	const [serverError, setServerError] = useState("");
	const [successData, setSuccessData] = useState<SignupResponse | null>(null);

	// 폼 제출 핸들러
	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setFieldErrors({});
		setServerError("");

		// 1. 클라이언트 유효성 검증
		const errors = validate({ userId, password, username, email });
		if (errors) {
			setFieldErrors(errors);
			return;
		}

		setPending(true);

		try {
			// 2. 백엔드 API 호출 (JSON 요청)
			const json = await apiPost<SignupResponse>("/auth/signup", {
				userId,
				password,
				username,
				email,
			});

			// 3. 성공 응답 처리
			if (json.success && json.data) {
				setSuccessData(json.data);
				return;
			}

			// 4. 백엔드 에러 응답 처리
			const errorCode = json.error?.code || "";
			const errorMessage = json.error?.message || "회원가입에 실패했습니다";

			if (errorCode === "USER_DUPLICATE_ID") {
				setFieldErrors({ userId: errorMessage });
			} else if (errorCode === "USER_DUPLICATE_EMAIL") {
				setFieldErrors({ email: errorMessage });
			} else {
				setServerError(errorMessage);
			}
		} catch {
			setServerError("서버에 연결할 수 없습니다");
		} finally {
			setPending(false);
		}
	}

	// 회원가입 성공 시 완료 화면
	if (successData) {
		return (
			<div className="text-center">
				<div className="mb-6">
					<div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
						<svg
							className="h-8 w-8 text-green-600"
							fill="none"
							viewBox="0 0 24 24"
							stroke="currentColor"
						>
							<path
								strokeLinecap="round"
								strokeLinejoin="round"
								strokeWidth={2}
								d="M5 13l4 4L19 7"
							/>
						</svg>
					</div>
				</div>
				<h2 className="mb-2 text-xl font-semibold text-gray-900">
					회원가입이 완료되었습니다
				</h2>
				<p className="mb-6 text-sm text-gray-500">
					<strong>{successData.userId}</strong>님, 환영합니다!
				</p>
				<Link
					href="/"
					className="inline-block rounded-lg bg-blue-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-blue-700 transition-colors"
				>
					홈으로 이동
				</Link>
			</div>
		);
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
					minLength={4}
					maxLength={50}
					placeholder="4자 이상 입력"
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
					minLength={8}
					maxLength={100}
					placeholder="8자 이상 입력"
					className={`w-full rounded-lg border px-3.5 py-2.5 text-sm outline-none transition-colors
						placeholder:text-gray-400
						focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20
						${fieldErrors.password ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"}`}
				/>
				{fieldErrors.password && (
					<p className="mt-1 text-xs text-red-500">{fieldErrors.password}</p>
				)}
			</div>

			{/* 이름 */}
			<div>
				<label
					htmlFor="username"
					className="mb-1.5 block text-sm font-medium text-gray-700"
				>
					이름
				</label>
				<input
					id="username"
					type="text"
					value={username}
					onChange={(e) => setUsername(e.target.value)}
					required
					minLength={2}
					maxLength={100}
					placeholder="이름을 입력하세요"
					className={`w-full rounded-lg border px-3.5 py-2.5 text-sm outline-none transition-colors
						placeholder:text-gray-400
						focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20
						${fieldErrors.username ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"}`}
				/>
				{fieldErrors.username && (
					<p className="mt-1 text-xs text-red-500">{fieldErrors.username}</p>
				)}
			</div>

			{/* 이메일 */}
			<div>
				<label
					htmlFor="email"
					className="mb-1.5 block text-sm font-medium text-gray-700"
				>
					이메일
				</label>
				<input
					id="email"
					type="email"
					value={email}
					onChange={(e) => setEmail(e.target.value)}
					required
					placeholder="example@email.com"
					className={`w-full rounded-lg border px-3.5 py-2.5 text-sm outline-none transition-colors
						placeholder:text-gray-400
						focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20
						${fieldErrors.email ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"}`}
				/>
				{fieldErrors.email && (
					<p className="mt-1 text-xs text-red-500">{fieldErrors.email}</p>
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
				{pending ? "가입 처리 중..." : "회원가입"}
			</button>

			{/* 로그인 링크 */}
			<p className="text-center text-sm text-gray-500">
				이미 계정이 있으신가요?{" "}
				<Link
					href="/login"
					className="font-medium text-blue-600 hover:text-blue-700"
				>
					로그인
				</Link>
			</p>
		</form>
	);
}
