/**
 * UI 컴포넌트 Barrel Export
 *
 * 기존 Limitless 커스텀 API를 유지하면서 shadcn/ui 전환을 지원한다.
 * - 호환 래퍼: Button, Badge, CardHeader, CardBody (기존 페이지용)
 * - shadcn 패스스루: Card, CardFooter, Input (API 호환 가능)
 * - Limitless 전용: Modal, Alert, FormGroup, Pagination, Dropdown
 *
 * 새 페이지(users 등)는 shadcn 컴포넌트를 직접 import하여 사용.
 */

/* 호환 래퍼 (기존 페이지용) */
export {
  CompatButton as Button,
  type CompatButtonProps as ButtonProps,
  CompatBadge as Badge,
  type CompatBadgeProps as BadgeProps,
  CompatCardHeader as CardHeader,
  type CompatCardHeaderProps as CardHeaderProps,
  CardBody,
} from "./_compat";

/* shadcn 패스스루 (API 호환) */
export { Card, CardFooter } from "./Card";
export type { ComponentProps as CardProps } from "react";
export type { ComponentProps as CardFooterProps } from "react";

/* shadcn Input (기존 Input과 API 호환) */
export { Input } from "./Input";

/* Limitless 전용 컴포넌트 */
export { Alert, type AlertProps } from "./Alert";
export { FormGroup, type FormGroupProps } from "./FormGroup";
export { Modal, type ModalProps } from "./Modal";
export { Pagination, type PaginationProps } from "./Pagination";
export {
  Dropdown,
  DropdownTrigger,
  DropdownMenu,
  DropdownItem,
  DropdownDivider,
  type DropdownProps,
  type DropdownTriggerProps,
  type DropdownMenuProps,
  type DropdownItemProps,
} from "./Dropdown";
