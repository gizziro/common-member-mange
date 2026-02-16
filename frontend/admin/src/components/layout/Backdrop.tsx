"use client";

import { cn } from "@/lib/cn";

/* ===========================
 * Backdrop 컴포넌트
 * =========================== */

export interface BackdropProps {
  /** 표시 여부 */
  visible: boolean;
  /** 클릭 시 콜백 */
  onClick?: () => void;
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * Backdrop — 모바일 사이드바 뒤 반투명 오버레이
 * 클릭 시 사이드바를 닫는 용도로 사용
 */
export function Backdrop({ visible, onClick, className }: BackdropProps) {
  return (
    <div
      className={cn(
        "fixed inset-0 z-40 bg-black/50 transition-opacity duration-300",
        visible ? "opacity-100" : "opacity-0 pointer-events-none",
        className
      )}
      onClick={onClick}
      aria-hidden="true"
    />
  );
}
