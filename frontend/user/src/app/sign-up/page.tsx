import type { Metadata } from "next";
import SignupForm from "./SignupForm";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";

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
				{/* 폼 카드 */}
				<Card>
					<CardHeader className="text-center">
						<CardTitle className="text-2xl">회원가입</CardTitle>
						<CardDescription>
							아래 정보를 입력하여 계정을 만드세요
						</CardDescription>
					</CardHeader>
					<CardContent>
						<SignupForm />
					</CardContent>
				</Card>
			</div>
		</div>
	);
}
