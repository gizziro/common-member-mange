"use client";

import { useState, useCallback, useRef } from "react";

/**
 * useSidebarResize — 미니 사이드바 hover-unfold 로직
 * 축소된 사이드바에 마우스를 올리면 150ms 딜레이 후 임시 펼침
 */
export function useSidebarResize() {
  const [isUnfolded, setIsUnfolded] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  /* 마우스 진입 — 150ms 딜레이 후 펼침 */
  const handleMouseEnter = useCallback(() => {
    timerRef.current = setTimeout(() => {
      setIsUnfolded(true);
    }, 150);
  }, []);

  /* 마우스 이탈 — 타이머 취소 + 즉시 접힘 */
  const handleMouseLeave = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    setIsUnfolded(false);
  }, []);

  return { isUnfolded, handleMouseEnter, handleMouseLeave };
}
