"use client";

import { cn } from "@/lib/cn";
import type { ReactNode } from "react";

/* ===========================
 * ContentWrapper 컴포넌트
 * =========================== */

export interface ContentWrapperProps {
  /** 자식 요소 */
  children: ReactNode;
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * ContentWrapper — BS5 `.content-wrapper .content-inner .content` 대체
 * 사이드바 옆 메인 콘텐츠 영역
 */
export function ContentWrapper({ children, className }: ContentWrapperProps) {
  return (
    <div className={cn("flex flex-1 flex-col overflow-hidden min-w-0", className)}>
      {/* 스크롤 가능한 내부 컨테이너 — 회색 배경으로 카드와 대비 */}
      <div className="flex flex-1 flex-col overflow-y-auto bg-gray-100">
        <main className="flex-1">{children}</main>
      </div>
    </div>
  );
}
