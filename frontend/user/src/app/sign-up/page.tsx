import type { Metadata } from "next";
import SignupForm from "./SignupForm";

// 페이지 메타데이터
export const metadata: Metadata = {
	title: "회원가입",
	description: "새 계정을 만들어 서비스를 이용하세요",
};

// 회원가입 페이지 (Server Component)
export default function SignupPage() {
	return (
		<div className="flex min-h-screen items-center justify-center px-4">
			<div className="w-full max-w-md">
				{/* 헤더 */}
				<div className="mb-8 text-center">
					<h1 className="text-2xl font-bold text-gray-900">회원가입</h1>
					<p className="mt-2 text-sm text-gray-500">
						아래 정보를 입력하여 계정을 만드세요
					</p>
				</div>

				{/* 폼 카드 */}
				<div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
					<SignupForm />
				</div>
			</div>
		</div>
	);
}
