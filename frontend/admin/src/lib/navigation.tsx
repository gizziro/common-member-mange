import { House, Users, UsersThree, Gear, GearSix, Key, List, FileText } from "@phosphor-icons/react";
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
  {
    type: "header",
    label: "콘텐츠",
  },
  {
    type: "link",
    label: "메뉴 관리",
    href: "/menus",
    icon: <List size={20} />,
  },
  {
    type: "link",
    label: "페이지 관리",
    href: "/pages",
    icon: <FileText size={20} />,
  },
  {
    type: "header",
    label: "설정",
  },
  {
    type: "link",
    label: "시스템 설정",
    href: "/settings/system",
    icon: <GearSix size={20} />,
  },
  {
    type: "link",
    label: "소셜 로그인",
    href: "/settings/auth-providers",
    icon: <Key size={20} />,
  },
];
