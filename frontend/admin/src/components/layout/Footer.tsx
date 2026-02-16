import { cn } from "@/lib/cn";

/* ===========================
 * Footer 컴포넌트 — 관리자용
 * =========================== */

export interface FooterProps {
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * Footer — 저작권 표시
 */
export function Footer({ className }: FooterProps) {
  return (
    <footer
      className={cn(
        "border-t border-gray-200 text-sm text-gray-500",
        className
      )}
    >
      <div className="flex items-center justify-between px-5 py-3">
        <span>
          &copy; {new Date().getFullYear()} Admin Panel
        </span>
      </div>
    </footer>
  );
}
