"use client";

import { useState, useRef, useEffect } from "react";
import { cn } from "@/lib/cn";
import { useSidebar } from "@/contexts/SidebarContext";
import { useAuth } from "@/contexts/AuthContext";
import { List, SignOut, User, GearSix } from "@phosphor-icons/react";
import Link from "next/link";
import type { ReactNode } from "react";

/* ===========================
 * Navbar 컴포넌트 — 관리자용
 * =========================== */

export interface NavbarProps {
  /** 네비바 테마 */
  variant?: "dark" | "light";
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * Navbar — 관리자 상단 네비게이션 바
 * 사이드바 토글 | 로고 | 우측(유저 드롭다운)
 */
export function Navbar({ variant = "dark", className }: NavbarProps) {
  const { toggleMobile } = useSidebar();
  const { user, logout } = useAuth();
  const [userOpen, setUserOpen] = useState(false);
  const userRef = useRef<HTMLDivElement>(null);

  const isDark = variant === "dark";

  /* 유저 드롭다운 바깥 클릭 닫기 */
  useEffect(() => {
    if (!userOpen) return;
    function handler(e: MouseEvent) {
      if (userRef.current && !userRef.current.contains(e.target as Node)) {
        setUserOpen(false);
      }
    }
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [userOpen]);

  /* 로그아웃 핸들러 */
  const handleLogout = async () => {
    setUserOpen(false);
    await logout();
  };

  return (
    <header
      className={cn(
        "shrink-0 z-20",
        isDark
          ? "bg-dark text-white border-b border-white/10"
          : "bg-white text-gray-900 border-b border-gray-200 shadow-sm",
        className
      )}
    >
      <div className="flex h-navbar items-center px-4">
        {/* 모바일 사이드바 토글 */}
        <button
          type="button"
          onClick={toggleMobile}
          className={cn(
            "inline-flex items-center justify-center w-9 h-9 rounded-md lg:hidden mr-2",
            isDark ? "hover:bg-white/10" : "hover:bg-gray-100"
          )}
          aria-label="사이드바 토글"
        >
          <List size={20} weight="bold" />
        </button>

        {/* 로고 */}
        <Link href="/dashboard" className="inline-flex items-center shrink-0 mr-4">
          <span className={cn(
            "text-lg font-bold",
            isDark ? "text-white" : "text-gray-900"
          )}>
            Admin
          </span>
        </Link>

        {/* 가변 스페이서 */}
        <div className="flex-1" />

        {/* 유저 드롭다운 */}
        <div ref={userRef} className="relative">
          <button
            type="button"
            onClick={() => setUserOpen(!userOpen)}
            className={cn(
              "inline-flex items-center gap-2 rounded-md px-2 py-1.5 transition-colors",
              isDark ? "hover:bg-white/10" : "hover:bg-gray-100"
            )}
          >
            {/* 아바타 아이콘 */}
            <div className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-primary/20 text-primary">
              <User size={16} weight="bold" />
            </div>
            <span className={cn(
              "hidden sm:inline-block text-sm",
              isDark ? "text-white" : "text-gray-700"
            )}>
              {user?.username ?? "관리자"}
            </span>
          </button>

          {/* 드롭다운 메뉴 */}
          {userOpen && (
            <div className="absolute right-0 top-full z-50 mt-1 min-w-48 rounded-md border border-gray-200 bg-white py-1 shadow-lg">
              {/* 사용자 정보 */}
              <div className="px-4 py-2 border-b border-gray-100">
                <p className="text-sm font-medium text-gray-900">{user?.username}</p>
                <p className="text-xs text-gray-500">{user?.email}</p>
              </div>

              {/* 메뉴 항목 */}
              <button
                type="button"
                onClick={handleLogout}
                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-danger hover:bg-gray-50 transition-colors"
              >
                <SignOut size={16} />
                로그아웃
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
