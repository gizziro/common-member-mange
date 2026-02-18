"use client";

import { useState, useEffect, useRef, type FormEvent } from "react";
import Link from "next/link";
import { apiPost } from "@/lib/api";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { CircleCheckIcon } from "lucide-react";
import type { SignupResponse } from "@/types/api";

// 필드별 유효성 검증 에러
interface FieldErrors {
	userId?: string;
	password?: string;
	username?: string;
	email?: string;
	phone?: string;
}

// OTP 검증 응답 타입
interface VerifyOtpResponse {
	verified: boolean;
	verificationToken: string;
}

// 클라이언트 유효성 검증
function validate(fields: {
	userId: string;
	password: string;
	username: string;
	email: string;
}): FieldErrors | null {
	const errors: FieldErrors = {};

	// 아이디 검증 (4~50자)
	if (!fields.userId.trim()) {
		errors.userId = "아이디는 필수입니다";
	} else if (fields.userId.length < 4 || fields.userId.length > 50) {
		errors.userId = "아이디는 4~50자여야 합니다";
	}

	// 비밀번호 검증 (8~100자)
	if (!fields.password) {
		errors.password = "비밀번호는 필수입니다";
	} else if (fields.password.length < 8 || fields.password.length > 100) {
		errors.password = "비밀번호는 8~100자여야 합니다";
	}

	// 이름 검증 (2~100자)
	if (!fields.username.trim()) {
		errors.username = "이름은 필수입니다";
	} else if (fields.username.length < 2 || fields.username.length > 100) {
		errors.username = "이름은 2~100자여야 합니다";
	}

	// 이메일 검증
	if (!fields.email.trim()) {
		errors.email = "이메일은 필수입니다";
	} else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(fields.email)) {
		errors.email = "올바른 이메일 형식이 아닙니다";
	}

	return Object.keys(errors).length > 0 ? errors : null;
}

// 전화번호 포맷 유효성 검증 (숫자만 10~11자리)
function isValidPhone(phone: string): boolean {
	return /^[0-9]{10,11}$/.test(phone);
}

export default function SignupForm() {
	// 폼 입력 상태
	const [userId, setUserId] = useState("");
	const [password, setPassword] = useState("");
	const [username, setUsername] = useState("");
	const [email, setEmail] = useState("");
	const [phone, setPhone] = useState("");

	// OTP 인증 상태
	const [otpCode, setOtpCode] = useState("");
	const [otpSent, setOtpSent] = useState(false);
	const [otpVerified, setOtpVerified] = useState(false);
	const [verificationToken, setVerificationToken] = useState("");
	const [sendingOtp, setSendingOtp] = useState(false);
	const [verifyingOtp, setVerifyingOtp] = useState(false);
	const [otpCountdown, setOtpCountdown] = useState(0);

	// UI 상태
	const [pending, setPending] = useState(false);
	const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
	const [successData, setSuccessData] = useState<SignupResponse | null>(null);

	// 타이머 참조
	const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

	// OTP 재발송 카운트다운 타이머
	useEffect(() => {
		if (otpCountdown > 0) {
			timerRef.current = setInterval(() => {
				setOtpCountdown((prev) => {
					if (prev <= 1) {
						if (timerRef.current) clearInterval(timerRef.current);
						return 0;
					}
					return prev - 1;
				});
			}, 1000);
		}
		return () => {
			if (timerRef.current) clearInterval(timerRef.current);
		};
	}, [otpCountdown]);

	// 전화번호 변경 시 OTP 상태 초기화
	const handlePhoneChange = (value: string) => {
		setPhone(value);
		// 전화번호가 변경되면 인증 상태 리셋
		if (otpSent || otpVerified) {
			setOtpSent(false);
			setOtpVerified(false);
			setOtpCode("");
			setVerificationToken("");
			setOtpCountdown(0);
		}
	};

	// OTP 발송 핸들러
	const handleSendOtp = async () => {
		// 전화번호 유효성 검증
		if (!isValidPhone(phone)) {
			setFieldErrors((prev) => ({
				...prev,
				phone: "올바른 전화번호를 입력해 주세요 (숫자 10~11자리)",
			}));
			return;
		}

		setFieldErrors((prev) => ({ ...prev, phone: undefined }));
		setSendingOtp(true);

		try {
			const res = await apiPost("/auth/sms/send-otp", { phone });

			if (res.success) {
				setOtpSent(true);
				// 60초 재발송 쿨다운
				setOtpCountdown(60);
				toast.success("인증번호가 발송되었습니다");
			} else {
				const errorMessage = res.error?.message ?? "인증번호 발송에 실패했습니다";
				toast.error(errorMessage);
			}
		} catch {
			toast.error("서버에 연결할 수 없습니다");
		} finally {
			setSendingOtp(false);
		}
	};

	// OTP 검증 핸들러
	const handleVerifyOtp = async () => {
		if (!otpCode.trim()) {
			toast.error("인증번호를 입력해 주세요");
			return;
		}

		setVerifyingOtp(true);

		try {
			const res = await apiPost<VerifyOtpResponse>("/auth/sms/verify-otp", {
				phone,
				code: otpCode,
			});

			if (res.success && res.data?.verified) {
				setOtpVerified(true);
				setVerificationToken(res.data.verificationToken);
				toast.success("전화번호가 인증되었습니다");
			} else {
				const errorMessage = res.error?.message ?? "인증번호가 올바르지 않습니다";
				toast.error(errorMessage);
			}
		} catch {
			toast.error("서버에 연결할 수 없습니다");
		} finally {
			setVerifyingOtp(false);
		}
	};

	// 폼 제출 핸들러
	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setFieldErrors({});

		// 1. 클라이언트 유효성 검증
		const errors = validate({ userId, password, username, email });
		if (errors) {
			setFieldErrors(errors);
			return;
		}

		setPending(true);

		try {
			// 2. 백엔드 API 호출 (JSON 요청) — 전화번호 + 검증 토큰 포함
			const body: Record<string, string> = {
				userId,
				password,
				username,
				email,
			};

			// 전화번호 입력 시 포함 (인증 완료된 경우 토큰도 함께)
			if (phone.trim()) {
				body.phone = phone;
			}
			if (verificationToken) {
				body.phoneVerificationToken = verificationToken;
			}

			const json = await apiPost<SignupResponse>("/auth/signup", body);

			// 3. 성공 응답 처리
			if (json.success && json.data) {
				setSuccessData(json.data);
				toast.success("회원가입 완료", {
					description: `${json.data.userId}님, 환영합니다!`,
				});
				return;
			}

			// 4. 백엔드 에러 응답 처리
			const errorCode = json.error?.code || "";
			const errorMessage = json.error?.message || "회원가입에 실패했습니다";

			if (errorCode === "USER_DUPLICATE_ID") {
				setFieldErrors({ userId: errorMessage });
			} else if (errorCode === "USER_DUPLICATE_EMAIL") {
				setFieldErrors({ email: errorMessage });
			} else if (errorCode === "SMS_VERIFICATION_INVALID") {
				setFieldErrors({ phone: errorMessage });
			} else {
				toast.error("회원가입 실패", { description: errorMessage });
			}
		} catch {
			toast.error("서버 연결 오류", {
				description: "서버에 연결할 수 없습니다",
			});
		} finally {
			setPending(false);
		}
	}

	// 회원가입 성공 시 완료 화면
	if (successData) {
		return (
			<div className="text-center">
				<div className="mb-6">
					<div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
						<CircleCheckIcon className="h-8 w-8 text-green-600" />
					</div>
				</div>
				<h2 className="mb-2 text-xl font-semibold">
					회원가입이 완료되었습니다
				</h2>
				<p className="mb-6 text-sm text-muted-foreground">
					<strong>{successData.userId}</strong>님, 환영합니다!
				</p>
				<Button asChild>
					<Link href="/">홈으로 이동</Link>
				</Button>
			</div>
		);
	}

	return (
		<form onSubmit={handleSubmit} className="space-y-5">
			{/* 아이디 */}
			<div className="space-y-2">
				<Label htmlFor="userId">아이디</Label>
				<Input
					id="userId"
					type="text"
					value={userId}
					onChange={(e) => setUserId(e.target.value)}
					required
					minLength={4}
					maxLength={50}
					placeholder="4자 이상 입력"
					aria-invalid={!!fieldErrors.userId}
				/>
				{fieldErrors.userId && (
					<p className="text-xs text-destructive">{fieldErrors.userId}</p>
				)}
			</div>

			{/* 비밀번호 */}
			<div className="space-y-2">
				<Label htmlFor="password">비밀번호</Label>
				<Input
					id="password"
					type="password"
					value={password}
					onChange={(e) => setPassword(e.target.value)}
					required
					minLength={8}
					maxLength={100}
					placeholder="8자 이상 입력"
					aria-invalid={!!fieldErrors.password}
				/>
				{fieldErrors.password && (
					<p className="text-xs text-destructive">{fieldErrors.password}</p>
				)}
			</div>

			{/* 이름 */}
			<div className="space-y-2">
				<Label htmlFor="username">이름</Label>
				<Input
					id="username"
					type="text"
					value={username}
					onChange={(e) => setUsername(e.target.value)}
					required
					minLength={2}
					maxLength={100}
					placeholder="이름을 입력하세요"
					aria-invalid={!!fieldErrors.username}
				/>
				{fieldErrors.username && (
					<p className="text-xs text-destructive">{fieldErrors.username}</p>
				)}
			</div>

			{/* 이메일 */}
			<div className="space-y-2">
				<Label htmlFor="email">이메일</Label>
				<Input
					id="email"
					type="email"
					value={email}
					onChange={(e) => setEmail(e.target.value)}
					required
					placeholder="example@email.com"
					aria-invalid={!!fieldErrors.email}
				/>
				{fieldErrors.email && (
					<p className="text-xs text-destructive">{fieldErrors.email}</p>
				)}
			</div>

			{/* 전화번호 + OTP 인증 */}
			<div className="space-y-2">
				<Label htmlFor="phone">전화번호 (선택)</Label>
				<div className="flex gap-2">
					<Input
						id="phone"
						type="tel"
						value={phone}
						onChange={(e) => handlePhoneChange(e.target.value)}
						placeholder="01012345678"
						maxLength={11}
						disabled={otpVerified}
						className="flex-1"
					/>
					{/* 인증 완료 표시 또는 발송 버튼 */}
					{otpVerified ? (
						<span className="inline-flex items-center rounded-md bg-green-50 px-3 text-sm font-medium text-green-700">
							인증완료
						</span>
					) : (
						<Button
							type="button"
							variant="outline"
							onClick={handleSendOtp}
							disabled={sendingOtp || otpCountdown > 0 || !phone.trim()}
						>
							{sendingOtp
								? "발송 중..."
								: otpCountdown > 0
									? `${otpCountdown}초`
									: otpSent
										? "재발송"
										: "인증번호 발송"}
						</Button>
					)}
				</div>
				{fieldErrors.phone && (
					<p className="text-xs text-destructive">{fieldErrors.phone}</p>
				)}

				{/* OTP 코드 입력 (발송 후 & 미인증 시 표시) */}
				{otpSent && !otpVerified && (
					<div className="flex gap-2 pt-1">
						<Input
							type="text"
							value={otpCode}
							onChange={(e) => setOtpCode(e.target.value)}
							placeholder="인증번호 6자리"
							maxLength={6}
							className="flex-1"
						/>
						<Button
							type="button"
							variant="outline"
							onClick={handleVerifyOtp}
							disabled={verifyingOtp || !otpCode.trim()}
						>
							{verifyingOtp ? "확인 중..." : "인증 확인"}
						</Button>
					</div>
				)}
			</div>

			{/* 제출 버튼 */}
			<Button type="submit" disabled={pending} className="w-full">
				{pending ? "가입 처리 중..." : "회원가입"}
			</Button>

			{/* 로그인 링크 */}
			<p className="text-center text-sm text-muted-foreground">
				이미 계정이 있으신가요?{" "}
				<Link
					href="/login"
					className="font-medium text-primary hover:underline"
				>
					로그인
				</Link>
			</p>
		</form>
	);
}
