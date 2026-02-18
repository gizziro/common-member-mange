import type { ApiResponse } from "@/types/api";

// Next.js rewrites가 /api/** → 백엔드로 프록시
const API_PREFIX = "/api";

// 백엔드 API POST 요청 유틸리티
export async function apiPost<T>(
	path: string,
	body: unknown,
	token?: string
): Promise<ApiResponse<T>> {
	// 요청 헤더 구성 (JSON + 선택적 인증 토큰)
	const headers: Record<string, string> = {
		"Content-Type": "application/json",
	};
	if (token) {
		headers["Authorization"] = `Bearer ${token}`;
	}

	const res = await fetch(`${API_PREFIX}${path}`, {
		method: "POST",
		headers,
		body: JSON.stringify(body),
	});

	return res.json();
}

// 백엔드 API GET 요청 유틸리티
export async function apiGet<T>(
	path: string,
	token?: string
): Promise<ApiResponse<T>> {
	// 요청 헤더 구성 (선택적 인증 토큰)
	const headers: Record<string, string> = {};
	if (token) {
		headers["Authorization"] = `Bearer ${token}`;
	}

	const res = await fetch(`${API_PREFIX}${path}`, {
		method: "GET",
		headers,
	});

	return res.json();
}

// 백엔드 API PUT 요청 유틸리티
export async function apiPut<T>(
	path: string,
	body: unknown,
	token?: string
): Promise<ApiResponse<T>> {
	// 요청 헤더 구성 (JSON + 선택적 인증 토큰)
	const headers: Record<string, string> = {
		"Content-Type": "application/json",
	};
	if (token) {
		headers["Authorization"] = `Bearer ${token}`;
	}

	const res = await fetch(`${API_PREFIX}${path}`, {
		method: "PUT",
		headers,
		body: JSON.stringify(body),
	});

	return res.json();
}

// 백엔드 API DELETE 요청 유틸리티
export async function apiDelete<T>(
	path: string,
	token?: string
): Promise<ApiResponse<T>> {
	// 요청 헤더 구성 (선택적 인증 토큰)
	const headers: Record<string, string> = {};
	if (token) {
		headers["Authorization"] = `Bearer ${token}`;
	}

	const res = await fetch(`${API_PREFIX}${path}`, {
		method: "DELETE",
		headers,
	});

	return res.json();
}
