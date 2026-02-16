"use client";

/**
 * Limitless ↔ shadcn/ui 호환성 레이어
 * 기존 Limitless 커스텀 컴포넌트 API를 shadcn 컴포넌트로 매핑한다.
 * 기존 페이지(groups, login, dashboard 등)가 깨지지 않도록 호환 유지.
 * 새 페이지(users)는 shadcn 컴포넌트를 직접 import하여 사용한다.
 */

import * as React from "react";
import { cn } from "@/lib/utils";
import {
  Button as ShadcnButton,
  type buttonVariants,
} from "@/components/ui/Button";
import { Badge as ShadcnBadge } from "@/components/ui/Badge";
import {
  Card as ShadcnCard,
  CardHeader as ShadcnCardHeader,
  CardTitle as ShadcnCardTitle,
  CardContent as ShadcnCardContent,
  CardFooter as ShadcnCardFooter,
} from "@/components/ui/Card";

/* ===========================
 * CompatButton — 기존 Limitless Button API 호환
 * =========================== */

/** Limitless Button variant → shadcn variant + 추가 클래스 매핑 */
const BUTTON_VARIANT_MAP: Record<
  string,
  { variant: "default" | "destructive" | "outline" | "secondary" | "ghost" | "link"; className?: string }
> = {
  primary:          { variant: "default" },
  danger:           { variant: "destructive" },
  "flat-primary":   { variant: "ghost", className: "text-primary hover:text-primary" },
  "flat-danger":    { variant: "ghost", className: "text-destructive hover:text-destructive" },
  "flat-dark":      { variant: "ghost" },
  light:            { variant: "secondary" },
  "outline-primary": { variant: "outline" },
};

/** Limitless Button size → shadcn size 매핑 */
const BUTTON_SIZE_MAP: Record<string, "default" | "sm" | "lg" | "icon" | "icon-sm" | "xs"> = {
  sm: "sm",
  lg: "lg",
  xs: "xs",
};

export interface CompatButtonProps extends Omit<React.ComponentProps<"button">, "children"> {
  variant?: string;
  size?: string;
  loading?: boolean;
  iconOnly?: boolean;
  children?: React.ReactNode;
}

export function CompatButton({
  variant = "primary",
  size,
  loading = false,
  iconOnly = false,
  className,
  children,
  disabled,
  ...props
}: CompatButtonProps) {
  // variant 매핑
  const mapped = BUTTON_VARIANT_MAP[variant] ?? { variant: "default" as const };

  // size 매핑 (iconOnly이면 icon-sm 사용)
  let mappedSize = size ? BUTTON_SIZE_MAP[size] ?? "default" : "default";
  if (iconOnly) {
    mappedSize = size === "sm" ? "icon-sm" as "icon" : "icon";
  }

  return (
    <ShadcnButton
      variant={mapped.variant}
      size={mappedSize as "default" | "sm" | "lg" | "icon" | "xs"}
      disabled={disabled || loading}
      className={cn(mapped.className, className)}
      {...props}
    >
      {loading && (
        <span className="mr-1.5 inline-flex">
          <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent" />
        </span>
      )}
      {children}
    </ShadcnButton>
  );
}

/* ===========================
 * CompatBadge — 기존 Limitless Badge API 호환
 * =========================== */

/** Limitless Badge variant → 커스텀 클래스 매핑 */
const BADGE_STYLE_MAP: Record<string, string> = {
  success:  "bg-success text-white",
  warning:  "bg-warning text-white",
  danger:   "bg-destructive text-white",
  light:    "bg-secondary text-secondary-foreground",
};

export interface CompatBadgeProps extends React.ComponentProps<"span"> {
  variant?: string;
  pill?: boolean;
}

export function CompatBadge({
  variant = "default",
  pill = false,
  className,
  ...props
}: CompatBadgeProps) {
  const customStyle = BADGE_STYLE_MAP[variant];

  if (customStyle) {
    // Limitless 색상 직접 적용
    return (
      <span
        className={cn(
          "inline-flex items-center justify-center rounded-full border border-transparent px-2 py-0.5 text-xs font-medium whitespace-nowrap",
          customStyle,
          className
        )}
        {...props}
      />
    );
  }

  // shadcn 기본 variant 사용
  return <ShadcnBadge className={className} {...props} />;
}

/* ===========================
 * CompatCard — 기존 Limitless Card API 호환
 * =========================== */

/** CardBody = CardContent 별칭 */
export function CardBody({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return <ShadcnCardContent className={className} {...props} />;
}

/** CardHeader with title prop — 기존 Limitless CardHeader 호환 */
export interface CompatCardHeaderProps extends React.ComponentProps<"div"> {
  title?: string;
}

export function CompatCardHeader({
  title,
  children,
  className,
  ...props
}: CompatCardHeaderProps) {
  return (
    <ShadcnCardHeader className={cn("border-b", className)} {...props}>
      {title && <ShadcnCardTitle>{title}</ShadcnCardTitle>}
      {children}
    </ShadcnCardHeader>
  );
}
