"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from "react";
import { useRouter, usePathname } from "next/navigation";
import {
  apiGet,
  apiPost,
  setTokens,
  clearTokens,
  getAccessToken,
  type ApiResponse,
} from "@/lib/api";

/* ===========================
 * 타입 정의
 * =========================== */

/** 사용자 정보 */
interface User {
  id: string;
  userId: string;
  username: string;
  email: string;
}

/** 로그인 응답 */
interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

/** /auth/me 응답 */
interface UserMeResponse {
  id: string;
  userId: string;
  username: string;
  email: string;
}

/** 시스템 초기화 상태 응답 */
interface SetupStatusResponse {
  initialized: boolean;
}

/** AuthContext에서 제공하는 값 */
interface AuthContextValue {
  /** 현재 사용자 정보 */
  user: User | null;
  /** 로딩 상태 */
  isLoading: boolean;
  /** 시스템 초기화 여부 */
  isInitialized: boolean | null;
  /** 로그인 */
  login: (userId: string, password: string) => Promise<ApiResponse<LoginResponse>>;
  /** 로그아웃 */
  logout: () => Promise<void>;
  /** 시스템 초기화 완료 알림 (셋업 페이지에서 호출) */
  markInitialized: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/* ===========================
 * AuthProvider
 * =========================== */

/** 인증 없이 접근 가능한 경로 */
const PUBLIC_PATHS = ["/login", "/setup"];

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isInitialized, setIsInitialized] = useState<boolean | null>(null);
  const router = useRouter();
  const pathname = usePathname();

  /* 앱 마운트 시 초기화 체크 + 사용자 확인 */
  useEffect(() => {
    async function init() {
      try {
        /* 1. 시스템 초기화 상태 확인 */
        const setupRes = await apiGet<SetupStatusResponse>("/setup/status");
        const initialized = setupRes.success && setupRes.data?.initialized === true;
        setIsInitialized(initialized);

        /* 미초기화 → 셋업 페이지로 */
        if (!initialized) {
          if (pathname !== "/setup") {
            router.replace("/setup");
          }
          setIsLoading(false);
          return;
        }

        /* 2. 토큰 있으면 사용자 정보 조회 */
        const token = getAccessToken();
        if (token) {
          const meRes = await apiGet<UserMeResponse>("/auth/me");
          if (meRes.success && meRes.data) {
            setUser({
              id: meRes.data.id,
              userId: meRes.data.userId,
              username: meRes.data.username,
              email: meRes.data.email,
            });
          } else {
            /* 토큰 만료/유효하지 않음 */
            clearTokens();
          }
        }
      } catch {
        /* 네트워크 오류 등 */
        clearTokens();
      } finally {
        setIsLoading(false);
      }
    }

    init();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /* 라우트 보호: 로딩 완료 후 리다이렉트 */
  useEffect(() => {
    if (isLoading) return;

    const isPublicPath = PUBLIC_PATHS.some((p) => pathname.startsWith(p));

    /* 미초기화 → 셋업 */
    if (isInitialized === false && pathname !== "/setup") {
      router.replace("/setup");
      return;
    }

    /* 초기화 완료 + 셋업 페이지 접근 → 대시보드 */
    if (isInitialized === true && pathname === "/setup") {
      router.replace("/dashboard");
      return;
    }

    /* 미로그인 + 비공개 경로 → 로그인 */
    if (!user && !isPublicPath) {
      router.replace("/login");
      return;
    }

    /* 로그인 상태 + 로그인 페이지 → 대시보드 */
    if (user && pathname === "/login") {
      router.replace("/dashboard");
      return;
    }
  }, [isLoading, isInitialized, user, pathname, router]);

  /* 로그인 */
  const login = useCallback(
    async (userId: string, password: string): Promise<ApiResponse<LoginResponse>> => {
      const res = await apiPost<LoginResponse>("/auth/login", { userId, password });

      if (res.success && res.data) {
        setTokens(res.data.accessToken, res.data.refreshToken);

        /* 사용자 정보 조회 */
        const meRes = await apiGet<UserMeResponse>("/auth/me");
        if (meRes.success && meRes.data) {
          setUser({
            id: meRes.data.id,
            userId: meRes.data.userId,
            username: meRes.data.username,
            email: meRes.data.email,
          });
        }

        router.replace("/dashboard");
      }

      return res;
    },
    [router]
  );

  /* 로그아웃 */
  const logout = useCallback(async () => {
    try {
      await apiPost("/auth/logout");
    } catch {
      /* 로그아웃 API 실패해도 클라이언트 토큰은 제거 */
    }
    clearTokens();
    setUser(null);
    router.replace("/login");
  }, [router]);

  /* 시스템 초기화 완료 알림 (셋업 성공 후 호출) */
  const markInitialized = useCallback(() => {
    setIsInitialized(true);
  }, []);

  return (
    <AuthContext.Provider value={{ user, isLoading, isInitialized, login, logout, markInitialized }}>
      {children}
    </AuthContext.Provider>
  );
}

/** 인증 상태에 접근하는 훅 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth는 AuthProvider 안에서 사용해야 합니다.");
  }
  return context;
}
