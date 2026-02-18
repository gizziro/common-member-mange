"use client";

import { useEffect, useState, useCallback } from "react";
import { apiGet, apiPut, apiPost, type ApiResponse } from "@/lib/api";
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
import { FloppyDisk, Eye, EyeSlash, PaperPlaneTilt } from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

/** SMS 프로바이더 응답 타입 */
interface SmsProvider {
  id: string;
  code: string;
  name: string;
  isEnabled: boolean;
  apiKey: string | null;
  apiSecret: string | null;
  senderNumber: string | null;
  configJson: string | null;
  displayOrder: number;
}

/** 프로바이더별 수정 폼 상태 */
interface ProviderFormState {
  apiKey: string;
  apiSecret: string;
  senderNumber: string;
  configJson: string;
  isEnabled: boolean;
}

/** Provider 코드별 브랜드 색상 */
function getProviderColor(code: string): string {
  switch (code) {
    case "solapi":
      return "border-l-4 border-l-violet-500";
    case "aws_sns":
      return "border-l-4 border-l-orange-500";
    default:
      return "border-l-4 border-l-gray-300";
  }
}

/** SMS 프로바이더 관리 페이지 */
export default function SmsProvidersPage() {
  // Provider 목록 상태
  const [providers, setProviders] = useState<SmsProvider[]>([]);
  const [loading, setLoading]     = useState(true);

  // Provider별 폼 상태 (id → ProviderFormState)
  const [forms, setForms] = useState<Record<string, ProviderFormState>>({});

  // 저장 중인 Provider ID
  const [savingId, setSavingId] = useState<string | null>(null);

  // 테스트 발송 중인 Provider ID
  const [testingId, setTestingId] = useState<string | null>(null);

  // API Secret 표시/숨김 상태 (id → boolean)
  const [showSecret, setShowSecret] = useState<Record<string, boolean>>({});

  // 테스트 전화번호 입력 상태 (id → string)
  const [testPhones, setTestPhones] = useState<Record<string, string>>({});

  // Provider 목록 로드
  const loadProviders = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<SmsProvider[]>("/sms/providers");
      if (res.success && res.data) {
        setProviders(res.data);

        // 폼 상태 초기화
        const initialForms: Record<string, ProviderFormState> = {};
        for (const p of res.data) {
          initialForms[p.id] = {
            apiKey: p.apiKey ?? "",
            apiSecret: "",
            senderNumber: p.senderNumber ?? "",
            configJson: p.configJson ?? "",
            isEnabled: p.isEnabled,
          };
        }
        setForms(initialForms);
      } else {
        toast.error(res.error?.message ?? "SMS 프로바이더 목록을 불러올 수 없습니다.");
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
  const handleSave = async (provider: SmsProvider) => {
    const form = forms[provider.id];
    if (!form) return;

    setSavingId(provider.id);

    try {
      const body = {
        apiKey: form.apiKey || null,
        apiSecret: form.apiSecret || null,
        senderNumber: form.senderNumber || null,
        configJson: form.configJson || null,
        isEnabled: form.isEnabled,
      };

      const res: ApiResponse<SmsProvider> = await apiPut(
        `/sms/providers/${provider.id}`,
        body
      );

      if (res.success && res.data) {
        toast.success(`${provider.name} 설정이 저장되었습니다.`);
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

  // 테스트 SMS 발송
  const handleTestSend = async (provider: SmsProvider) => {
    const phone = testPhones[provider.id]?.trim();
    if (!phone) {
      toast.error("테스트 전화번호를 입력해 주세요.");
      return;
    }

    setTestingId(provider.id);

    try {
      const res = await apiPost(`/sms/providers/${provider.id}/test`, { phone });

      if (res.success) {
        toast.success(`${phone}로 테스트 SMS가 발송되었습니다.`);
      } else {
        toast.error(res.error?.message ?? "테스트 발송에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setTestingId(null);
    }
  };

  return (
    <>
      <PageHeader
        title="SMS 프로바이더 관리"
        subtitle="SMS 발송 서비스 제공자를 설정합니다"
      />

      <div className="p-5 space-y-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : providers.length === 0 ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            등록된 SMS 프로바이더가 없습니다.
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

                  {/* API Key */}
                  <div className="grid grid-cols-[100px_1fr] items-center gap-3">
                    <Label>API Key</Label>
                    <Input
                      value={form.apiKey}
                      onChange={(e) =>
                        handleFieldChange(provider.id, "apiKey", e.target.value)
                      }
                      placeholder="API Key"
                    />
                  </div>

                  {/* API Secret */}
                  <div className="grid grid-cols-[100px_1fr] items-center gap-3">
                    <Label>API Secret</Label>
                    <div className="relative">
                      <Input
                        type={showSecret[provider.id] ? "text" : "password"}
                        value={form.apiSecret}
                        onChange={(e) =>
                          handleFieldChange(
                            provider.id,
                            "apiSecret",
                            e.target.value
                          )
                        }
                        placeholder={
                          provider.apiSecret
                            ? `현재: ${provider.apiSecret} (비어두면 유지)`
                            : "API Secret"
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

                  {/* 발신 번호 */}
                  <div className="grid grid-cols-[100px_1fr] items-center gap-3">
                    <Label>발신 번호</Label>
                    <Input
                      value={form.senderNumber}
                      onChange={(e) =>
                        handleFieldChange(
                          provider.id,
                          "senderNumber",
                          e.target.value
                        )
                      }
                      placeholder="01012345678"
                    />
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

                  {/* 테스트 발송 (활성화된 프로바이더만 표시) */}
                  {form.isEnabled && (
                    <div className="rounded-md bg-muted/50 p-4 space-y-3">
                      <p className="text-sm font-medium">테스트 발송</p>
                      <div className="flex gap-2">
                        <Input
                          value={testPhones[provider.id] ?? ""}
                          onChange={(e) =>
                            setTestPhones((prev) => ({
                              ...prev,
                              [provider.id]: e.target.value,
                            }))
                          }
                          placeholder="수신 전화번호 (01012345678)"
                          className="flex-1"
                        />
                        <Button
                          variant="outline"
                          onClick={() => handleTestSend(provider)}
                          disabled={testingId === provider.id}
                        >
                          <PaperPlaneTilt size={16} className="mr-1" />
                          {testingId === provider.id ? "발송 중..." : "발송"}
                        </Button>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })
        )}
      </div>
    </>
  );
}
