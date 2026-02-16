"use client";

import { cn } from "@/lib/cn";
import { useSidebar } from "@/contexts/SidebarContext";
import { ArrowsLeftRight, X } from "@phosphor-icons/react";
import { Backdrop } from "./Backdrop";
import type { ReactNode } from "react";

/* ===========================
 * Sidebar 컴포넌트 — 관리자용
 * =========================== */

export interface SidebarProps {
  /** 사이드바 테마 */
  variant?: "dark" | "light";
  /** 사이드바 콘텐츠 (SidebarNav 등) */
  children: ReactNode;
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * Sidebar — 3가지 모드: Desktop Expanded, Desktop Collapsed (mini), Mobile Slide-in
 */
export function Sidebar({
  variant = "dark",
  children,
  className,
}: SidebarProps) {
  const { state, toggle, mobileOpen, closeMobile, isUnfolded, handleMouseEnter, handleMouseLeave } = useSidebar();

  const isDark = variant === "dark";
  const isCollapsed = state === "collapsed";
  const showExpanded = isCollapsed ? isUnfolded : true;

  /* 버튼 클래스 */
  const btnClass = cn(
    "inline-flex items-center justify-center w-8 h-8 rounded-full transition-colors",
    isDark
      ? "text-white/70 bg-white/10 hover:text-white hover:bg-white/20 active:bg-white/25"
      : "text-gray-400 bg-gray-100 hover:text-gray-600 hover:bg-gray-200"
  );

  /* 헤더 렌더링 */
  const headerContent = (
    <div
      className={cn(
        "flex items-center justify-center py-3 shrink-0",
        showExpanded ? "px-5" : "px-2",
        isDark ? "border-b border-white/10" : "border-b border-gray-200"
      )}
    >
      {/* Navigation 텍스트 — 축소 시 숨김 */}
      {showExpanded && (
        <h5 className={cn(
          "text-lg font-medium flex-1 my-auto",
          isDark ? "text-white/70" : "text-gray-500"
        )}>
          Navigation
        </h5>
      )}

      <div className="flex items-center gap-0.5">
        {/* 리사이즈 버튼 — 데스크톱만 */}
        <button
          type="button"
          onClick={toggle}
          className={cn(btnClass, "hidden lg:inline-flex")}
          aria-label="사이드바 리사이즈"
        >
          <ArrowsLeftRight size={16} />
        </button>

        {/* 닫기 버튼 — 모바일만 */}
        <button
          type="button"
          onClick={closeMobile}
          className={cn(btnClass, "lg:hidden")}
          aria-label="사이드바 닫기"
        >
          <X size={16} />
        </button>
      </div>
    </div>
  );

  return (
    <>
      {/* 모바일 백드롭 */}
      <Backdrop visible={mobileOpen} onClick={closeMobile} />

      {/* 사이드바 본체 — 데스크톱 */}
      <aside
        onMouseEnter={isCollapsed ? handleMouseEnter : undefined}
        onMouseLeave={isCollapsed ? handleMouseLeave : undefined}
        className={cn(
          "flex flex-col shrink-0 overflow-hidden transition-all duration-300",
          isDark ? "bg-sidebar-dark-bg text-white" : "bg-sidebar-bg border-r border-gray-200",
          "hidden lg:flex",
          isCollapsed && !isUnfolded ? "w-sidebar-mini" : "w-sidebar",
          isCollapsed && isUnfolded && "absolute left-0 top-0 z-30 h-full shadow-lg",
          className
        )}
      >
        {headerContent}
        <div className="flex-1 overflow-y-auto overflow-x-hidden">
          {children}
        </div>
      </aside>

      {/* 모바일 사이드바 — 오버레이 슬라이드인 */}
      <aside
        className={cn(
          "fixed left-0 top-0 z-50 flex h-full flex-col overflow-hidden transition-transform duration-300 lg:hidden",
          "w-sidebar",
          isDark ? "bg-sidebar-dark-bg text-white" : "bg-sidebar-bg",
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        {headerContent}
        <div className="flex-1 overflow-y-auto">
          {children}
        </div>
      </aside>
    </>
  );
}
