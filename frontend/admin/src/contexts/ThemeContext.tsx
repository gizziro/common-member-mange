"use client";

import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from "react";

/** 테마 모드 */
type ThemeMode = "light" | "dark" | "auto";

/** ThemeContext에서 제공하는 값 */
interface ThemeContextValue {
  /** 현재 테마 모드 설정값 */
  mode: ThemeMode;
  /** 실제 적용된 테마 (auto인 경우 시스템 설정 반영) */
  resolvedTheme: "light" | "dark";
  /** 테마 모드 변경 */
  setMode: (mode: ThemeMode) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

interface ThemeProviderProps {
  children: ReactNode;
  defaultMode?: ThemeMode;
}

/** 테마(라이트/다크) 상태를 관리하는 Provider */
export function ThemeProvider({ children, defaultMode = "light" }: ThemeProviderProps) {
  const [mode, setMode] = useState<ThemeMode>(defaultMode);
  const [resolvedTheme, setResolvedTheme] = useState<"light" | "dark">("light");

  /* 시스템 테마 감지 및 auto 모드 처리 */
  useEffect(() => {
    if (mode !== "auto") {
      setResolvedTheme(mode);
      return;
    }

    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    setResolvedTheme(mediaQuery.matches ? "dark" : "light");

    const handler = (e: MediaQueryListEvent) => {
      setResolvedTheme(e.matches ? "dark" : "light");
    };

    mediaQuery.addEventListener("change", handler);
    return () => mediaQuery.removeEventListener("change", handler);
  }, [mode]);

  /* HTML data-theme 속성 동기화 */
  useEffect(() => {
    document.documentElement.setAttribute("data-theme", resolvedTheme);
  }, [resolvedTheme]);

  const handleSetMode = useCallback((newMode: ThemeMode) => {
    setMode(newMode);
  }, []);

  return (
    <ThemeContext.Provider value={{ mode, resolvedTheme, setMode: handleSetMode }}>
      {children}
    </ThemeContext.Provider>
  );
}

/** 테마 상태에 접근하는 훅 */
export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme은 ThemeProvider 안에서 사용해야 합니다.");
  }
  return context;
}
