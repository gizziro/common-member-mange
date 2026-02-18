"use client";

import { useEffect, useState, useCallback } from "react";
import { apiGet, apiPost, type ApiResponse } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
import { Pagination } from "@/components/ui";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "@/components/ui/Card";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { PaperPlaneTilt, MagnifyingGlass, ArrowCounterClockwise } from "@phosphor-icons/react";
import { toast } from "sonner";

/* ===========================
 * 타입 정의
 * =========================== */

/** 그룹별 수신자 정보 */
interface GroupRecipient {
  groupId: string;
  groupName: string;
  groupCode: string;
  recipientCount: number;
}

/** 발송 결과 */
interface SmsSendResult {
  batchId: string;
  totalCount: number;
  successCount: number;
  failCount: number;
}

/** SMS 로그 항목 */
interface SmsLogItem {
  id: string;
  sendType: string;
  triggerType: string | null;
  senderUserId: string | null;
  recipientPhone: string;
  recipientUserId: string | null;
  message: string;
  providerCode: string | null;
  sendStatus: string;
  errorMessage: string | null;
  batchId: string | null;
  createdAt: string;
}

/** 페이지네이션 응답 */
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** 수신 대상 유형 옵션 */
const RECIPIENT_TYPE_OPTIONS = [
  { value: "ALL", label: "전체 회원" },
  { value: "GROUP", label: "그룹별" },
  { value: "INDIVIDUAL", label: "개별 회원" },
];

/** 발송 유형 필터 옵션 */
const SEND_TYPE_OPTIONS = [
  { value: "__all__", label: "전체" },
  { value: "MANUAL", label: "수동" },
  { value: "AUTO", label: "자동" },
];

/* ===========================
 * SMS 발송 페이지
 * =========================== */
export default function SmsPage() {
  /* 탭 상태: send (발송) / logs (이력) */
  const [activeTab, setActiveTab] = useState<"send" | "logs">("send");

  /* ===========================
   * SMS 발송 탭 상태
   * =========================== */
  const [recipientType, setRecipientType] = useState("ALL");
  const [message, setMessage] = useState("");
  const [sending, setSending] = useState(false);

  /* 수신 가능 회원 수 */
  const [totalRecipientCount, setTotalRecipientCount] = useState<number | null>(null);

  /* 그룹별 수신자 */
  const [groupRecipients, setGroupRecipients] = useState<GroupRecipient[]>([]);
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>([]);
  const [groupLoading, setGroupLoading] = useState(false);

  /* 개별 회원 ID 입력 */
  const [individualUserIds, setIndividualUserIds] = useState("");

  /* 발송 결과 다이얼로그 */
  const [sendResult, setSendResult] = useState<SmsSendResult | null>(null);

  /* ===========================
   * SMS 이력 탭 상태
   * =========================== */
  const [logs, setLogs] = useState<SmsLogItem[]>([]);
  const [logsLoading, setLogsLoading] = useState(false);
  const [logsPage, setLogsPage] = useState(0);
  const [logsTotalPages, setLogsTotalPages] = useState(0);
  const [logsTotalElements, setLogsTotalElements] = useState(0);
  const [filterSendType, setFilterSendType] = useState("__all__");
  const [filterStartDate, setFilterStartDate] = useState("");
  const [filterEndDate, setFilterEndDate] = useState("");

  /* ===========================
   * 수신 대상 조회
   * =========================== */

  /** 전체 수신 가능 회원 수 로드 */
  const loadRecipientCount = useCallback(async () => {
    try {
      const res = await apiGet<{ totalCount: number }>("/sms/recipients/count");
      if (res.success && res.data) {
        setTotalRecipientCount(res.data.totalCount);
      }
    } catch {
      /* 무시 */
    }
  }, []);

  /** 그룹별 수신 가능 회원 로드 */
  const loadGroupRecipients = useCallback(async () => {
    setGroupLoading(true);
    try {
      const res = await apiGet<GroupRecipient[]>("/sms/recipients/groups");
      if (res.success && res.data) {
        setGroupRecipients(res.data);
      }
    } catch {
      /* 무시 */
    } finally {
      setGroupLoading(false);
    }
  }, []);

  /* 발송 탭 진입 시 수신 가능 회원 수 로드 */
  useEffect(() => {
    loadRecipientCount();
  }, [loadRecipientCount]);

  /* 그룹 선택 시 그룹 목록 로드 */
  useEffect(() => {
    if (recipientType === "GROUP" && groupRecipients.length === 0) {
      loadGroupRecipients();
    }
  }, [recipientType, groupRecipients.length, loadGroupRecipients]);

  /* ===========================
   * SMS 발송
   * =========================== */
  const handleSend = async () => {
    if (!message.trim()) {
      toast.error("메시지를 입력해 주세요.");
      return;
    }

    if (recipientType === "GROUP" && selectedGroupIds.length === 0) {
      toast.error("발송 대상 그룹을 선택해 주세요.");
      return;
    }

    if (recipientType === "INDIVIDUAL" && !individualUserIds.trim()) {
      toast.error("발송 대상 회원 ID를 입력해 주세요.");
      return;
    }

    setSending(true);
    try {
      const body: Record<string, unknown> = {
        recipientType,
        message: message.trim(),
      };

      if (recipientType === "GROUP") {
        body.groupIds = selectedGroupIds;
      }

      if (recipientType === "INDIVIDUAL") {
        body.userIds = individualUserIds
          .split(",")
          .map((id) => id.trim())
          .filter(Boolean);
      }

      const res = await apiPost<SmsSendResult>("/sms/send", body);
      if (res.success && res.data) {
        setSendResult(res.data);
        toast.success(`SMS 발송 완료 (${res.data.successCount}/${res.data.totalCount}건)`);
      } else {
        toast.error(res.error?.message ?? "SMS 발송에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setSending(false);
    }
  };

  /* ===========================
   * SMS 이력 조회
   * =========================== */
  const loadLogs = useCallback(
    async (p: number) => {
      setLogsLoading(true);
      try {
        const params = new URLSearchParams();
        params.set("page", String(p));
        params.set("size", "20");
        params.set("sort", "createdAt,desc");

        if (filterSendType !== "__all__") {
          params.set("sendType", filterSendType);
        }
        if (filterStartDate) {
          params.set("startTime", `${filterStartDate}T00:00:00`);
        }
        if (filterEndDate) {
          params.set("endTime", `${filterEndDate}T23:59:59`);
        }

        const res = await apiGet<PageResponse<SmsLogItem>>(
          `/sms/logs?${params.toString()}`
        );

        if (res.success && res.data) {
          setLogs(res.data.content);
          setLogsPage(res.data.page);
          setLogsTotalPages(res.data.totalPages);
          setLogsTotalElements(res.data.totalElements);
        } else {
          toast.error(res.error?.message ?? "이력을 불러올 수 없습니다.");
        }
      } catch {
        toast.error("서버에 연결할 수 없습니다.");
      } finally {
        setLogsLoading(false);
      }
    },
    [filterSendType, filterStartDate, filterEndDate]
  );

  /* 이력 탭 진입 시 로드 */
  useEffect(() => {
    if (activeTab === "logs") {
      loadLogs(0);
    }
  }, [activeTab, loadLogs]);

  /* 그룹 체크박스 토글 */
  const toggleGroupSelection = (groupId: string) => {
    setSelectedGroupIds((prev) =>
      prev.includes(groupId)
        ? prev.filter((id) => id !== groupId)
        : [...prev, groupId]
    );
  };

  return (
    <>
      <PageHeader
        title="SMS 발송"
        subtitle="회원에게 SMS를 발송하고 이력을 확인합니다"
      />

      <div className="p-5">
        {/* 탭 헤더 */}
        <div className="mb-4 flex gap-1 rounded-lg border bg-muted/50 p-1 w-fit">
          <button
            type="button"
            className={`rounded-md px-4 py-1.5 text-sm font-medium transition-colors ${
              activeTab === "send"
                ? "bg-background shadow-sm"
                : "text-muted-foreground hover:text-foreground"
            }`}
            onClick={() => setActiveTab("send")}
          >
            SMS 발송
          </button>
          <button
            type="button"
            className={`rounded-md px-4 py-1.5 text-sm font-medium transition-colors ${
              activeTab === "logs"
                ? "bg-background shadow-sm"
                : "text-muted-foreground hover:text-foreground"
            }`}
            onClick={() => setActiveTab("logs")}
          >
            발송 이력
          </button>
        </div>

        {/* ===========================
         * 탭 1: SMS 발송
         * =========================== */}
        {activeTab === "send" && (
          <Card>
            <CardHeader className="border-b">
              <CardTitle>SMS 발송</CardTitle>
            </CardHeader>
            <CardContent className="pt-6 space-y-6 max-w-2xl">
              {/* 수신 가능 회원 수 안내 */}
              {totalRecipientCount !== null && (
                <div className="rounded-lg border bg-muted/30 p-3 text-sm text-muted-foreground">
                  SMS 수신 가능 회원: <span className="font-semibold text-foreground">{totalRecipientCount}명</span>
                  <span className="ml-1 text-xs">(전화번호 등록 + SMS 동의 + 활성 상태)</span>
                </div>
              )}

              {/* 수신 대상 유형 */}
              <div className="space-y-2">
                <Label>수신 대상</Label>
                <Select value={recipientType} onValueChange={(v) => {
                  setRecipientType(v);
                  setSelectedGroupIds([]);
                  setIndividualUserIds("");
                }}>
                  <SelectTrigger className="w-full max-w-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {RECIPIENT_TYPE_OPTIONS.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 그룹 선택 (GROUP 유형) */}
              {recipientType === "GROUP" && (
                <div className="space-y-2">
                  <Label>대상 그룹</Label>
                  {groupLoading ? (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                      로딩 중...
                    </div>
                  ) : groupRecipients.length === 0 ? (
                    <p className="text-sm text-muted-foreground">그룹이 없습니다.</p>
                  ) : (
                    <div className="space-y-1.5 max-h-48 overflow-y-auto rounded-md border p-3">
                      {groupRecipients.map((group) => (
                        <label
                          key={group.groupId}
                          className="flex items-center gap-2 text-sm cursor-pointer hover:bg-muted/50 rounded px-2 py-1"
                        >
                          <input
                            type="checkbox"
                            checked={selectedGroupIds.includes(group.groupId)}
                            onChange={() => toggleGroupSelection(group.groupId)}
                            className="rounded border-input"
                          />
                          <span className="font-medium">{group.groupName}</span>
                          <span className="text-xs text-muted-foreground font-mono">({group.groupCode})</span>
                          <span className="ml-auto text-xs text-muted-foreground">{group.recipientCount}명</span>
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* 개별 회원 ID 입력 (INDIVIDUAL 유형) */}
              {recipientType === "INDIVIDUAL" && (
                <div className="space-y-2">
                  <Label htmlFor="userIds">회원 ID (쉼표로 구분)</Label>
                  <Input
                    id="userIds"
                    placeholder="user1, user2, ..."
                    value={individualUserIds}
                    onChange={(e) => setIndividualUserIds(e.target.value)}
                  />
                  <p className="text-xs text-muted-foreground">
                    회원의 로그인 ID를 입력하세요. SMS 수신 가능(전화번호 등록 + SMS 동의 + 활성)한 회원에게만 발송됩니다.
                  </p>
                </div>
              )}

              {/* 메시지 입력 */}
              <div className="space-y-2">
                <Label htmlFor="message">
                  메시지 <span className="text-destructive">*</span>
                </Label>
                <textarea
                  id="message"
                  className="w-full min-h-[120px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring resize-y"
                  placeholder="발송할 메시지를 입력하세요"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  maxLength={2000}
                />
                <p className="text-xs text-muted-foreground text-right">
                  {message.length} / 2,000자
                </p>
              </div>

              {/* 발송 버튼 */}
              <div>
                <Button
                  onClick={handleSend}
                  disabled={sending || !message.trim()}
                  size="sm"
                >
                  <PaperPlaneTilt size={16} className="mr-1.5" />
                  {sending ? "발송 중..." : "SMS 발송"}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* ===========================
         * 탭 2: 발송 이력
         * =========================== */}
        {activeTab === "logs" && (
          <>
            {/* 필터 바 */}
            <div className="mb-4 flex flex-wrap items-end gap-3 rounded-xl border bg-card p-4 shadow-sm">
              {/* 발송 유형 */}
              <div className="flex flex-col gap-1">
                <label className="text-xs font-medium text-muted-foreground">발송 유형</label>
                <Select value={filterSendType} onValueChange={setFilterSendType}>
                  <SelectTrigger className="h-9 w-[120px] text-sm">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {SEND_TYPE_OPTIONS.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value} className="text-sm">
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 시작일 */}
              <div className="flex flex-col gap-1">
                <label className="text-xs font-medium text-muted-foreground">시작일</label>
                <Input
                  type="date"
                  value={filterStartDate}
                  onChange={(e) => setFilterStartDate(e.target.value)}
                  className="h-9 w-[160px] text-sm"
                />
              </div>

              {/* 종료일 */}
              <div className="flex flex-col gap-1">
                <label className="text-xs font-medium text-muted-foreground">종료일</label>
                <Input
                  type="date"
                  value={filterEndDate}
                  onChange={(e) => setFilterEndDate(e.target.value)}
                  className="h-9 w-[160px] text-sm"
                />
              </div>

              {/* 검색 / 초기화 */}
              <div className="flex items-center gap-2">
                <Button variant="default" size="sm" onClick={() => loadLogs(0)}>
                  <MagnifyingGlass size={14} className="mr-1" />
                  검색
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setFilterSendType("__all__");
                    setFilterStartDate("");
                    setFilterEndDate("");
                  }}
                >
                  <ArrowCounterClockwise size={14} className="mr-1" />
                  초기화
                </Button>
              </div>
            </div>

            {/* 이력 테이블 */}
            <div className="rounded-xl border bg-card shadow-sm">
              {logsLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : logs.length === 0 ? (
                <div className="py-12 text-center text-sm text-muted-foreground">
                  조회된 발송 이력이 없습니다.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/50">
                      <TableHead className="px-5">일시</TableHead>
                      <TableHead className="px-5">유형</TableHead>
                      <TableHead className="px-5">트리거</TableHead>
                      <TableHead className="px-5">수신번호</TableHead>
                      <TableHead className="px-5">상태</TableHead>
                      <TableHead className="px-5 max-w-[250px]">메시지</TableHead>
                      <TableHead className="px-5">배치 ID</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {logs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell className="px-5 text-xs text-muted-foreground whitespace-nowrap">
                          {new Date(log.createdAt).toLocaleString("ko-KR")}
                        </TableCell>
                        <TableCell className="px-5">
                          {log.sendType === "MANUAL" ? (
                            <Badge variant="secondary" className="text-xs">수동</Badge>
                          ) : (
                            <Badge className="bg-blue-100 text-blue-700 text-xs">자동</Badge>
                          )}
                        </TableCell>
                        <TableCell className="px-5 text-xs text-muted-foreground">
                          {log.triggerType ?? "-"}
                        </TableCell>
                        <TableCell className="px-5 font-mono text-xs">
                          {log.recipientPhone}
                        </TableCell>
                        <TableCell className="px-5">
                          {log.sendStatus === "SUCCESS" ? (
                            <Badge className="bg-success text-white text-xs">성공</Badge>
                          ) : log.sendStatus === "FAILED" ? (
                            <Badge variant="destructive" className="text-xs" title={log.errorMessage ?? undefined}>
                              실패
                            </Badge>
                          ) : (
                            <Badge variant="secondary" className="text-xs">대기</Badge>
                          )}
                        </TableCell>
                        <TableCell className="px-5 max-w-[250px] truncate text-xs text-muted-foreground" title={log.message}>
                          {log.message}
                        </TableCell>
                        <TableCell className="px-5 font-mono text-xs text-muted-foreground">
                          {log.batchId ? log.batchId.substring(0, 8) + "..." : "-"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </div>

            {/* 페이지네이션 */}
            {logsTotalPages > 1 && (
              <div className="mt-4 flex justify-center">
                <Pagination
                  page={logsPage}
                  totalPages={logsTotalPages}
                  onPageChange={(p) => loadLogs(p)}
                />
              </div>
            )}
          </>
        )}
      </div>

      {/* 발송 결과 다이얼로그 */}
      <Dialog open={!!sendResult} onOpenChange={(open) => !open && setSendResult(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>SMS 발송 결과</DialogTitle>
            <DialogDescription>발송이 완료되었습니다.</DialogDescription>
          </DialogHeader>
          {sendResult && (
            <div className="space-y-3 py-2">
              <div className="grid grid-cols-3 gap-4 text-center">
                <div>
                  <p className="text-2xl font-bold">{sendResult.totalCount}</p>
                  <p className="text-xs text-muted-foreground">전체</p>
                </div>
                <div>
                  <p className="text-2xl font-bold text-green-600">{sendResult.successCount}</p>
                  <p className="text-xs text-muted-foreground">성공</p>
                </div>
                <div>
                  <p className="text-2xl font-bold text-red-600">{sendResult.failCount}</p>
                  <p className="text-xs text-muted-foreground">실패</p>
                </div>
              </div>
              <p className="text-xs text-muted-foreground text-center font-mono">
                배치 ID: {sendResult.batchId}
              </p>
            </div>
          )}
          <DialogFooter>
            <Button onClick={() => setSendResult(null)}>확인</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
