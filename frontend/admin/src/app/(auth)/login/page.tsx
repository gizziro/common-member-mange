"use client";

import { useState, type FormEvent } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button, Input, FormGroup, Alert } from "@/components/ui";

/** 로그인 페이지 */
export default function LoginPage() {
  const { login, isLoading } = useAuth();
  const [userId, setUserId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  /* 로그인 폼 제출 */
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const res = await login(userId, password);
      if (!res.success && res.error) {
        setError(res.error.message);
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  /* 초기 로딩 중 — 스피너 표시 */
  if (isLoading) {
    return (
      <div className="flex items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
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
