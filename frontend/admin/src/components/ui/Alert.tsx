"use client";

import { useState, type ReactNode } from "react";
import { cn } from "@/lib/cn";
import type { ColorVariant } from "@/types/components";

/* ===========================
 * 변형별 Tailwind 클래스 맵
 * =========================== */

const variantClasses: Record<ColorVariant, string> = {
  primary: "bg-primary/10 text-primary border-primary/20",
  secondary: "bg-secondary/10 text-secondary border-secondary/20",
  success: "bg-success/10 text-success border-success/20",
  danger: "bg-danger/10 text-danger border-danger/20",
  warning: "bg-warning/10 text-warning border-warning/20",
  info: "bg-info/10 text-info border-info/20",
  light: "bg-gray-50 text-gray-700 border-gray-200",
  dark: "bg-dark/10 text-gray-900 border-dark/20",
};

/* ===========================
 * Alert 컴포넌트
 * =========================== */

export interface AlertProps {
  /** 색상 변형 */
  variant?: ColorVariant;
  /** 닫기 버튼 표시 */
  dismissible?: boolean;
  /** 추가 CSS 클래스 */
  className?: string;
  /** 자식 요소 */
  children: ReactNode;
}

/**
 * Alert — BS5 `.alert .alert-{color}` 대체
 */
export function Alert({
  variant = "primary",
  dismissible = false,
  className,
  children,
}: AlertProps) {
  const [visible, setVisible] = useState(true);

  if (!visible) return null;

  return (
    <div
      role="alert"
      className={cn(
        "relative rounded-md border px-4 py-3 text-sm",
        variantClasses[variant],
        className
      )}
    >
      {children}

      {/* 닫기 버튼 */}
      {dismissible && (
        <button
          type="button"
          onClick={() => setVisible(false)}
          className="absolute right-2 top-2 inline-flex items-center justify-center w-6 h-6 rounded hover:bg-black/10 transition-colors"
          aria-label="닫기"
        >
          <svg className="h-3.5 w-3.5" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M4 4l8 8M12 4l-8 8" />
          </svg>
        </button>
      )}
    </div>
  );
}
