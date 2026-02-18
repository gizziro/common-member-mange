"use client";

import { useCallback, useEffect, useState } from "react";
import { apiGet, apiPost, getAccessToken, clearTokens } from "@/lib/api";
import type { UserMeResponse } from "@/types/api";

// 인증 상태 반환 타입
interface AuthState {
	// 현재 사용자 정보 (미인증 시 null)
	user: UserMeResponse | null;
	// 인증 상태 로딩 중 여부
	loading: boolean;
	// 로그아웃 함수
	logout: () => Promise<void>;
	// 인증 상태 새로고침 함수
	refresh: () => Promise<void>;
}

// 인증 상태 관리 훅 (localStorage 토큰 + /auth/me 검증)
export function useAuth(): AuthState {
	const [user, setUser]       = useState<UserMeResponse | null>(null);
	const [loading, setLoading] = useState(true);

	// 토큰으로 사용자 정보 조회
	const fetchUser = useCallback(async () => {
		// 토큰이 없으면 미인증 상태
		if (!getAccessToken()) {
			setUser(null);
			setLoading(false);
			return;
		}

		try {
			// 백엔드 /auth/me 호출 (토큰 자동 추가, 401 시 자동 갱신)
			const json = await apiGet<UserMeResponse>("/auth/me");

			if (json.success && json.data) {
				setUser(json.data);
			} else {
				// 토큰이 유효하지 않으면 정리
				clearTokens();
				setUser(null);
			}
		} catch {
			// 서버 연결 실패 시 토큰 정리
			clearTokens();
			setUser(null);
		} finally {
			setLoading(false);
		}
	}, []);

	// 컴포넌트 마운트 시 인증 상태 확인
	useEffect(() => {
		fetchUser();
	}, [fetchUser]);

	// 로그아웃 처리
	const logout = useCallback(async () => {
		// 백엔드 로그아웃 API 호출 (토큰 자동 추가)
		if (getAccessToken()) {
			try {
				await apiPost("/auth/logout", {});
			} catch {
				// 로그아웃 API 실패해도 로컬 토큰은 삭제
			}
		}

		// 로컬 토큰 삭제
		clearTokens();
		setUser(null);
	}, []);

	return { user, loading, logout, refresh: fetchUser };
}
