"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { apiPost, type ApiResponse } from "@/lib/api";
import { Button, Input, FormGroup, Alert } from "@/components/ui";
import { useAuth } from "@/contexts/AuthContext";

/** 셋업 위자드 — 최초 관리자 계정 생성 */
export default function SetupPage() {
  const { isLoading, isInitialized, markInitialized } = useAuth();
  const router = useRouter();

  const [userId, setUserId] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

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
      const res: ApiResponse = await apiPost("/setup/initialize", {
        userId,
        password,
        username,
        email,
      });

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
