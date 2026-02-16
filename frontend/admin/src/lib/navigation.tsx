import { House, Users, UsersThree } from "@phosphor-icons/react";
import type { SidebarNavItem } from "@/types/components";

/**
 * 관리자 사이드바 네비게이션 아이템
 */
export const adminNavItems: SidebarNavItem[] = [
  {
    type: "header",
    label: "관리",
  },
  {
    type: "link",
    label: "대시보드",
    href: "/dashboard",
    icon: <House size={20} />,
  },
  {
    type: "link",
    label: "회원 관리",
    href: "/users",
    icon: <Users size={20} />,
  },
  {
    type: "link",
    label: "그룹 관리",
    href: "/groups",
    icon: <UsersThree size={20} />,
  },
];
