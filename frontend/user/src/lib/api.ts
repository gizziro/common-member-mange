/* ===========================
 * API 클라이언트 — fetch 기반 JSON 요청 유틸리티
 * 자동 Authorization 헤더 추가 + 401 시 토큰 갱신 재시도
 * =========================== */

import type { ApiResponse } from "@/types/api";

/** 토큰 저장 키 */
const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";

/** Access Token 조회 */
export function getAccessToken(): string | null {
	if (typeof window === "undefined") return null;
	return localStorage.getItem(ACCESS_TOKEN_KEY);
}

/** Refresh Token 조회 */
export function getRefreshToken(): string | null {
	if (typeof window === "undefined") return null;
	return localStorage.getItem(REFRESH_TOKEN_KEY);
}

/** 토큰 쌍 저장 */
export function setTokens(accessToken: string, refreshToken: string): void {
	localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
	localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

/** 토큰 삭제 (로그아웃 효과) */
export function clearTokens(): void {
	localStorage.removeItem(ACCESS_TOKEN_KEY);
	localStorage.removeItem(REFRESH_TOKEN_KEY);
}

/** 토큰 갱신 진행 중 플래그 (중복 갱신 방지) */
let isRefreshing = false;
let refreshPromise: Promise<boolean> | null = null;

/** 토큰 갱신 시도 — race condition 방지 (동시 다발 401 시 단일 갱신) */
async function tryRefreshToken(): Promise<boolean> {
	// 이미 갱신 중이면 기존 Promise 반환
	if (isRefreshing && refreshPromise) {
		return refreshPromise;
	}

	isRefreshing = true;
	refreshPromise = (async () => {
		try {
			const refreshToken = getRefreshToken();
			if (!refreshToken) return false;

			// 갱신 요청은 apiFetch를 거치지 않고 직접 fetch (무한 재귀 방지)
			const res = await fetch("/api/auth/refresh", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ refreshToken }),
			});

			if (!res.ok) return false;

			const json: ApiResponse<{ accessToken: string; refreshToken: string }> = await res.json();
			if (json.success && json.data) {
				setTokens(json.data.accessToken, json.data.refreshToken);
				return true;
			}
			return false;
		} catch {
			return false;
		} finally {
			isRefreshing = false;
			refreshPromise = null;
		}
	})();

	return refreshPromise;
}

/** 공통 fetch 래퍼 — 자동 인증 헤더 + 401 재시도 */
async function apiFetch<T>(
	path: string,
	options: RequestInit = {},
	retry = true
): Promise<ApiResponse<T>> {
	const token = getAccessToken();

	// 요청 헤더 구성 (JSON + 선택적 인증 토큰)
	const headers: Record<string, string> = {
		"Content-Type": "application/json",
		...(options.headers as Record<string, string>),
	};

	if (token) {
		headers["Authorization"] = `Bearer ${token}`;
	}

	const res = await fetch(`/api${path}`, {
		...options,
		headers,
	});

	/* 401 응답 시 토큰 갱신 후 재시도 */
	if (res.status === 401 && retry) {
		const refreshed = await tryRefreshToken();
		if (refreshed) {
			return apiFetch<T>(path, options, false);
		}
		/* 갱신 실패 — 토큰 제거 (자동 로그아웃 효과) */
		clearTokens();
	}

	return res.json();
}

/** GET 요청 */
export function apiGet<T>(path: string): Promise<ApiResponse<T>> {
	return apiFetch<T>(path, { method: "GET" });
}

/** POST 요청 */
export function apiPost<T>(path: string, body?: unknown): Promise<ApiResponse<T>> {
	return apiFetch<T>(path, {
		method: "POST",
		body: body ? JSON.stringify(body) : undefined,
	});
}

/** PUT 요청 */
export function apiPut<T>(path: string, body?: unknown): Promise<ApiResponse<T>> {
	return apiFetch<T>(path, {
		method: "PUT",
		body: body ? JSON.stringify(body) : undefined,
	});
}

/** DELETE 요청 */
export function apiDelete<T>(path: string): Promise<ApiResponse<T>> {
	return apiFetch<T>(path, { method: "DELETE" });
}
