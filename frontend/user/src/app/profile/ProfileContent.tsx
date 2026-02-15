"use client";

import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";

// 마이페이지 콘텐츠 (Client Component, 인증 상태 확인)
export default function ProfileContent() {
	const { user, loading, logout } = useAuth();

	// 로딩 중 스피너
	if (loading) {
		return (
			<div className="text-center py-12">
				<div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" />
				<p className="mt-3 text-sm text-gray-500">인증 확인 중...</p>
			</div>
		);
	}

	// 미인증 상태: 로그인 유도
	if (!user) {
		return (
			<div className="rounded-xl border border-gray-200 bg-white p-8 shadow-sm text-center">
				{/* 잠금 아이콘 */}
				<div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
					<svg
						className="h-8 w-8 text-red-600"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
					>
						<path
							strokeLinecap="round"
							strokeLinejoin="round"
							strokeWidth={2}
							d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
						/>
					</svg>
				</div>
				<h2 className="mb-2 text-xl font-semibold text-gray-900">
					인증이 필요합니다
				</h2>
				<p className="mb-6 text-sm text-gray-500">
					이 페이지는 로그인한 사용자만 접근할 수 있습니다
				</p>
				<div className="flex justify-center gap-3">
					<Link
						href="/login"
						className="rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-medium text-white
							hover:bg-blue-700 transition-colors"
					>
						로그인
					</Link>
					<Link
						href="/"
						className="rounded-lg border border-gray-300 px-5 py-2.5 text-sm text-gray-600
							hover:bg-gray-50 transition-colors"
					>
						홈으로
					</Link>
				</div>
			</div>
		);
	}

	// 인증된 사용자: 프로필 정보 표시
	return (
		<>
			{/* 헤더 */}
			<div className="mb-8 text-center">
				<h1 className="text-2xl font-bold text-gray-900">마이페이지</h1>
				<p className="mt-2 text-sm text-gray-500">
					내 계정 정보를 확인할 수 있습니다
				</p>
			</div>

			{/* 프로필 카드 */}
			<div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
				{/* 사용자 아바타 + 이름 */}
				<div className="mb-6 flex items-center gap-4">
					<div className="flex h-14 w-14 items-center justify-center rounded-full bg-purple-100 text-xl font-bold text-purple-700">
						{user.username.charAt(0)}
					</div>
					<div>
						<h2 className="text-lg font-semibold text-gray-900">
							{user.username}
						</h2>
						<p className="text-sm text-gray-500">@{user.userId}</p>
					</div>
				</div>

				{/* 상세 정보 */}
				<div className="space-y-4 border-t border-gray-100 pt-4">
					{/* 사용자 PK */}
					<div className="flex items-center justify-between">
						<span className="text-sm text-gray-500">ID (PK)</span>
						<span className="text-sm font-mono text-gray-700">{user.id}</span>
					</div>

					{/* 로그인 아이디 */}
					<div className="flex items-center justify-between">
						<span className="text-sm text-gray-500">아이디</span>
						<span className="text-sm font-medium text-gray-900">{user.userId}</span>
					</div>

					{/* 이메일 */}
					<div className="flex items-center justify-between">
						<span className="text-sm text-gray-500">이메일</span>
						<span className="text-sm text-gray-900">{user.email}</span>
					</div>

					{/* 계정 상태 */}
					<div className="flex items-center justify-between">
						<span className="text-sm text-gray-500">상태</span>
						<span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
							user.userStatus === "ACTIVE"
								? "bg-green-100 text-green-700"
								: user.userStatus === "PENDING"
									? "bg-yellow-100 text-yellow-700"
									: "bg-red-100 text-red-700"
						}`}>
							{user.userStatus}
						</span>
					</div>
				</div>

				{/* 액션 버튼 */}
				<div className="mt-6 flex gap-3">
					<button
						onClick={logout}
						className="flex-1 rounded-lg border border-red-300 py-2.5 text-sm font-medium text-red-600
							hover:bg-red-50 transition-colors"
					>
						로그아웃
					</button>
					<Link
						href="/"
						className="flex-1 rounded-lg border border-gray-300 py-2.5 text-center text-sm text-gray-600
							hover:bg-gray-50 transition-colors"
					>
						홈으로
					</Link>
				</div>
			</div>
		</>
	);
}
