"use client";

import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

// 네비게이션 카드 정보
interface NavCard {
	title: string;
	description: string;
	href: string;
	requiresAuth: boolean;
}

// 네비게이션 카드 목록
const navCards: NavCard[] = [
	{
		title: "로그인",
		description: "계정에 로그인하여 서비스를 이용하세요",
		href: "/login",
		requiresAuth: false,
	},
	{
		title: "회원가입",
		description: "새 계정을 만들어 서비스를 이용하세요",
		href: "/sign-up",
		requiresAuth: false,
	},
	{
		title: "마이페이지",
		description: "내 정보를 확인하고 관리할 수 있습니다",
		href: "/profile",
		requiresAuth: true,
	},
	{
		title: "공개 페이지",
		description: "누구나 접근 가능한 공개 정보 페이지입니다",
		href: "/about",
		requiresAuth: false,
	},
];

// 홈 페이지 (인증 상태에 따라 UI 분기)
export default function HomePage() {
	const { user, loading } = useAuth();

	return (
		<main className="mx-auto max-w-3xl px-6 py-12">
			{/* 인증 상태 배너 */}
			{!loading && (
				<div className={`mb-8 rounded-lg border p-4 text-sm ${
					user
						? "border-green-200 bg-green-50 text-green-700"
						: "border-yellow-200 bg-yellow-50 text-yellow-700"
				}`}>
					{user ? (
						<>
							<strong>{user.username}</strong>님으로 로그인되어 있습니다.
							모든 페이지에 접근할 수 있습니다.
						</>
					) : (
						"로그인하지 않은 상태입니다. 인증이 필요한 페이지는 접근이 제한됩니다."
					)}
				</div>
			)}

			{/* 페이지 설명 */}
			<div className="mb-8">
				<h2 className="text-xl font-semibold">
					테스트 페이지 목록
				</h2>
				<p className="mt-1 text-sm text-muted-foreground">
					각 페이지의 인증 요구 사항을 확인하고 테스트할 수 있습니다
				</p>
			</div>

			{/* 네비게이션 카드 그리드 */}
			<div className="grid gap-4 sm:grid-cols-2">
				{navCards.map((card) => (
					<Link key={card.href} href={card.href} className="group">
						<Card className="h-full transition-all hover:shadow-md">
							<CardHeader>
								{/* 카드 헤더 (제목 + 배지) */}
								<div className="flex items-center justify-between">
									<CardTitle className="text-base">{card.title}</CardTitle>
									<Badge variant={card.requiresAuth ? "destructive" : "secondary"}>
										{card.requiresAuth ? "인증 필요" : "공개"}
									</Badge>
								</div>
								{/* 카드 설명 */}
								<CardDescription>{card.description}</CardDescription>
							</CardHeader>
							<CardContent>
								{/* 경로 표시 */}
								<p className="text-xs font-mono text-muted-foreground group-hover:text-foreground transition-colors">
									{card.href}
								</p>
							</CardContent>
						</Card>
					</Link>
				))}
			</div>
		</main>
	);
}
