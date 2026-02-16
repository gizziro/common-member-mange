"use client";

import { useEffect, useCallback, type ReactNode } from "react";
import { cn } from "@/lib/cn";

/* ===========================
 * Modal 컴포넌트
 * =========================== */

export interface ModalProps {
  /** 모달 열림 상태 */
  open: boolean;
  /** 모달 닫기 콜백 */
  onClose: () => void;
  /** 모달 제목 */
  title?: string;
  /** 모달 크기 */
  size?: "sm" | "md" | "lg" | "xl";
  /** 백드롭 클릭으로 닫기 */
  closeOnBackdrop?: boolean;
  /** 추가 CSS 클래스 */
  className?: string;
  /** 모달 콘텐츠 */
  children: ReactNode;
  /** 푸터 콘텐츠 */
  footer?: ReactNode;
}

/** 크기별 너비 클래스 */
const sizeClasses = {
  sm: "max-w-sm",
  md: "max-w-lg",
  lg: "max-w-2xl",
  xl: "max-w-4xl",
};

/**
 * Modal — BS5 `.modal` 대체
 * 백드롭 + 센터 정렬 + Escape 닫기
 */
export function Modal({
  open,
  onClose,
  title,
  size = "md",
  closeOnBackdrop = true,
  className,
  children,
  footer,
}: ModalProps) {
  /* Escape 키 닫기 */
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    },
    [onClose]
  );

  useEffect(() => {
    if (!open) return;

    document.addEventListener("keydown", handleKeyDown);
    /* 스크롤 잠금 */
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [open, handleKeyDown]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* 백드롭 */}
      <div
        className="absolute inset-0 bg-black/50 transition-opacity"
        onClick={closeOnBackdrop ? onClose : undefined}
        aria-hidden="true"
      />

      {/* 모달 본체 */}
      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          "relative w-full rounded-lg bg-white shadow-xl",
          sizeClasses[size],
          className
        )}
      >
        {/* 헤더 */}
        {title && (
          <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
            <h3 className="text-base font-semibold text-gray-900">{title}</h3>
            <button
              type="button"
              onClick={onClose}
              className="inline-flex items-center justify-center w-8 h-8 rounded text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
              aria-label="닫기"
            >
              <svg className="h-4 w-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M4 4l8 8M12 4l-8 8" />
              </svg>
            </button>
          </div>
        )}

        {/* 본문 */}
        <div className="px-5 py-4">{children}</div>

        {/* 푸터 */}
        {footer && (
          <div className="flex items-center justify-end gap-2 border-t border-gray-200 px-5 py-3">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}
