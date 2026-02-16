import type { ReactNode } from "react";

/* ===========================
 * 공통 변형 타입
 * =========================== */

/** 기본 색상 변형 */
export type ColorVariant =
  | "primary"
  | "secondary"
  | "success"
  | "danger"
  | "warning"
  | "info"
  | "light"
  | "dark";

/** 버튼 변형 — solid / outline / flat */
export type ButtonVariant =
  | ColorVariant
  | `outline-${ColorVariant}`
  | `flat-${ColorVariant}`;

/** 컴포넌트 크기 */
export type ComponentSize = "sm" | "md" | "lg";

/* ===========================
 * 네비게이션 관련 타입
 * =========================== */

/** 사이드바 네비게이션 아이템 — 재귀적 (4단계 깊이 지원) */
export interface SidebarNavItem {
  /** 아이템 타입 */
  type: "link" | "header" | "divider";
  /** 표시 라벨 */
  label?: string;
  /** 아이콘 (React 노드) */
  icon?: ReactNode;
  /** 링크 URL */
  href?: string;
  /** 현재 활성 상태 */
  active?: boolean;
  /** 비활성 상태 */
  disabled?: boolean;
  /** 뱃지 (React 노드) */
  badge?: ReactNode;
  /** 하위 메뉴 아이템 */
  children?: SidebarNavItem[];
}

/** 브레드크럼 아이템 */
export interface BreadcrumbItem {
  /** 표시 라벨 */
  label: string;
  /** 링크 URL (없으면 현재 페이지) */
  href?: string;
  /** 아이콘 (React 노드) */
  icon?: ReactNode;
}
