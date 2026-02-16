"use client";

import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { LockIcon } from "lucide-react";

// 상태 배지 variant 매핑
function statusBadge(status: string) {
	switch (status) {
		case "ACTIVE":
			return <Badge className="bg-green-100 text-green-700 border-green-200 hover:bg-green-100">활성</Badge>;
		case "PENDING":
			return <Badge className="bg-yellow-100 text-yellow-700 border-yellow-200 hover:bg-yellow-100">대기</Badge>;
		default:
			return <Badge variant="destructive">{status}</Badge>;
	}
}

// 마이페이지 콘텐츠 (Client Component, 인증 상태 확인)
export default function ProfileContent() {
	const { user, loading, logout } = useAuth();

	// 로딩 중 스피너
	if (loading) {
		return (
			<div className="py-12 text-center">
				<div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
				<p className="mt-3 text-sm text-muted-foreground">인증 확인 중...</p>
			</div>
		);
	}

	// 미인증 상태: 로그인 유도
	if (!user) {
		return (
			<Card className="text-center">
				<CardContent className="pt-6">
					{/* 잠금 아이콘 */}
					<div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
						<LockIcon className="h-8 w-8 text-red-600" />
					</div>
					<h2 className="mb-2 text-xl font-semibold">
						인증이 필요합니다
					</h2>
					<p className="mb-6 text-sm text-muted-foreground">
						이 페이지는 로그인한 사용자만 접근할 수 있습니다
					</p>
					<div className="flex justify-center gap-3">
						<Button asChild>
							<Link href="/login">로그인</Link>
						</Button>
						<Button variant="outline" asChild>
							<Link href="/">홈으로</Link>
						</Button>
					</div>
				</CardContent>
			</Card>
		);
	}

	// 인증된 사용자: 프로필 정보 표시
	return (
		<>
			{/* 헤더 */}
			<div className="mb-8 text-center">
				<h1 className="text-2xl font-bold">마이페이지</h1>
				<p className="mt-2 text-sm text-muted-foreground">
					내 계정 정보를 확인할 수 있습니다
				</p>
			</div>

			{/* 프로필 카드 */}
			<Card>
				<CardHeader>
					{/* 사용자 아바타 + 이름 */}
					<div className="flex items-center gap-4">
						<div className="flex h-14 w-14 items-center justify-center rounded-full bg-purple-100 text-xl font-bold text-purple-700">
							{user.username.charAt(0)}
						</div>
						<div>
							<CardTitle className="text-lg">{user.username}</CardTitle>
							<CardDescription>@{user.userId}</CardDescription>
						</div>
					</div>
				</CardHeader>

				<CardContent>
					{/* 상세 정보 */}
					<div className="space-y-4 border-t pt-4">
						{/* 사용자 PK */}
						<div className="flex items-center justify-between">
							<span className="text-sm text-muted-foreground">ID (PK)</span>
							<span className="text-sm font-mono">{user.id}</span>
						</div>

						{/* 로그인 아이디 */}
						<div className="flex items-center justify-between">
							<span className="text-sm text-muted-foreground">아이디</span>
							<span className="text-sm font-medium">{user.userId}</span>
						</div>

						{/* 이메일 */}
						<div className="flex items-center justify-between">
							<span className="text-sm text-muted-foreground">이메일</span>
							<span className="text-sm">{user.email}</span>
						</div>

						{/* 계정 상태 */}
						<div className="flex items-center justify-between">
							<span className="text-sm text-muted-foreground">상태</span>
							{statusBadge(user.userStatus)}
						</div>
					</div>

					{/* 액션 버튼 */}
					<div className="mt-6 flex gap-3">
						<Button
							variant="destructive"
							className="flex-1"
							onClick={logout}
						>
							로그아웃
						</Button>
						<Button variant="outline" className="flex-1" asChild>
							<Link href="/">홈으로</Link>
						</Button>
					</div>
				</CardContent>
			</Card>
		</>
	);
}
