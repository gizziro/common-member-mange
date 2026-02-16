"use client";

import { cn } from "@/lib/cn";
import type { ReactNode } from "react";

/* ===========================
 * FormGroup 컴포넌트
 * =========================== */

export interface FormGroupProps {
  /** 라벨 텍스트 */
  label?: string;
  /** 라벨 연결 htmlFor */
  htmlFor?: string;
  /** 에러 메시지 */
  error?: string;
  /** 도움말 텍스트 */
  help?: string;
  /** 필수 표시 */
  required?: boolean;
  /** 추가 CSS 클래스 */
  className?: string;
  /** 자식 요소 (Input 등) */
  children: ReactNode;
}

/**
 * FormGroup — 라벨 + 입력필드 + 에러/도움말 메시지를 그룹핑
 */
export function FormGroup({
  label,
  htmlFor,
  error,
  help,
  required = false,
  className,
  children,
}: FormGroupProps) {
  return (
    <div className={cn("space-y-1.5", className)}>
      {/* 라벨 */}
      {label && (
        <label
          htmlFor={htmlFor}
          className="block text-sm font-medium text-gray-700"
        >
          {label}
          {required && <span className="text-danger ml-0.5">*</span>}
        </label>
      )}

      {/* 입력 필드 */}
      {children}

      {/* 에러 메시지 */}
      {error && (
        <p className="text-xs text-danger">{error}</p>
      )}

      {/* 도움말 */}
      {!error && help && (
        <p className="text-xs text-gray-500">{help}</p>
      )}
    </div>
  );
}
