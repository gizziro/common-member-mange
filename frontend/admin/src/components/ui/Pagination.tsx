"use client";

import { cn } from "@/lib/cn";

/* ===========================
 * Pagination 컴포넌트
 * =========================== */

export interface PaginationProps {
  /** 현재 페이지 (0-based) */
  page: number;
  /** 전체 페이지 수 */
  totalPages: number;
  /** 페이지 변경 콜백 */
  onPageChange: (page: number) => void;
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * Pagination — 페이지네이션 UI
 * 이전/다음 버튼 + 페이지 번호 표시
 */
export function Pagination({
  page,
  totalPages,
  onPageChange,
  className,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  /* 표시할 페이지 번호 계산 (최대 5개) */
  const pages = getVisiblePages(page, totalPages);

  const btnClass =
    "inline-flex items-center justify-center min-w-[2rem] h-8 px-2 text-sm rounded transition-colors";

  return (
    <nav className={cn("flex items-center gap-1", className)} aria-label="페이지네이션">
      {/* 이전 */}
      <button
        type="button"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        className={cn(btnClass, "text-gray-500 hover:bg-gray-100 disabled:opacity-50 disabled:pointer-events-none")}
        aria-label="이전 페이지"
      >
        <svg className="h-4 w-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M10 4l-4 4 4 4" />
        </svg>
      </button>

      {/* 페이지 번호 */}
      {pages.map((p, i) =>
        p === -1 ? (
          <span key={`dots-${i}`} className="px-1 text-gray-400">...</span>
        ) : (
          <button
            key={p}
            type="button"
            onClick={() => onPageChange(p)}
            className={cn(
              btnClass,
              p === page
                ? "bg-primary text-white"
                : "text-gray-700 hover:bg-gray-100"
            )}
          >
            {p + 1}
          </button>
        )
      )}

      {/* 다음 */}
      <button
        type="button"
        disabled={page === totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className={cn(btnClass, "text-gray-500 hover:bg-gray-100 disabled:opacity-50 disabled:pointer-events-none")}
        aria-label="다음 페이지"
      >
        <svg className="h-4 w-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M6 4l4 4-4 4" />
        </svg>
      </button>
    </nav>
  );
}

/** 표시할 페이지 번호 배열 계산 (-1은 dots) */
function getVisiblePages(current: number, total: number): number[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i);
  }

  if (current <= 2) {
    return [0, 1, 2, 3, -1, total - 1];
  }

  if (current >= total - 3) {
    return [0, -1, total - 4, total - 3, total - 2, total - 1];
  }

  return [0, -1, current - 1, current, current + 1, -1, total - 1];
}
