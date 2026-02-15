"use client";

import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";

// 네비게이션 카드 정보
interface NavCard {
	title: string;
	description: string;
	href: string;
	requiresAuth: boolean;
	color: string;
}

// 네비게이션 카드 목록
const navCards: NavCard[] = [
	{
		title: "로그인",
		description: "계정에 로그인하여 서비스를 이용하세요",
		href: "/login",
		requiresAuth: false,
		color: "blue",
	},
	{
		title: "회원가입",
		description: "새 계정을 만들어 서비스를 이용하세요",
		href: "/sign-up",
		requiresAuth: false,
		color: "green",
	},
	{
		title: "마이페이지",
		description: "내 정보를 확인하고 관리할 수 있습니다",
		href: "/profile",
		requiresAuth: true,
		color: "purple",
	},
	{
		title: "공개 페이지",
		description: "누구나 접근 가능한 공개 정보 페이지입니다",
		href: "/about",
		requiresAuth: false,
		color: "gray",
	},
];

// 색상별 Tailwind 클래스 매핑
const colorMap: Record<string, { bg: string; border: string; text: string; badge: string }> = {
	blue:   { bg: "bg-blue-50",   border: "border-blue-200",   text: "text-blue-700",   badge: "bg-blue-100 text-blue-700" },
	green:  { bg: "bg-green-50",  border: "border-green-200",  text: "text-green-700",  badge: "bg-green-100 text-green-700" },
	purple: { bg: "bg-purple-50", border: "border-purple-200", text: "text-purple-700", badge: "bg-purple-100 text-purple-700" },
	gray:   { bg: "bg-gray-50",   border: "border-gray-200",   text: "text-gray-700",   badge: "bg-gray-100 text-gray-700" },
};

// 홈 페이지 (인증 상태에 따라 UI 분기)
export default function HomePage() {
	const { user, loading, logout } = useAuth();

	return (
		<div className="min-h-screen bg-gray-50">
			{/* 상단 헤더 */}
			<header className="border-b border-gray-200 bg-white">
				<div className="mx-auto flex max-w-3xl items-center justify-between px-6 py-4">
					<h1 className="text-lg font-bold text-gray-900">
						회원 관리 시스템
					</h1>

					{/* 인증 상태 표시 */}
					<div className="flex items-center gap-3">
						{loading ? (
							<span className="text-sm text-gray-400">확인 중...</span>
						) : user ? (
							<>
								{/* 인증된 사용자 정보 */}
								<span className="text-sm text-gray-700">
									<strong>{user.username}</strong>
									<span className="ml-1 text-gray-400">({user.userId})</span>
								</span>
								<button
									onClick={logout}
									className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-600
										hover:bg-gray-50 transition-colors"
								>
									로그아웃
								</button>
							</>
						) : (
							<>
								{/* 미인증 상태 */}
								<Link
									href="/login"
									className="rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white
										hover:bg-blue-700 transition-colors"
								>
									로그인
								</Link>
								<Link
									href="/sign-up"
									className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-600
										hover:bg-gray-50 transition-colors"
								>
									회원가입
								</Link>
							</>
						)}
					</div>
				</div>
			</header>

			{/* 메인 콘텐츠 */}
			<main className="mx-auto max-w-3xl px-6 py-12">
				{/* 인증 상태 배너 */}
				{!loading && (
					<div className={`mb-8 rounded-lg p-4 text-sm ${
						user
							? "bg-green-50 text-green-700 border border-green-200"
							: "bg-yellow-50 text-yellow-700 border border-yellow-200"
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
					<h2 className="text-xl font-semibold text-gray-900">
						테스트 페이지 목록
					</h2>
					<p className="mt-1 text-sm text-gray-500">
						각 페이지의 인증 요구 사항을 확인하고 테스트할 수 있습니다
					</p>
				</div>

				{/* 네비게이션 카드 그리드 */}
				<div className="grid gap-4 sm:grid-cols-2">
					{navCards.map((card) => {
						const colors = colorMap[card.color];

						return (
							<Link
								key={card.href}
								href={card.href}
								className={`group rounded-xl border p-5 transition-all hover:shadow-md ${colors.border} ${colors.bg}`}
							>
								{/* 카드 헤더 (제목 + 배지) */}
								<div className="mb-2 flex items-center justify-between">
									<h3 className={`font-semibold ${colors.text}`}>
										{card.title}
									</h3>
									<span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
										card.requiresAuth
											? "bg-orange-100 text-orange-700"
											: colors.badge
									}`}>
										{card.requiresAuth ? "인증 필요" : "공개"}
									</span>
								</div>

								{/* 카드 설명 */}
								<p className="text-sm text-gray-600">
									{card.description}
								</p>

								{/* 경로 표시 */}
								<p className="mt-3 text-xs font-mono text-gray-400 group-hover:text-gray-500">
									{card.href}
								</p>
							</Link>
						);
					})}
				</div>
			</main>
		</div>
	);
}
