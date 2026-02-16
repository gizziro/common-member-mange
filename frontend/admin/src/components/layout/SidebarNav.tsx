"use client";

import { useState, useCallback } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/cn";
import { useSidebar } from "@/contexts/SidebarContext";
import { DotsThree } from "@phosphor-icons/react";
import type { SidebarNavItem } from "@/types/components";

/* ===========================
 * SidebarNav 컴포넌트
 * =========================== */

export interface SidebarNavProps {
  /** 네비게이션 아이템 목록 */
  items: SidebarNavItem[];
  /** 아코디언 모드 — 한 번에 하나의 서브메뉴만 열림 */
  accordion?: boolean;
  /** 사이드바 테마 (부모에서 전달) */
  variant?: "dark" | "light";
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * SidebarNav — 재귀 렌더링으로 4단계 깊이까지 지원
 * usePathname으로 현재 라우트 자동 하이라이트
 */
export function SidebarNav({
  items,
  accordion = false,
  variant = "dark",
  className,
}: SidebarNavProps) {
  const { state, isUnfolded } = useSidebar();
  const isMini = state === "collapsed" && !isUnfolded;

  return (
    <nav className={cn("px-3 py-2", isMini && "px-1.5", className)}>
      <ul className="space-y-0.5">
        <NavGroup items={items} accordion={accordion} variant={variant} depth={0} isMini={isMini} />
      </ul>
    </nav>
  );
}

/* ===========================
 * NavGroup — 재귀 렌더링 내부 컴포넌트
 * =========================== */

interface NavGroupProps {
  items: SidebarNavItem[];
  accordion: boolean;
  variant: "dark" | "light";
  depth: number;
  isMini?: boolean;
}

function NavGroup({ items, accordion, variant, depth, isMini = false }: NavGroupProps) {
  const [openIndex, setOpenIndex] = useState<number | null>(null);
  const pathname = usePathname();

  const handleToggle = useCallback(
    (index: number) => {
      setOpenIndex((prev) => (prev === index ? null : index));
    },
    []
  );

  return (
    <>
      {items.map((item, index) => {
        /* 구분선 */
        if (item.type === "divider") {
          return (
            <li key={index} className="my-2">
              <hr className={cn(variant === "dark" ? "border-white/10" : "border-gray-200")} />
            </li>
          );
        }

        /* 헤더 — 미니모드에서 dots-three 아이콘 표시 */
        if (item.type === "header") {
          return (
            <li key={index} className={cn("px-3 pt-4 pb-1", index === 0 && "pt-0")}>
              {isMini ? (
                <div className="flex justify-center">
                  <DotsThree
                    size={16}
                    weight="bold"
                    className={cn(variant === "dark" ? "text-white/40" : "text-gray-400")}
                  />
                </div>
              ) : (
                <span className={cn(
                  "text-[0.6875rem] font-semibold uppercase tracking-wider",
                  variant === "dark" ? "text-white/40" : "text-gray-400"
                )}>
                  {item.label}
                </span>
              )}
            </li>
          );
        }

        /* 링크 아이템 */
        const hasChildren = item.children && item.children.length > 0;
        const isOpen = openIndex === index;
        const isActive = item.href ? pathname === item.href : false;
        const isChildActive = hasChildren ? isPathInChildren(pathname, item.children!) : false;

        return (
          <NavItem
            key={index}
            item={item}
            variant={variant}
            depth={depth}
            isMini={isMini}
            isOpen={isOpen || isChildActive}
            isActive={isActive}
            isChildActive={isChildActive}
            hasChildren={!!hasChildren}
            onToggle={() => handleToggle(index)}
            accordion={accordion}
          />
        );
      })}
    </>
  );
}

/* ===========================
 * NavItem — 개별 네비게이션 아이템
 * =========================== */

interface NavItemProps {
  item: SidebarNavItem;
  variant: "dark" | "light";
  depth: number;
  isMini: boolean;
  isOpen: boolean;
  isActive: boolean;
  isChildActive: boolean;
  hasChildren: boolean;
  onToggle: () => void;
  accordion: boolean;
}

function NavItem({
  item,
  variant,
  depth,
  isMini,
  isOpen,
  isActive,
  isChildActive,
  hasChildren,
  onToggle,
  accordion,
}: NavItemProps) {
  const isDark = variant === "dark";

  if (isMini && depth > 0) return null;

  const linkClasses = cn(
    "flex items-center rounded-md text-sm transition-colors",
    isMini ? "justify-center px-0 py-2" : "gap-3 px-3 py-2",
    !isMini && depth > 0 && "pl-9",
    !isMini && depth > 1 && "pl-12",
    !isMini && depth > 2 && "pl-16",
    isActive &&
      (isDark
        ? "bg-sidebar-dark-nav-active-bg text-sidebar-dark-nav-active-color"
        : "bg-primary/10 text-primary"),
    !isActive && isChildActive && (isDark ? "text-white" : "text-primary"),
    !isActive && !isChildActive &&
      (isDark
        ? "text-sidebar-dark-nav-link hover:text-white hover:bg-sidebar-dark-nav-hover-bg"
        : "text-gray-700 hover:text-gray-900 hover:bg-gray-100"),
    item.disabled && "opacity-50 pointer-events-none"
  );

  return (
    <li>
      {hasChildren ? (
        <>
          {isMini ? (
            <Link href={item.href ?? "#"} className={cn(linkClasses, "w-full")}>
              {item.icon && <span className="shrink-0 text-base">{item.icon}</span>}
            </Link>
          ) : (
            <button
              type="button"
              onClick={onToggle}
              className={cn(linkClasses, "w-full justify-between")}
            >
              <span className="flex items-center gap-3 min-w-0">
                {item.icon && <span className="shrink-0 text-base">{item.icon}</span>}
                <span className="truncate">{item.label}</span>
              </span>
              <span className="flex items-center gap-2">
                {item.badge}
                <svg
                  className={cn("h-4 w-4 shrink-0 transition-transform duration-200", isOpen && "rotate-90")}
                  viewBox="0 0 16 16"
                  fill="currentColor"
                >
                  <path d="M6 4l4 4-4 4" stroke="currentColor" strokeWidth="1.5" fill="none" />
                </svg>
              </span>
            </button>
          )}

          {!isMini && (
            <div
              className={cn(
                "overflow-hidden transition-all duration-200",
                isOpen ? "max-h-[2000px] opacity-100" : "max-h-0 opacity-0"
              )}
            >
              <ul className="mt-0.5 space-y-0.5">
                <NavGroup items={item.children!} accordion={accordion} variant={variant} depth={depth + 1} />
              </ul>
            </div>
          )}
        </>
      ) : (
        <Link href={item.href ?? "#"} className={linkClasses}>
          {item.icon && <span className="shrink-0 text-base">{item.icon}</span>}
          {!isMini && <span className="truncate">{item.label}</span>}
          {!isMini && item.badge && <span className="ml-auto">{item.badge}</span>}
        </Link>
      )}
    </li>
  );
}

/* ===========================
 * 유틸: 자식 경로 확인
 * =========================== */

function isPathInChildren(pathname: string, children: SidebarNavItem[]): boolean {
  return children.some((child) => {
    if (child.href && pathname.startsWith(child.href)) return true;
    if (child.children) return isPathInChildren(pathname, child.children);
    return false;
  });
}
