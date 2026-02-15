import type { Metadata } from "next";
import ProfileContent from "./ProfileContent";

// 페이지 메타데이터
export const metadata: Metadata = {
	title: "마이페이지",
	description: "내 계정 정보를 확인하고 관리합니다",
};

// 마이페이지 (Server Component, 인증 필요)
export default function ProfilePage() {
	return (
		<div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
			<div className="w-full max-w-md">
				<ProfileContent />
			</div>
		</div>
	);
}
