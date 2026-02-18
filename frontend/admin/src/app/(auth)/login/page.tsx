"use client";

import { useState, type FormEvent } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button, Input, FormGroup, Alert } from "@/components/ui";

/** 로그인 페이지 (2FA OTP 지원) */
export default function LoginPage() {
  const { login, verifyOtp, isLoading } = useAuth();
  const [userId, setUserId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // OTP 2FA 상태
  const [otpStep, setOtpStep] = useState(false);
  const [otpSessionId, setOtpSessionId] = useState("");
  const [otpCode, setOtpCode] = useState("");
  const [verifyingOtp, setVerifyingOtp] = useState(false);

  /* 로그인 폼 제출 */
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const res = await login(userId, password);

      if (res.success && res.data?.requireOtp) {
        // OTP 필요 — 2단계 인증 화면으로 전환
        setOtpStep(true);
        setOtpSessionId(res.data.otpSessionId ?? "");
      } else if (!res.success && res.error) {
        setError(res.error.message);
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  /* OTP 검증 폼 제출 */
  const handleOtpSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!otpCode.trim()) {
      setError("인증번호를 입력해 주세요.");
      return;
    }

    setVerifyingOtp(true);

    try {
      const res = await verifyOtp(otpSessionId, otpCode);

      if (!res.success && res.error) {
        setError(res.error.message);
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setVerifyingOtp(false);
    }
  };

  /* OTP 취소 — 로그인 화면으로 복귀 */
  const handleOtpCancel = () => {
    setOtpStep(false);
    setOtpSessionId("");
    setOtpCode("");
    setError(null);
  };

  /* 초기 로딩 중 — 스피너 표시 */
  if (isLoading) {
    return (
      <div className="flex items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  /* OTP 2단계 인증 화면 */
  if (otpStep) {
    return (
      <div className="w-full max-w-sm">
        <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
          {/* 헤더 */}
          <div className="border-b border-gray-200 px-6 py-5 text-center">
            <h1 className="text-xl font-semibold text-gray-900">2단계 인증</h1>
            <p className="mt-1 text-sm text-gray-500">
              등록된 전화번호로 발송된 인증번호를 입력하세요
            </p>
          </div>

          {/* 본문 */}
          <div className="px-6 py-5">
            {/* 에러 메시지 */}
            {error && (
              <Alert variant="danger" dismissible className="mb-4">
                {error}
              </Alert>
            )}

            <form onSubmit={handleOtpSubmit} className="space-y-4">
              {/* 인증번호 입력 */}
              <FormGroup label="인증번호" htmlFor="otpCode" required>
                <Input
                  id="otpCode"
                  type="text"
                  placeholder="6자리 인증번호"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value)}
                  maxLength={6}
                  required
                  autoFocus
                />
              </FormGroup>

              {/* 인증 버튼 */}
              <Button
                type="submit"
                variant="primary"
                loading={verifyingOtp}
                className="w-full"
              >
                인증 확인
              </Button>

              {/* 취소 버튼 */}
              <Button
                type="button"
                variant="outline"
                onClick={handleOtpCancel}
                className="w-full"
              >
                로그인으로 돌아가기
              </Button>
            </form>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-sm">
      {/* 카드 */}
      <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
        {/* 헤더 */}
        <div className="border-b border-gray-200 px-6 py-5 text-center">
          <h1 className="text-xl font-semibold text-gray-900">관리자 로그인</h1>
          <p className="mt-1 text-sm text-gray-500">관리자 계정으로 로그인하세요</p>
        </div>

        {/* 본문 */}
        <div className="px-6 py-5">
          {/* 에러 메시지 */}
          {error && (
            <Alert variant="danger" dismissible className="mb-4">
              {error}
            </Alert>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* 아이디 */}
            <FormGroup label="아이디" htmlFor="userId" required>
              <Input
                id="userId"
                type="text"
                placeholder="아이디를 입력하세요"
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
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </FormGroup>

            {/* 로그인 버튼 */}
            <Button
              type="submit"
              variant="primary"
              loading={submitting}
              className="w-full"
            >
              로그인
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
