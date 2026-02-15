import type { Metadata } from "next";
import Link from "next/link";

// 페이지 메타데이터
export const metadata: Metadata = {
	title: "소개",
	description: "회원 관리 시스템 소개 페이지",
};

// 공개 페이지 (Server Component, 인증 불필요)
export default function AboutPage() {
	return (
		<div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
			<div className="w-full max-w-lg">
				{/* 헤더 */}
				<div className="mb-8 text-center">
					<h1 className="text-2xl font-bold text-gray-900">소개</h1>
					<p className="mt-2 text-sm text-gray-500">
						이 페이지는 누구나 접근할 수 있는 공개 페이지입니다
					</p>
				</div>

				{/* 콘텐츠 카드 */}
				<div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
					{/* 공개 배지 */}
					<div className="mb-4">
						<span className="rounded-full bg-green-100 px-3 py-1 text-xs font-medium text-green-700">
							인증 불필요 — 공개 페이지
						</span>
					</div>

					{/* 시스템 설명 */}
					<h2 className="mb-3 text-lg font-semibold text-gray-900">
						회원 관리 시스템
					</h2>
					<p className="mb-4 text-sm leading-relaxed text-gray-600">
						RBAC 기반 회원/그룹/모듈 권한 관리 시스템입니다.
						관리자 API와 사용자 API를 분리하여 각각 독립적으로 배포·운영할 수 있는 구조로 설계되었습니다.
					</p>

					{/* 기술 스택 */}
					<h3 className="mb-2 text-sm font-semibold text-gray-900">기술 스택</h3>
					<div className="mb-4 flex flex-wrap gap-2">
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
							<span
								key={tech}
								className="rounded-md bg-gray-100 px-2 py-1 text-xs text-gray-600"
							>
								{tech}
							</span>
						))}
					</div>

					{/* 주요 기능 */}
					<h3 className="mb-2 text-sm font-semibold text-gray-900">주요 기능</h3>
					<ul className="mb-6 space-y-1.5 text-sm text-gray-600">
						<li className="flex items-start gap-2">
							<span className="mt-0.5 text-blue-500">•</span>
							JWT 기반 인증 (Access + Refresh Token)
						</li>
						<li className="flex items-start gap-2">
							<span className="mt-0.5 text-blue-500">•</span>
							RBAC 권한 관리 (사용자/그룹/역할)
						</li>
						<li className="flex items-start gap-2">
							<span className="mt-0.5 text-blue-500">•</span>
							모듈 시스템 (게시판, 블로그, 가계부 등)
						</li>
						<li className="flex items-start gap-2">
							<span className="mt-0.5 text-blue-500">•</span>
							Slug 기반 동적 라우팅
						</li>
						<li className="flex items-start gap-2">
							<span className="mt-0.5 text-blue-500">•</span>
							감사 로깅 및 세션 관리
						</li>
					</ul>

					{/* 홈 링크 */}
					<Link
						href="/"
						className="inline-block rounded-lg border border-gray-300 px-5 py-2.5 text-sm text-gray-600
							hover:bg-gray-50 transition-colors"
					>
						홈으로 돌아가기
					</Link>
				</div>
			</div>
		</div>
	);
}
