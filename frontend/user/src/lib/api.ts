import type { ApiResponse } from "@/types/api";

// Next.js rewrites가 /api/** → 백엔드로 프록시
const API_PREFIX = "/api";

// 백엔드 API JSON 요청 유틸리티
export async function apiPost<T>(
	path: string,
	body: unknown
): Promise<ApiResponse<T>> {
	const res = await fetch(`${API_PREFIX}${path}`, {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(body),
	});

	return res.json();
}
