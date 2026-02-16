"use client";

import { createContext, useContext, useState, useCallback, useRef, type ReactNode } from "react";

/** 사이드바 상태 타입 */
type SidebarState = "expanded" | "collapsed" | "hidden";

/** SidebarContext에서 제공하는 값 */
interface SidebarContextValue {
  /** 현재 사이드바 상태 */
  state: SidebarState;
  /** 모바일에서 사이드바 열림 여부 */
  mobileOpen: boolean;
  /** 축소 상태에서 hover로 임시 펼침 여부 */
  isUnfolded: boolean;
  /** 사이드바 축소/확대 토글 */
  toggle: () => void;
  /** 모바일 사이드바 열림/닫힘 토글 */
  toggleMobile: () => void;
  /** 사이드바 상태를 직접 설정 */
  setState: (state: SidebarState) => void;
  /** 모바일 사이드바 닫기 */
  closeMobile: () => void;
  /** hover-unfold: 마우스 진입 핸들러 */
  handleMouseEnter: () => void;
  /** hover-unfold: 마우스 이탈 핸들러 */
  handleMouseLeave: () => void;
}

const SidebarContext = createContext<SidebarContextValue | null>(null);

/** SidebarProvider props */
interface SidebarProviderProps {
  children: ReactNode;
  /** 초기 사이드바 상태 (기본값: expanded) */
  defaultState?: SidebarState;
}

/** 사이드바 상태를 관리하는 Provider */
export function SidebarProvider({ children, defaultState = "expanded" }: SidebarProviderProps) {
  const [state, setState] = useState<SidebarState>(defaultState);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [isUnfolded, setIsUnfolded] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  /* 데스크톱 사이드바 토글: expanded ↔ collapsed */
  const toggle = useCallback(() => {
    setState((prev) => (prev === "expanded" ? "collapsed" : "expanded"));
  }, []);

  /* 모바일 사이드바 토글 */
  const toggleMobile = useCallback(() => {
    setMobileOpen((prev) => !prev);
  }, []);

  /* 모바일 사이드바 닫기 */
  const closeMobile = useCallback(() => {
    setMobileOpen(false);
  }, []);

  /* hover-unfold: 마우스 진입 — 150ms 딜레이 후 펼침 */
  const handleMouseEnter = useCallback(() => {
    timerRef.current = setTimeout(() => {
      setIsUnfolded(true);
    }, 150);
  }, []);

  /* hover-unfold: 마우스 이탈 — 타이머 취소 + 즉시 접힘 */
  const handleMouseLeave = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    setIsUnfolded(false);
  }, []);

  return (
    <SidebarContext.Provider value={{
      state, mobileOpen, isUnfolded,
      toggle, toggleMobile, setState, closeMobile,
      handleMouseEnter, handleMouseLeave,
    }}>
      {children}
    </SidebarContext.Provider>
  );
}

/** 사이드바 상태에 접근하는 훅 */
export function useSidebar() {
  const context = useContext(SidebarContext);
  if (!context) {
    throw new Error("useSidebar는 SidebarProvider 안에서 사용해야 합니다.");
  }
  return context;
}
