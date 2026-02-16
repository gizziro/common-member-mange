"use client";

import { cn } from "@/lib/cn";
import type { ReactNode } from "react";

/* ===========================
 * PageHeader 컴포넌트 — 관리자용 (간소화)
 * =========================== */

export interface PageHeaderProps {
  /** 페이지 제목 */
  title: string;
  /** 부제목 */
  subtitle?: string;
  /** 상단 우측 액션 영역 */
  actions?: ReactNode;
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * PageHeader — 페이지 제목 + 액션 버튼
 */
export function PageHeader({
  title,
  subtitle,
  actions,
  className,
}: PageHeaderProps) {
  return (
    <div className={cn("bg-page-header-light-bg shadow-sm", className)}>
      <div className="px-5 py-3">
        <div className="flex items-center justify-between">
          <div>
            <h4 className="text-lg font-semibold text-gray-900">{title}</h4>
            {subtitle && (
              <p className="mt-0.5 text-sm text-gray-500">{subtitle}</p>
            )}
          </div>
          {actions && (
            <div className="flex items-center gap-3">{actions}</div>
          )}
        </div>
      </div>
    </div>
  );
}
