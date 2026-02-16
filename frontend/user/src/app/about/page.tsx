import type { Metadata } from "next";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";

// 페이지 메타데이터
export const metadata: Metadata = {
	title: "소개",
	description: "회원 관리 시스템 소개 페이지",
};

// 공개 페이지 (Server Component, 인증 불필요)
export default function AboutPage() {
	return (
		<div className="flex min-h-screen items-center justify-center bg-background px-4">
			<div className="w-full max-w-lg">
				{/* 헤더 */}
				<div className="mb-8 text-center">
					<h1 className="text-2xl font-bold">소개</h1>
					<p className="mt-2 text-sm text-muted-foreground">
						이 페이지는 누구나 접근할 수 있는 공개 페이지입니다
					</p>
				</div>

				{/* 콘텐츠 카드 */}
				<Card>
					<CardHeader>
						{/* 공개 배지 */}
						<div>
							<Badge className="bg-green-100 text-green-700 border-green-200 hover:bg-green-100">
								인증 불필요 — 공개 페이지
							</Badge>
						</div>
						{/* 시스템 설명 */}
						<CardTitle className="text-lg">회원 관리 시스템</CardTitle>
						<CardDescription className="leading-relaxed">
							RBAC 기반 회원/그룹/모듈 권한 관리 시스템입니다.
							관리자 API와 사용자 API를 분리하여 각각 독립적으로 배포·운영할 수 있는 구조로 설계되었습니다.
						</CardDescription>
					</CardHeader>

					<CardContent className="space-y-6">
						{/* 기술 스택 */}
						<div>
							<h3 className="mb-2 text-sm font-semibold">기술 스택</h3>
							<div className="flex flex-wrap gap-2">
								{[
									"Spring Boot 4.0",
									"Java 21",
									"MySQL 8.0",
									"Redis 7",
									"Next.js",
									"TypeScript",
									"Tailwind CSS",
									"Docker",
								].map((tech) => (
									<Badge key={tech} variant="secondary">
										{tech}
									</Badge>
								))}
							</div>
						</div>

						{/* 주요 기능 */}
						<div>
							<h3 className="mb-2 text-sm font-semibold">주요 기능</h3>
							<ul className="space-y-1.5 text-sm text-muted-foreground">
								<li className="flex items-start gap-2">
									<span className="mt-0.5 text-primary">•</span>
									JWT 기반 인증 (Access + Refresh Token)
								</li>
								<li className="flex items-start gap-2">
									<span className="mt-0.5 text-primary">•</span>
									RBAC 권한 관리 (사용자/그룹/역할)
								</li>
								<li className="flex items-start gap-2">
									<span className="mt-0.5 text-primary">•</span>
									모듈 시스템 (게시판, 블로그, 가계부 등)
								</li>
								<li className="flex items-start gap-2">
									<span className="mt-0.5 text-primary">•</span>
									Slug 기반 동적 라우팅
								</li>
								<li className="flex items-start gap-2">
									<span className="mt-0.5 text-primary">•</span>
									감사 로깅 및 세션 관리
								</li>
							</ul>
						</div>

						{/* 홈 링크 */}
						<Button variant="outline" asChild>
							<Link href="/">홈으로 돌아가기</Link>
						</Button>
					</CardContent>
				</Card>
			</div>
		</div>
	);
}
