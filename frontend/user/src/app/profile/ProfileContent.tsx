"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { apiGet, apiPost, apiDelete } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle,
	AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
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
	const { user, loading, logout, refresh } = useAuth();

	// 소셜 연동 상태
	const [identities, setIdentities]               = useState<UserIdentity[]>([]);
	const [identitiesLoading, setIdentitiesLoading]  = useState(false);
	const [providers, setProviders]                   = useState<OAuth2Provider[]>([]);
	const [unlinking, setUnlinking]                   = useState<string | null>(null);

	// 비밀번호 설정 폼 상태 (소셜 전용 사용자용)
	const [newUserId, setNewUserId]         = useState("");
	const [newPassword, setNewPassword]     = useState("");
	const [settingPassword, setSettingPassword] = useState(false);

	// 회원 탈퇴 상태
	const [withdrawing, setWithdrawing] = useState(false);

	// 소셜 전용 사용자 여부 (비밀번호 미설정)
	const isLocalUser = user?.provider === "LOCAL";

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

	// 비밀번호 설정 핸들러 (소셜 전용 사용자 → 로컬 전환)
	async function handleSetPassword(e: FormEvent) {
		e.preventDefault();

		if (!newUserId.trim() || !newPassword) {
			toast.error("아이디와 비밀번호를 입력해주세요.");
			return;
		}

		const token = localStorage.getItem("accessToken");
		if (!token) return;

		setSettingPassword(true);
		try {
			const res = await apiPost("/auth/oauth2/set-password", {
				userId: newUserId,
				password: newPassword,
			}, token);

			if (res.success) {
				toast.success("비밀번호가 설정되었습니다.", {
					description: "이제 소셜 계정 연동 관리가 가능합니다.",
				});
				// 사용자 정보 새로고침 (provider 변경 반영)
				await refresh();
				// 폼 초기화
				setNewUserId("");
				setNewPassword("");
			} else {
				toast.error("비밀번호 설정 실패", {
					description: res.error?.message ?? "비밀번호를 설정할 수 없습니다.",
				});
			}
		} catch {
			toast.error("서버 연결 오류");
		} finally {
			setSettingPassword(false);
		}
	}

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

	// 회원 탈퇴 핸들러
	async function handleWithdraw() {
		const token = localStorage.getItem("accessToken");
		if (!token) return;

		setWithdrawing(true);
		try {
			const res = await apiDelete("/auth/withdraw", token);
			if (res.success) {
				// 로컬 토큰 삭제
				localStorage.removeItem("accessToken");
				localStorage.removeItem("refreshToken");

				toast.success("회원 탈퇴가 완료되었습니다.");

				// 홈으로 이동
				window.location.href = "/";
			} else {
				toast.error("탈퇴 실패", {
					description: res.error?.message ?? "회원 탈퇴를 처리할 수 없습니다.",
				});
			}
		} catch {
			toast.error("서버 연결 오류");
		} finally {
			setWithdrawing(false);
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

						{/* 로그인 방식 */}
						<div className="flex items-center justify-between">
							<span className="text-sm text-muted-foreground">로그인 방식</span>
							<Badge variant={isLocalUser ? "secondary" : "outline"}>
								{isLocalUser ? "자체 계정" : user.provider}
							</Badge>
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

			{/* 비밀번호 설정 카드 (소셜 전용 사용자만 표시) */}
			{!isLocalUser && (
				<Card className="mt-5">
					<CardHeader>
						<CardTitle className="text-lg">로컬 계정 설정</CardTitle>
						<CardDescription>
							아이디와 비밀번호를 설정하면 자체 로그인이 가능해지고,
							다른 소셜 계정의 연동/해제를 관리할 수 있습니다.
						</CardDescription>
					</CardHeader>

					<CardContent>
						<form onSubmit={handleSetPassword} className="space-y-4">
							{/* 새 로컬 아이디 */}
							<div className="space-y-2">
								<Label htmlFor="newUserId">아이디</Label>
								<Input
									id="newUserId"
									type="text"
									value={newUserId}
									onChange={(e) => setNewUserId(e.target.value)}
									placeholder="4~50자 로컬 로그인 아이디"
									minLength={4}
									maxLength={50}
									required
								/>
							</div>

							{/* 비밀번호 */}
							<div className="space-y-2">
								<Label htmlFor="newPassword">비밀번호</Label>
								<Input
									id="newPassword"
									type="password"
									value={newPassword}
									onChange={(e) => setNewPassword(e.target.value)}
									placeholder="8자 이상 비밀번호"
									minLength={8}
									maxLength={100}
									required
								/>
							</div>

							{/* 설정 버튼 */}
							<Button type="submit" disabled={settingPassword} className="w-full">
								{settingPassword ? "설정 중..." : "비밀번호 설정"}
							</Button>
						</form>
					</CardContent>
				</Card>
			)}

			{/* 소셜 계정 연동 카드 */}
			<Card className="mt-5">
				<CardHeader>
					<CardTitle className="text-lg">소셜 계정 연동</CardTitle>
					<CardDescription>
						{isLocalUser
							? "소셜 계정을 연동하여 간편하게 로그인할 수 있습니다"
							: "비밀번호를 설정하면 소셜 계정 연동/해제를 관리할 수 있습니다"
						}
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
									{/* 로컬 사용자만 연동 해제 가능 */}
									{isLocalUser && (
										<Button
											variant="outline"
											size="sm"
											disabled={unlinking === identity.id}
											onClick={() => handleUnlink(identity.id)}
										>
											{unlinking === identity.id ? "해제 중..." : "연동 해제"}
										</Button>
									)}
								</div>
							))}

							{/* 미연동 Provider (로컬 사용자만 연동 추가 가능) */}
							{isLocalUser && unlinkedProviders.map((provider) => (
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

			{/* 회원 탈퇴 카드 */}
			<Card className="mt-5 border-destructive/30">
				<CardHeader>
					<CardTitle className="text-lg text-destructive">위험 구역</CardTitle>
					<CardDescription>
						회원 탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다
					</CardDescription>
				</CardHeader>

				<CardContent>
					<AlertDialog>
						<AlertDialogTrigger asChild>
							<Button variant="destructive" className="w-full" disabled={withdrawing}>
								{withdrawing ? "탈퇴 처리 중..." : "회원 탈퇴"}
							</Button>
						</AlertDialogTrigger>
						<AlertDialogContent>
							<AlertDialogHeader>
								<AlertDialogTitle>정말 탈퇴하시겠습니까?</AlertDialogTitle>
								<AlertDialogDescription>
									회원 탈퇴 시 계정, 소셜 연동, 그룹 멤버십 등
									모든 데이터가 영구적으로 삭제됩니다.
									이 작업은 되돌릴 수 없습니다.
								</AlertDialogDescription>
							</AlertDialogHeader>
							<AlertDialogFooter>
								<AlertDialogCancel>취소</AlertDialogCancel>
								<AlertDialogAction
									onClick={handleWithdraw}
									className="bg-destructive text-white hover:bg-destructive/90"
								>
									탈퇴하기
								</AlertDialogAction>
							</AlertDialogFooter>
						</AlertDialogContent>
					</AlertDialog>
				</CardContent>
			</Card>
		</>
	);
}
