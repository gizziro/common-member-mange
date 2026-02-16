"use client";

import {
  createContext,
  useContext,
  useState,
  useRef,
  useEffect,
  useCallback,
  type ReactNode,
  type HTMLAttributes,
} from "react";
import { cn } from "@/lib/cn";

/* ===========================
 * Dropdown Context
 * =========================== */

interface DropdownContextValue {
  /** 드롭다운 열림 여부 */
  open: boolean;
  /** 드롭다운 열기/닫기 토글 */
  toggle: () => void;
  /** 드롭다운 닫기 */
  close: () => void;
}

const DropdownContext = createContext<DropdownContextValue | null>(null);

function useDropdown() {
  const ctx = useContext(DropdownContext);
  if (!ctx) throw new Error("Dropdown 하위 컴포넌트는 Dropdown 안에서 사용해야 합니다.");
  return ctx;
}

/* ===========================
 * Dropdown (루트)
 * =========================== */

export interface DropdownProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

/**
 * Dropdown — BS5 `.dropdown` 대체
 * Compound 패턴: Dropdown, DropdownTrigger, DropdownMenu, DropdownItem, DropdownDivider
 */
export function Dropdown({ children, className, ...props }: DropdownProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const toggle = useCallback(() => setOpen((prev) => !prev), []);
  const close = useCallback(() => setOpen(false), []);

  /* 바깥 클릭 감지 */
  useEffect(() => {
    if (!open) return;

    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  /* Escape 키 닫기 */
  useEffect(() => {
    if (!open) return;

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open]);

  return (
    <DropdownContext.Provider value={{ open, toggle, close }}>
      <div ref={ref} className={cn("relative inline-block", className)} {...props}>
        {children}
      </div>
    </DropdownContext.Provider>
  );
}

/* ===========================
 * DropdownTrigger
 * =========================== */

export interface DropdownTriggerProps extends HTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
}

/** 드롭다운을 열고 닫는 트리거 버튼 */
export function DropdownTrigger({ children, className, onClick, ...props }: DropdownTriggerProps) {
  const { toggle } = useDropdown();

  return (
    <button
      type="button"
      className={cn("inline-flex items-center", className)}
      onClick={(e) => {
        toggle();
        onClick?.(e);
      }}
      {...props}
    >
      {children}
    </button>
  );
}

/* ===========================
 * DropdownMenu
 * =========================== */

export interface DropdownMenuProps extends HTMLAttributes<HTMLDivElement> {
  /** 정렬 방향 */
  align?: "left" | "right";
  children: ReactNode;
}

/** 드롭다운 메뉴 컨테이너 */
export function DropdownMenu({
  align = "left",
  children,
  className,
  ...props
}: DropdownMenuProps) {
  const { open } = useDropdown();

  if (!open) return null;

  return (
    <div
      className={cn(
        "absolute z-50 mt-1 min-w-48 rounded-md border border-gray-200 bg-white py-1 shadow-lg",
        align === "right" ? "right-0" : "left-0",
        className
      )}
      role="menu"
      {...props}
    >
      {children}
    </div>
  );
}

/* ===========================
 * DropdownItem
 * =========================== */

export interface DropdownItemProps extends HTMLAttributes<HTMLButtonElement> {
  /** 비활성 상태 */
  disabled?: boolean;
  children: ReactNode;
}

/** 드롭다운 메뉴 아이템 */
export function DropdownItem({
  disabled = false,
  children,
  className,
  onClick,
  ...props
}: DropdownItemProps) {
  const { close } = useDropdown();

  return (
    <button
      type="button"
      role="menuitem"
      disabled={disabled}
      className={cn(
        "flex w-full items-center gap-2 px-4 py-2 text-sm text-gray-700",
        "hover:bg-gray-50 hover:text-gray-900 transition-colors",
        "disabled:opacity-50 disabled:pointer-events-none",
        className
      )}
      onClick={(e) => {
        onClick?.(e);
        close();
      }}
      {...props}
    >
      {children}
    </button>
  );
}

/* ===========================
 * DropdownDivider
 * =========================== */

/** 드롭다운 구분선 */
export function DropdownDivider({ className }: { className?: string }) {
  return <hr className={cn("my-1 border-gray-200", className)} role="separator" />;
}
