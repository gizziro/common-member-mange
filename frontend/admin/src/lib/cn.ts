import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * 조건부 className 합성 유틸리티
 * clsx로 조건부 클래스를 결합하고, tailwind-merge로 충돌을 해결한다.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
