"use client";

import { useEffect, useState, useCallback } from "react";
import { apiGet, apiPut, type ApiResponse } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/Badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { toast } from "sonner";
import { FloppyDisk, Eye, EyeSlash } from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

/** 인증 제공자 응답 타입 */
interface AuthProvider {
  id: string;
  code: string;
  name: string;
  isEnabled: boolean;
  clientId: string | null;
  clientSecret: string | null;
  redirectUri: string | null;
  authorizationUri: string | null;
  tokenUri: string | null;
  userinfoUri: string | null;
  scope: string | null;
  iconUrl: string | null;
  displayOrder: number;
}

/** 제공자별 수정 폼 상태 */
interface ProviderFormState {
  clientId: string;
  clientSecret: string;
  redirectUri: string;
  scope: string;
  iconUrl: string;
  isEnabled: boolean;
}

/** Provider 코드별 브랜드 색상 */
function getProviderColor(code: string): string {
  switch (code) {
    case "google":
      return "border-l-4 border-l-blue-500";
    case "kakao":
      return "border-l-4 border-l-yellow-400";
    case "naver":
      return "border-l-4 border-l-green-500";
    default:
      return "border-l-4 border-l-gray-300";
  }
}

/** 소셜 로그인 관리 페이지 */
export default function AuthProvidersPage() {
  // Provider 목록 상태
  const [providers, setProviders] = useState<AuthProvider[]>([]);
  const [loading, setLoading]     = useState(true);

  // Provider별 폼 상태 (id → ProviderFormState)
  const [forms, setForms] = useState<Record<string, ProviderFormState>>({});

  // 저장 중인 Provider ID
  const [savingId, setSavingId] = useState<string | null>(null);

  // Client Secret 표시/숨김 상태 (id → boolean)
  const [showSecret, setShowSecret] = useState<Record<string, boolean>>({});

  // Provider 목록 로드
  const loadProviders = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<AuthProvider[]>("/auth/providers");
      if (res.success && res.data) {
        // local 제외한 소셜 Provider만 표시
        const socialProviders = res.data.filter((p) => p.code !== "local");
        setProviders(socialProviders);

        // 폼 상태 초기화
        const initialForms: Record<string, ProviderFormState> = {};
        for (const p of socialProviders) {
          initialForms[p.id] = {
            clientId: p.clientId ?? "",
            clientSecret: "",
            redirectUri: p.redirectUri ?? "",
            scope: p.scope ?? "",
            iconUrl: p.iconUrl ?? "",
            isEnabled: p.isEnabled,
          };
        }
        setForms(initialForms);
      } else {
        toast.error(res.error?.message ?? "Provider 목록을 불러올 수 없습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProviders();
  }, [loadProviders]);

  // 폼 필드 변경 핸들러
  const handleFieldChange = (
    id: string,
    field: keyof ProviderFormState,
    value: string | boolean
  ) => {
    setForms((prev) => ({
      ...prev,
      [id]: {
        ...prev[id],
        [field]: value,
      },
    }));
  };

  // Provider 설정 저장
  const handleSave = async (provider: AuthProvider) => {
    const form = forms[provider.id];
    if (!form) return;

    setSavingId(provider.id);

    try {
      // Client Secret이 빈 문자열이면 기존 값 유지를 위해 null 전송
      const body = {
        clientId: form.clientId || null,
        clientSecret: form.clientSecret || null,
        redirectUri: form.redirectUri || null,
        scope: form.scope || null,
        iconUrl: form.iconUrl || null,
        isEnabled: form.isEnabled,
      };

      const res: ApiResponse<AuthProvider> = await apiPut(
        `/auth/providers/${provider.id}`,
        body
      );

      if (res.success && res.data) {
        toast.success(`${provider.name} 설정이 저장되었습니다.`);
        // 저장 후 목록 새로고침
        await loadProviders();
      } else {
        toast.error(res.error?.message ?? "설정 저장에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setSavingId(null);
    }
  };

  return (
    <>
      <PageHeader
        title="소셜 로그인 관리"
        subtitle="OAuth2 소셜 로그인 제공자를 설정합니다"
      />

      <div className="p-5 space-y-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : providers.length === 0 ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            등록된 소셜 로그인 제공자가 없습니다.
          </div>
        ) : (
          providers.map((provider) => {
            const form = forms[provider.id];
            if (!form) return null;

            return (
              <Card
                key={provider.id}
                className={`${getProviderColor(provider.code)}`}
              >
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle className="text-lg">
                        {provider.name}
                      </CardTitle>
                      <CardDescription>
                        코드: {provider.code}
                      </CardDescription>
                    </div>
                    <Badge variant={form.isEnabled ? "default" : "secondary"}>
                      {form.isEnabled ? "활성" : "비활성"}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {/* 활성화 토글 */}
                  <div className="flex items-center gap-3">
                    <Label className="min-w-[100px]">활성화</Label>
                    <button
                      type="button"
                      role="switch"
                      aria-checked={form.isEnabled}
                      onClick={() =>
                        handleFieldChange(
                          provider.id,
                          "isEnabled",
                          !form.isEnabled
                        )
                      }
                      className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors ${
                        form.isEnabled ? "bg-primary" : "bg-gray-200"
                      }`}
                    >
                      <span
                        className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-lg ring-0 transition-transform ${
                          form.isEnabled ? "translate-x-5" : "translate-x-0"
                        }`}
                      />
                    </button>
                  </div>

                  {/* Client ID */}
                  <div className="grid grid-cols-[100px_1fr] items-center gap-3">
                    <Label>Client ID</Label>
                    <Input
                      value={form.clientId}
                      onChange={(e) =>
                        handleFieldChange(provider.id, "clientId", e.target.value)
                      }
                      placeholder="OAuth2 Client ID"
                    />
                  </div>

                  {/* Client Secret */}
                  <div className="grid grid-cols-[100px_1fr] items-center gap-3">
                    <Label>Client Secret</Label>
                    <div className="relative">
                      <Input
                        type={showSecret[provider.id] ? "text" : "password"}
                        value={form.clientSecret}
                        onChange={(e) =>
                          handleFieldChange(
                            provider.id,
                            "clientSecret",
                            e.target.value
                          )
                        }
                        placeholder={
                          provider.clientSecret
                            ? `현재: ${provider.clientSecret} (비어두면 유지)`
                            : "OAuth2 Client Secret"
                        }
                      />
                      <button
                        type="button"
                        onClick={() =>
                          setShowSecret((prev) => ({
                            ...prev,
                            [provider.id]: !prev[provider.id],
                          }))
                        }
                        className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                      >
                        {showSecret[provider.id] ? (
                          <EyeSlash size={16} />
                        ) : (
                          <Eye size={16} />
                        )}
                      </button>
                    </div>
                  </div>

                  {/* Redirect URI */}
                  <div className="grid grid-cols-[100px_1fr] items-center gap-3">
                    <Label>Redirect URI</Label>
                    <Input
                      value={form.redirectUri}
                      onChange={(e) =>
                        handleFieldChange(
                          provider.id,
                          "redirectUri",
                          e.target.value
                        )
                      }
                      placeholder={`http://localhost:6100/auth/oauth2/callback/${provider.code}`}
                    />
                  </div>

                  {/* Scope */}
                  <div className="grid grid-cols-[100px_1fr] items-center gap-3">
                    <Label>Scope</Label>
                    <Input
                      value={form.scope}
                      onChange={(e) =>
                        handleFieldChange(provider.id, "scope", e.target.value)
                      }
                      placeholder="요청 Scope (공백 구분)"
                    />
                  </div>

                  {/* 엔드포인트 정보 (읽기 전용) */}
                  <div className="rounded-md bg-muted/50 p-3 space-y-1 text-xs text-muted-foreground">
                    <p>
                      <span className="font-medium">Authorization URI:</span>{" "}
                      {provider.authorizationUri ?? "-"}
                    </p>
                    <p>
                      <span className="font-medium">Token URI:</span>{" "}
                      {provider.tokenUri ?? "-"}
                    </p>
                    <p>
                      <span className="font-medium">UserInfo URI:</span>{" "}
                      {provider.userinfoUri ?? "-"}
                    </p>
                  </div>

                  {/* 저장 버튼 */}
                  <div className="flex justify-end">
                    <Button
                      onClick={() => handleSave(provider)}
                      disabled={savingId === provider.id}
                    >
                      <FloppyDisk size={16} className="mr-1" />
                      {savingId === provider.id ? "저장 중..." : "저장"}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })
        )}
      </div>
    </>
  );
}
