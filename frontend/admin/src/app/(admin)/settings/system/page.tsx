"use client";

import { useEffect, useState, useCallback } from "react";
import { apiGet, apiPut, type ApiResponse } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { toast } from "sonner";
import { FloppyDisk, Lock } from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

/** 개별 설정 항목 */
interface SettingItem {
  settingKey: string;
  settingValue: string;
  valueType: "STRING" | "NUMBER" | "BOOLEAN" | "JSON";
  name: string;
  description: string | null;
  readonly: boolean;
  sortOrder: number;
}

/** 그룹별 설정 응답 */
interface SettingGroup {
  group: string;
  settings: SettingItem[];
}

/** 그룹 표시명 매핑 */
const GROUP_LABELS: Record<string, string> = {
  general: "일반",
  signup: "회원가입",
  auth: "인증/보안",
  session: "세션/토큰",
};

/** 그룹 설명 매핑 */
const GROUP_DESCRIPTIONS: Record<string, string> = {
  general: "사이트 기본 정보를 설정합니다",
  signup: "회원가입 정책을 설정합니다",
  auth: "로그인 보안 및 인증 정책을 설정합니다",
  session: "JWT 토큰 만료 시간을 설정합니다",
};

/** 시스템 설정 관리 페이지 */
export default function SystemSettingsPage() {
  // 설정 그룹 목록
  const [groups, setGroups] = useState<SettingGroup[]>([]);
  // 로딩 상태
  const [loading, setLoading] = useState(true);
  // 그룹별 폼 상태 (group → key → value)
  const [forms, setForms] = useState<Record<string, Record<string, string>>>(
    {}
  );
  // 저장 중인 그룹
  const [savingGroup, setSavingGroup] = useState<string | null>(null);

  // 설정 데이터 로드
  const loadSettings = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<SettingGroup[]>("/settings/system");
      if (res.success && res.data) {
        setGroups(res.data);

        // 폼 상태 초기화 (group → key → value)
        const initialForms: Record<string, Record<string, string>> = {};
        for (const group of res.data) {
          initialForms[group.group] = {};
          for (const setting of group.settings) {
            initialForms[group.group][setting.settingKey] =
              setting.settingValue;
          }
        }
        setForms(initialForms);
      } else {
        toast.error(res.error?.message ?? "설정을 불러올 수 없습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  // 폼 필드 변경 핸들러
  const handleChange = (group: string, key: string, value: string) => {
    setForms((prev) => ({
      ...prev,
      [group]: {
        ...prev[group],
        [key]: value,
      },
    }));
  };

  // 그룹별 저장 핸들러
  const handleSave = async (group: SettingGroup) => {
    const formData = forms[group.group];
    if (!formData) return;

    setSavingGroup(group.group);
    try {
      // 읽기 전용이 아닌 설정만 전송
      const settings = group.settings
        .filter((s) => !s.readonly)
        .map((s) => ({
          settingKey: s.settingKey,
          settingValue: formData[s.settingKey] ?? s.settingValue,
        }));

      const res: ApiResponse<void> = await apiPut(
        `/settings/system/${group.group}`,
        { settings }
      );

      if (res.success) {
        toast.success(
          `${GROUP_LABELS[group.group] ?? group.group} 설정이 저장되었습니다.`
        );
      } else {
        toast.error(res.error?.message ?? "설정 저장에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setSavingGroup(null);
    }
  };

  // 설정 값 입력 컴포넌트 렌더링 (valueType 기반)
  const renderInput = (group: string, setting: SettingItem) => {
    const currentValue = forms[group]?.[setting.settingKey] ?? setting.settingValue;
    const isDisabled = setting.readonly;

    switch (setting.valueType) {
      case "BOOLEAN":
        return (
          <button
            type="button"
            role="switch"
            aria-checked={currentValue === "true"}
            disabled={isDisabled}
            onClick={() =>
              handleChange(
                group,
                setting.settingKey,
                currentValue === "true" ? "false" : "true"
              )
            }
            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${
              currentValue === "true" ? "bg-primary" : "bg-gray-200"
            }`}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-lg ring-0 transition-transform ${
                currentValue === "true" ? "translate-x-5" : "translate-x-0"
              }`}
            />
          </button>
        );

      case "NUMBER":
        return (
          <Input
            type="number"
            value={currentValue}
            onChange={(e) =>
              handleChange(group, setting.settingKey, e.target.value)
            }
            disabled={isDisabled}
            className="max-w-xs"
          />
        );

      case "JSON":
        return (
          <textarea
            value={currentValue}
            onChange={(e) =>
              handleChange(group, setting.settingKey, e.target.value)
            }
            disabled={isDisabled}
            rows={4}
            className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] outline-none disabled:cursor-not-allowed disabled:opacity-50"
          />
        );

      default: // STRING
        return (
          <Input
            type="text"
            value={currentValue}
            onChange={(e) =>
              handleChange(group, setting.settingKey, e.target.value)
            }
            disabled={isDisabled}
            className="max-w-md"
          />
        );
    }
  };

  return (
    <>
      <PageHeader
        title="시스템 설정"
        subtitle="사이트 전역 설정을 관리합니다"
      />

      <div className="p-5 space-y-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : groups.length === 0 ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            등록된 시스템 설정이 없습니다.
          </div>
        ) : (
          groups.map((group) => (
            <Card key={group.group}>
              <CardHeader>
                <CardTitle className="text-lg">
                  {GROUP_LABELS[group.group] ?? group.group}
                </CardTitle>
                {GROUP_DESCRIPTIONS[group.group] && (
                  <CardDescription>
                    {GROUP_DESCRIPTIONS[group.group]}
                  </CardDescription>
                )}
              </CardHeader>
              <CardContent className="space-y-5">
                {group.settings.map((setting) => (
                  <div key={setting.settingKey} className="space-y-1.5">
                    <div className="flex items-center gap-2">
                      <Label className="text-sm font-medium">
                        {setting.name}
                      </Label>
                      {setting.readonly && (
                        <span title="읽기 전용">
                          <Lock
                            size={14}
                            className="text-muted-foreground"
                          />
                        </span>
                      )}
                    </div>
                    {setting.description && (
                      <p className="text-xs text-muted-foreground">
                        {setting.description}
                      </p>
                    )}
                    <div className="mt-1">
                      {renderInput(group.group, setting)}
                    </div>
                  </div>
                ))}

                {/* 그룹별 저장 버튼 */}
                <div className="flex justify-end pt-2 border-t">
                  <Button
                    onClick={() => handleSave(group)}
                    disabled={savingGroup === group.group}
                  >
                    <FloppyDisk size={16} className="mr-1" />
                    {savingGroup === group.group ? "저장 중..." : "저장"}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>
    </>
  );
}
