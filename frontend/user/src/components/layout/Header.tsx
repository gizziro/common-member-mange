"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { apiGet } from "@/lib/api";
import { Button } from "@/components/ui/button";

/* ===========================
 * 메뉴 항목 타입
 * =========================== */

/** 백엔드 메뉴 응답 항목 */
interface MenuItem {
	/** 메뉴 PK */
	id: string;
	/** 메뉴 표시명 */
	name: string;
	/** 아이콘 식별자 */
	icon: string | null;
	/** 메뉴 유형 */
	menuType: "MODULE" | "LINK" | "SEPARATOR";
	/** 자동 생성된 URL (MODULE/LINK) */
	url: string | null;
	/** 단축 경로 (alias) */
	aliasPath: string | null;
	/** 정렬 순서 */
	sortOrder: number;
	/** 하위 메뉴 목록 (재귀) */
	children: MenuItem[];
}

/** 렌더링용 네비게이션 링크 */
interface NavLink {
	/** 표시 텍스트 */
	name: string;
	/** 이동 경로 */
	url: string;
}

/* ===========================
 * 동적 메뉴 기반 헤더 네비게이션
 * =========================== */

export function Header() {
	const { user, loading, logout } = useAuth();
	const pathname = usePathname();
	const [menuItems, setMenuItems] = useState<MenuItem[]>([]);

	// 메뉴 로드 (인증 불필요 — 모든 보이는 메뉴를 공개)
	useEffect(() => {
		const loadMenus = async () => {
			try {
				// 공개 메뉴 트리 조회 (인증 토큰 불필요)
				const res = await apiGet<MenuItem[]>("/menus/me");
				if (res.success && res.data) {
					setMenuItems(res.data);
				}
			} catch {
				// 메뉴 로드 실패 시 무시
			}
		};

		// 로딩 완료 후 메뉴 로드
		if (!loading) {
			loadMenus();
		}
	}, [loading]);

	// 메뉴 트리에서 네비게이션 링크 추출 (SEPARATOR 하위 + 최상위 링크)
	// aliasPath가 있으면 단축 URL 사용, 없으면 내부 URL 사용
	const extractLinks = (items: MenuItem[]): NavLink[] => {
		const links: NavLink[] = [];

		for (const item of items) {
			if (item.menuType === "SEPARATOR") {
				// SEPARATOR 하위의 링크 수집
				for (const child of item.children) {
					const linkUrl = child.aliasPath
						? "/" + child.aliasPath
						: child.url;
					if (linkUrl) {
						links.push({ name: child.name, url: linkUrl });
					}
				}
			} else {
				// 최상위 MODULE/LINK 항목
				const linkUrl = item.aliasPath
					? "/" + item.aliasPath
					: item.url;
				if (linkUrl) {
					links.push({ name: item.name, url: linkUrl });
				}
			}
		}

		return links;
	};

	// 네비게이션 링크 목록
	const navLinks = extractLinks(menuItems);

	// 현재 경로가 링크와 일치하는지 확인
	const isActive = (url: string) =>
		pathname === url || pathname.startsWith(url + "/");

	return (
		<header className="border-b bg-card">
			<div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-3">
				{/* 좌측: 사이트 로고 + 메뉴 링크 */}
				<div className="flex items-center gap-6">
					{/* 사이트 로고 (홈 링크) */}
					<Link href="/" className="text-lg font-bold">
						회원 관리 시스템
					</Link>

					{/* 동적 메뉴 네비게이션 (데스크톱) */}
					{navLinks.length > 0 && (
						<nav className="hidden items-center gap-4 md:flex">
							{navLinks.map((link) => (
								<Link
									key={link.url}
									href={link.url}
									className={`text-sm transition-colors hover:text-foreground ${
										isActive(link.url)
											? "font-medium text-foreground"
											: "text-muted-foreground"
									}`}
								>
									{link.name}
								</Link>
							))}
						</nav>
					)}
				</div>

				{/* 우측: 인증 상태 */}
				<div className="flex items-center gap-3">
					{loading ? (
						// 인증 확인 중 로딩 표시
						<span className="text-sm text-muted-foreground">확인 중...</span>
					) : user ? (
						<>
							{/* 인증된 사용자 정보 */}
							<span className="hidden text-sm sm:inline">
								<strong>{user.username}</strong>
								<span className="ml-1 text-muted-foreground">
									({user.userId})
								</span>
							</span>
							{/* 마이페이지 링크 */}
							<Button variant="outline" size="sm" asChild>
								<Link href="/profile">마이페이지</Link>
							</Button>
							{/* 로그아웃 버튼 */}
							<Button variant="ghost" size="sm" onClick={logout}>
								로그아웃
							</Button>
						</>
					) : (
						<>
							{/* 미인증: 로그인/회원가입 링크 */}
							<Button size="sm" asChild>
								<Link href="/login">로그인</Link>
							</Button>
							<Button variant="outline" size="sm" asChild>
								<Link href="/sign-up">회원가입</Link>
							</Button>
						</>
					)}
				</div>
			</div>
		</header>
	);
}
