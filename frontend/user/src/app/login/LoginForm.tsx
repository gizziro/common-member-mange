"use client";

import { useState, useEffect, type FormEvent } from "react";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
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

// 소셜 Provider 타입
interface OAuth2Provider {
	code: string;
	name: string;
	iconUrl: string | null;
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

// Provider별 스타일 매핑 (배경색, 글자색, hover)
function getProviderStyle(code: string): string {
	switch (code) {
		case "google":
			return "bg-white text-gray-700 border border-gray-300 hover:bg-gray-50";
		case "kakao":
			return "bg-[#FEE500] text-[#191919] hover:bg-[#FDD800]";
		case "naver":
			return "bg-[#03C75A] text-white hover:bg-[#02b351]";
		default:
			return "bg-gray-100 text-gray-800 hover:bg-gray-200";
	}
}

// Provider별 아이콘 SVG
function ProviderIcon({ code }: { code: string }) {
	switch (code) {
		case "google":
			return (
				<svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
					<path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
					<path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
					<path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
					<path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
				</svg>
			);
		case "kakao":
			return (
				<svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
					<path d="M12 3C6.48 3 2 6.58 2 10.94c0 2.8 1.86 5.27 4.66 6.67-.15.54-.96 3.47-.99 3.69 0 0-.02.17.09.23.11.07.24.01.24.01.32-.04 3.7-2.44 4.28-2.86.56.08 1.14.12 1.72.12 5.52 0 10-3.58 10-7.94S17.52 3 12 3z" fill="#191919"/>
				</svg>
			);
		case "naver":
			return (
				<svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
					<path d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727v12.845z" fill="currentColor"/>
				</svg>
			);
		default:
			return null;
	}
}

export default function LoginForm() {
	// 폼 입력 상태
	const [userId, setUserId]     = useState("");
	const [password, setPassword] = useState("");

	// UI 상태
	const [pending, setPending]         = useState(false);
	const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

	// 소셜 Provider 목록
	const [providers, setProviders] = useState<OAuth2Provider[]>([]);

	// 소셜 로그인 Provider 목록 조회
	useEffect(() => {
		apiGet<OAuth2Provider[]>("/auth/oauth2/providers")
			.then((json) => {
				if (json.success && json.data) {
					setProviders(json.data);
				}
			})
			.catch(() => {});
	}, []);

	// 소셜 로그인 버튼 클릭 핸들러
	async function handleSocialLogin(providerCode: string) {
		try {
			// Authorization URL 생성 요청
			const json = await apiGet<string>(`/auth/oauth2/authorize/${providerCode}`);
			if (json.success && json.data) {
				// 소셜 로그인 페이지로 리다이렉트
				window.location.href = json.data;
			} else {
				toast.error("소셜 로그인 오류", {
					description: json.error?.message || "Authorization URL을 가져올 수 없습니다",
				});
			}
		} catch {
			toast.error("서버 연결 오류", {
				description: "서버에 연결할 수 없습니다",
			});
		}
	}

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

			{/* 소셜 로그인 버튼 영역 */}
			{providers.length > 0 && (
				<>
					{/* 구분선 */}
					<div className="relative my-4">
						<div className="absolute inset-0 flex items-center">
							<span className="w-full border-t" />
						</div>
						<div className="relative flex justify-center text-xs uppercase">
							<span className="bg-background px-2 text-muted-foreground">
								또는
							</span>
						</div>
					</div>

					{/* 소셜 로그인 버튼 */}
					<div className="space-y-2">
						{providers.map((provider) => (
							<button
								key={provider.code}
								type="button"
								onClick={() => handleSocialLogin(provider.code)}
								className={`flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5 text-sm font-medium transition-colors ${getProviderStyle(provider.code)}`}
							>
								<ProviderIcon code={provider.code} />
								{provider.name}로 로그인
							</button>
						))}
					</div>
				</>
			)}

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
