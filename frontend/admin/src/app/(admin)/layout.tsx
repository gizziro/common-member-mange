"use client";

import { useAuth } from "@/contexts/AuthContext";
import { ThemeProvider } from "@/contexts/ThemeContext";
import { SidebarProvider } from "@/contexts/SidebarContext";
import { Navbar } from "@/components/layout/Navbar";
import { Sidebar } from "@/components/layout/Sidebar";
import { SidebarNav } from "@/components/layout/SidebarNav";
import { ContentWrapper } from "@/components/layout/ContentWrapper";
import { Footer } from "@/components/layout/Footer";
import { adminNavItems } from "@/lib/navigation";
import { Toaster } from "@/components/ui/sonner";
import type { ReactNode } from "react";

/** 관리자 페이지 레이아웃 — Limitless Layout 1 쉘 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth();

  /* 로딩 중 — 스피너 */
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  /* 미로그인 — AuthContext에서 리다이렉트 처리하므로 빈 화면 */
  if (!user) {
    return null;
  }

  return (
    <ThemeProvider defaultMode="light">
      <SidebarProvider defaultState="expanded">
        <div className="flex h-screen flex-col">
          {/* 상단 Navbar (다크) */}
          <Navbar variant="dark" />

          <div className="flex flex-1 overflow-hidden">
            {/* 사이드바 */}
            <Sidebar variant="dark">
              <SidebarNav items={adminNavItems} variant="dark" />
            </Sidebar>

            {/* 메인 콘텐츠 */}
            <ContentWrapper>
              {children}
              <Footer />
            </ContentWrapper>

            {/* Toast 알림 렌더링 영역 */}
            <Toaster richColors position="top-right" />
          </div>
        </div>
      </SidebarProvider>
    </ThemeProvider>
  );
}
