"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { apiGet, apiDelete } from "@/lib/api";
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
import { toast } from "sonner";

// 소셜 연동 정보 타입
interface UserIdentity {
	id: string;
	providerCode: string;
	providerName: string;
	providerSubject: string;
	linkedAt: string;
}

// 소셜 Provider 타입
interface OAuth2Provider {
	code: string;
	name: string;
	iconUrl: string | null;
}

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

// Provider별 배경색 반환
function getProviderBadgeStyle(code: string): string {
	switch (code) {
		case "google":
			return "bg-white text-gray-700 border border-gray-300";
		case "kakao":
			return "bg-[#FEE500] text-[#191919] border-[#FDD800]";
		case "naver":
			return "bg-[#03C75A] text-white border-[#02b351]";
		default:
			return "bg-gray-100 text-gray-800 border-gray-200";
	}
}

// 마이페이지 콘텐츠 (Client Component, 인증 상태 확인)
export default function ProfileContent() {
	const { user, loading, logout } = useAuth();

	// 소셜 연동 상태
	const [identities, setIdentities]               = useState<UserIdentity[]>([]);
	const [identitiesLoading, setIdentitiesLoading]  = useState(false);
	const [providers, setProviders]                   = useState<OAuth2Provider[]>([]);
	const [unlinking, setUnlinking]                   = useState<string | null>(null);

	// 소셜 연동 목록 로드
	const loadIdentities = useCallback(async () => {
		const token = localStorage.getItem("accessToken");
		if (!token) return;

		setIdentitiesLoading(true);
		try {
			const res = await apiGet<UserIdentity[]>("/auth/oauth2/identities", token);
			if (res.success && res.data) {
				setIdentities(res.data);
			}
		} catch {
			/* 실패 시 빈 배열 유지 */
		} finally {
			setIdentitiesLoading(false);
		}
	}, []);

	// 활성 Provider 목록 로드
	const loadProviders = useCallback(async () => {
		try {
			const res = await apiGet<OAuth2Provider[]>("/auth/oauth2/providers");
			if (res.success && res.data) {
				setProviders(res.data);
			}
		} catch {
			/* 실패 시 빈 배열 유지 */
		}
	}, []);

	// 인증 완료 후 연동 목록 + Provider 목록 로드
	useEffect(() => {
		if (user) {
			loadIdentities();
			loadProviders();
		}
	}, [user, loadIdentities, loadProviders]);

	// 소셜 연동 추가 핸들러
	async function handleLink(providerCode: string) {
		const token = localStorage.getItem("accessToken");
		if (!token) return;

		try {
			const res = await apiGet<string>(`/auth/oauth2/link/${providerCode}`, token);
			if (res.success && res.data) {
				// 소셜 로그인 페이지로 리다이렉트
				window.location.href = res.data;
			} else {
				toast.error("연동 오류", {
					description: res.error?.message ?? "Authorization URL을 가져올 수 없습니다.",
				});
			}
		} catch {
			toast.error("서버 연결 오류");
		}
	}

	// 소셜 연동 해제 핸들러
	async function handleUnlink(identityId: string) {
		const token = localStorage.getItem("accessToken");
		if (!token) return;

		setUnlinking(identityId);
		try {
			const res = await apiDelete(`/auth/oauth2/identities/${identityId}`, token);
			if (res.success) {
				toast.success("연동이 해제되었습니다.");
				await loadIdentities();
			} else {
				toast.error("연동 해제 실패", {
					description: res.error?.message ?? "연동을 해제할 수 없습니다.",
				});
			}
		} catch {
			toast.error("서버 연결 오류");
		} finally {
			setUnlinking(null);
		}
	}

	// 연동되지 않은 Provider 필터링
	const linkedProviderCodes = new Set(identities.map((i) => i.providerCode));
	const unlinkedProviders = providers.filter((p) => !linkedProviderCodes.has(p.code));

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

			{/* 소셜 계정 연동 카드 */}
			<Card className="mt-5">
				<CardHeader>
					<CardTitle className="text-lg">소셜 계정 연동</CardTitle>
					<CardDescription>
						소셜 계정을 연동하여 간편하게 로그인할 수 있습니다
					</CardDescription>
				</CardHeader>

				<CardContent>
					{identitiesLoading ? (
						<div className="py-4 text-center">
							<div className="inline-block h-5 w-5 animate-spin rounded-full border-4 border-muted border-t-primary" />
						</div>
					) : (
						<div className="space-y-3">
							{/* 연동된 소셜 계정 */}
							{identities.map((identity) => (
								<div
									key={identity.id}
									className="flex items-center justify-between rounded-lg border p-3"
								>
									<div className="flex items-center gap-3">
										<Badge className={getProviderBadgeStyle(identity.providerCode)}>
											{identity.providerName}
										</Badge>
										<div>
											<p className="text-sm font-medium">{identity.providerName}</p>
											<p className="text-xs text-muted-foreground">
												{new Date(identity.linkedAt).toLocaleDateString("ko-KR")} 연동
											</p>
										</div>
									</div>
									<Button
										variant="outline"
										size="sm"
										disabled={unlinking === identity.id}
										onClick={() => handleUnlink(identity.id)}
									>
										{unlinking === identity.id ? "해제 중..." : "연동 해제"}
									</Button>
								</div>
							))}

							{/* 미연동 Provider */}
							{unlinkedProviders.map((provider) => (
								<div
									key={provider.code}
									className="flex items-center justify-between rounded-lg border border-dashed p-3"
								>
									<div className="flex items-center gap-3">
										<Badge variant="secondary">{provider.name}</Badge>
										<p className="text-sm text-muted-foreground">미연동</p>
									</div>
									<Button
										variant="default"
										size="sm"
										onClick={() => handleLink(provider.code)}
									>
										연동하기
									</Button>
								</div>
							))}

							{/* 연동 가능한 Provider가 없을 때 */}
							{identities.length === 0 && unlinkedProviders.length === 0 && (
								<p className="py-4 text-center text-sm text-muted-foreground">
									연동 가능한 소셜 계정이 없습니다.
								</p>
							)}
						</div>
					)}
				</CardContent>
			</Card>
		</>
	);
}
