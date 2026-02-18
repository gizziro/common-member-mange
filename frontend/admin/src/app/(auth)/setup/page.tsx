"use client";

import { useState, useEffect, useRef, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { apiPost, type ApiResponse } from "@/lib/api";
import { Button, Input, FormGroup, Alert } from "@/components/ui";
import { useAuth } from "@/contexts/AuthContext";

/** OTP 검증 응답 타입 */
interface VerifyOtpResponse {
  verified: boolean;
  verificationToken: string;
}

/** 전화번호 포맷 유효성 검증 (숫자만 10~11자리) */
function isValidPhone(phone: string): boolean {
  return /^[0-9]{10,11}$/.test(phone);
}

/** 셋업 위자드 — 최초 관리자 계정 생성 */
export default function SetupPage() {
  const { isLoading, isInitialized, markInitialized } = useAuth();
  const router = useRouter();

  const [userId, setUserId] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
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

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

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
    if (!isValidPhone(phone)) {
      setError("올바른 전화번호를 입력해 주세요 (숫자 10~11자리)");
      return;
    }

    setError(null);
    setSendingOtp(true);

    try {
      const res = await apiPost("/auth/sms/send-otp", { phone });

      if (res.success) {
        setOtpSent(true);
        setOtpCountdown(60);
      } else {
        setError(res.error?.message ?? "인증번호 발송에 실패했습니다");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSendingOtp(false);
    }
  };

  // OTP 검증 핸들러
  const handleVerifyOtp = async () => {
    if (!otpCode.trim()) {
      setError("인증번호를 입력해 주세요");
      return;
    }

    setError(null);
    setVerifyingOtp(true);

    try {
      const res: ApiResponse<VerifyOtpResponse> = await apiPost("/auth/sms/verify-otp", {
        phone,
        code: otpCode,
      });

      if (res.success && res.data?.verified) {
        setOtpVerified(true);
        setVerificationToken(res.data.verificationToken);
      } else {
        setError(res.error?.message ?? "인증번호가 올바르지 않습니다");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setVerifyingOtp(false);
    }
  };

  /* 프론트엔드 유효성 검증 */
  const validate = (): string | null => {
    if (userId.length < 4) return "아이디는 4자 이상이어야 합니다.";
    if (password.length < 8) return "비밀번호는 8자 이상이어야 합니다.";
    if (password !== passwordConfirm) return "비밀번호가 일치하지 않습니다.";
    if (!username.trim()) return "이름을 입력해주세요.";
    if (!email.includes("@")) return "올바른 이메일 형식이 아닙니다.";
    return null;
  };

  /* 폼 제출 */
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    /* 프론트 검증 */
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setSubmitting(true);

    try {
      // 요청 body 구성 — 전화번호 + 검증 토큰 포함
      const body: Record<string, string> = {
        userId,
        password,
        username,
        email,
      };

      if (phone.trim()) {
        body.phone = phone;
      }
      if (verificationToken) {
        body.phoneVerificationToken = verificationToken;
      }

      const res: ApiResponse = await apiPost("/setup/initialize", body);

      if (res.success) {
        setSuccess(true);
        /* 초기화 완료 상태 반영 후 로그인 페이지로 이동 */
        markInitialized();
        setTimeout(() => router.replace("/login"), 2000);
      } else if (res.error) {
        setError(res.error.message);
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  /* 초기 로딩 중 */
  if (isLoading) {
    return (
      <div className="flex items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  /* 이미 초기화된 경우 */
  if (isInitialized === true) {
    return (
      <div className="w-full max-w-sm text-center">
        <Alert variant="info">
          시스템이 이미 초기화되어 있습니다. 로그인 페이지로 이동합니다.
        </Alert>
      </div>
    );
  }

  return (
    <div className="w-full max-w-md">
      <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
        {/* 헤더 */}
        <div className="border-b border-gray-200 px-6 py-5 text-center">
          <h1 className="text-xl font-semibold text-gray-900">시스템 초기 설정</h1>
          <p className="mt-1 text-sm text-gray-500">최초 관리자 계정을 생성합니다</p>
        </div>

        {/* 본문 */}
        <div className="px-6 py-5">
          {/* 성공 메시지 */}
          {success && (
            <Alert variant="success" className="mb-4">
              관리자 계정이 생성되었습니다. 로그인 페이지로 이동합니다...
            </Alert>
          )}

          {/* 에러 메시지 */}
          {error && (
            <Alert variant="danger" dismissible className="mb-4">
              {error}
            </Alert>
          )}

          {!success && (
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* 아이디 */}
              <FormGroup label="아이디" htmlFor="userId" required>
                <Input
                  id="userId"
                  type="text"
                  placeholder="4자 이상 영문/숫자"
                  value={userId}
                  onChange={(e) => setUserId(e.target.value)}
                  required
                  autoFocus
                />
              </FormGroup>

              {/* 비밀번호 */}
              <FormGroup label="비밀번호" htmlFor="password" required>
                <Input
                  id="password"
                  type="password"
                  placeholder="8자 이상"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </FormGroup>

              {/* 비밀번호 확인 */}
              <FormGroup label="비밀번호 확인" htmlFor="passwordConfirm" required>
                <Input
                  id="passwordConfirm"
                  type="password"
                  placeholder="비밀번호를 다시 입력하세요"
                  value={passwordConfirm}
                  onChange={(e) => setPasswordConfirm(e.target.value)}
                  aria-invalid={passwordConfirm.length > 0 && password !== passwordConfirm}
                  required
                />
              </FormGroup>

              {/* 이름 */}
              <FormGroup label="이름" htmlFor="username" required>
                <Input
                  id="username"
                  type="text"
                  placeholder="관리자 이름"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
              </FormGroup>

              {/* 이메일 */}
              <FormGroup label="이메일" htmlFor="email" required>
                <Input
                  id="email"
                  type="email"
                  placeholder="admin@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </FormGroup>

              {/* 전화번호 + OTP 인증 (선택) */}
              <FormGroup label="전화번호 (선택)" htmlFor="phone">
                <div className="flex gap-2">
                  <Input
                    id="phone"
                    type="tel"
                    placeholder="01012345678"
                    value={phone}
                    onChange={(e) => handlePhoneChange(e.target.value)}
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

                {/* OTP 코드 입력 (발송 후 & 미인증 시 표시) */}
                {otpSent && !otpVerified && (
                  <div className="flex gap-2 mt-2">
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
              </FormGroup>

              {/* 생성 버튼 */}
              <Button
                type="submit"
                variant="primary"
                loading={submitting}
                className="w-full"
              >
                관리자 계정 생성
              </Button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
