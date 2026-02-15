import type { Metadata } from "next";
import LoginForm from "./LoginForm";

// 페이지 메타데이터
export const metadata: Metadata = {
	title: "로그인",
	description: "계정에 로그인하여 서비스를 이용하세요",
};

// 로그인 페이지 (Server Component)
export default function LoginPage() {
	return (
		<div className="flex min-h-screen items-center justify-center px-4">
			<div className="w-full max-w-md">
				{/* 헤더 */}
				<div className="mb-8 text-center">
					<h1 className="text-2xl font-bold text-gray-900">로그인</h1>
					<p className="mt-2 text-sm text-gray-500">
						계정에 로그인하여 서비스를 이용하세요
					</p>
				</div>

				{/* 폼 카드 */}
				<div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
					<LoginForm />
				</div>
			</div>
		</div>
	);
}
