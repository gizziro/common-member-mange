import type { Metadata } from "next";
import LoginForm from "./LoginForm";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";

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
				{/* 폼 카드 */}
				<Card>
					<CardHeader className="text-center">
						<CardTitle className="text-2xl">로그인</CardTitle>
						<CardDescription>
							계정에 로그인하여 서비스를 이용하세요
						</CardDescription>
					</CardHeader>
					<CardContent>
						<LoginForm />
					</CardContent>
				</Card>
			</div>
		</div>
	);
}
