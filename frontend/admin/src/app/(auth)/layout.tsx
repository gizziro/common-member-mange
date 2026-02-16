import type { ReactNode } from "react";

/** 인증 페이지 레이아웃 — 센터 정렬 미니멀 (사이드바 없음) */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100 px-4">
      {children}
    </div>
  );
}
