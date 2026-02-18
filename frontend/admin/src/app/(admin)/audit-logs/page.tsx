"use client";

import { useEffect, useState, useCallback } from "react";
import { apiGet, type ApiResponse } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
import { Pagination } from "@/components/ui";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
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
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { MagnifyingGlass, ArrowCounterClockwise, Eye } from "@phosphor-icons/react";
import { toast } from "sonner";

/* ===========================
 * 타입 정의
 * =========================== */

/** 감사 로그 항목 */
interface AuditLogItem {
  id:           string;
  actorUserId:  string | null;
  actorType:    string;
  actionType:   string;
  targetType:   string | null;
  targetId:     string | null;
  resultStatus: string;
  message:      string | null;
  metadataJson: string | null;
  ipAddress:    string | null;
  userAgent:    string | null;
  createdAt:    string;
}

/** 페이지네이션 응답 */
interface PageResponse<T> {
  content:       T[];
  page:          number;
  size:          number;
  totalElements: number;
  totalPages:    number;
}

/* ===========================
 * 한국어 라벨 & 배지 색상 매핑
 * =========================== */

/** 액션 유형별 한국어 라벨 + 배지 색상 */
const ACTION_TYPE_MAP: Record<string, { label: string; className: string }> = {
  LOGIN:          { label: "로그인",       className: "bg-primary text-white" },
  LOGOUT:         { label: "로그아웃",     className: "bg-primary text-white" },
  TOKEN_REFRESH:  { label: "토큰 갱신",    className: "bg-primary text-white" },
  USER_SIGNUP:    { label: "회원가입",     className: "bg-success text-white" },
  USER_UPDATE:    { label: "회원 수정",    className: "bg-success text-white" },
  USER_DELETE:    { label: "회원 삭제",    className: "bg-destructive text-white" },
  USER_UNLOCK:    { label: "잠금 해제",    className: "bg-teal-600 text-white" },
  PASSWORD_CHANGE:{ label: "비밀번호 변경", className: "bg-teal-600 text-white" },
  GROUP_CREATE:   { label: "그룹 생성",    className: "bg-purple-600 text-white" },
  GROUP_UPDATE:   { label: "그룹 수정",    className: "bg-purple-600 text-white" },
  GROUP_DELETE:   { label: "그룹 삭제",    className: "bg-destructive text-white" },
  MEMBER_ADD:     { label: "멤버 추가",    className: "bg-indigo-600 text-white" },
  MEMBER_REMOVE:  { label: "멤버 제거",    className: "bg-indigo-600 text-white" },
  SETTING_CHANGE: { label: "설정 변경",    className: "bg-gray-600 text-white" },
  OAUTH2_LOGIN:   { label: "소셜 로그인",  className: "bg-cyan-600 text-white" },
  OAUTH2_LINK:    { label: "소셜 연동",    className: "bg-cyan-600 text-white" },
  OAUTH2_UNLINK:  { label: "소셜 해제",    className: "bg-cyan-600 text-white" },
};

/** 행위자 유형 한국어 */
const ACTOR_TYPE_MAP: Record<string, string> = {
  USER:   "사용자",
  SYSTEM: "시스템",
};

/** 대상 유형 한국어 */
const TARGET_TYPE_MAP: Record<string, string> = {
  USER:     "사용자",
  GROUP:    "그룹",
  MEMBER:   "멤버",
  SETTING:  "설정",
  IDENTITY: "소셜 연동",
};

/** 액션 유형 Select 옵션 (전체 + 17개) */
const ACTION_TYPE_OPTIONS = [
  { value: "__all__", label: "전체" },
  ...Object.entries(ACTION_TYPE_MAP).map(([value, { label }]) => ({ value, label })),
];

/** 결과 상태 Select 옵션 */
const RESULT_STATUS_OPTIONS = [
  { value: "__all__", label: "전체" },
  { value: "SUCCESS", label: "성공" },
  { value: "FAILURE", label: "실패" },
];

/* ===========================
 * 감사 로그 페이지
 * =========================== */

/** 감사 로그 조회 페이지 */
export default function AuditLogsPage() {
  /* 데이터 상태 */
  const [logs, setLogs]                   = useState<AuditLogItem[]>([]);
  const [loading, setLoading]             = useState(true);

  /* 페이지네이션 상태 */
  const [page, setPage]                   = useState(0);
  const [totalPages, setTotalPages]       = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 20;

  /* 필터 상태 */
  const [filterAction, setFilterAction]   = useState("__all__");
  const [filterResult, setFilterResult]   = useState("__all__");
  const [filterStartDate, setFilterStartDate] = useState("");
  const [filterEndDate, setFilterEndDate]     = useState("");

  /* 상세 Dialog 상태 */
  const [detailLog, setDetailLog] = useState<AuditLogItem | null>(null);

  /* 감사 로그 목록 로드 */
  const loadAuditLogs = useCallback(async (p: number) => {
    setLoading(true);
    try {
      /* 쿼리 파라미터 구성 */
      const params = new URLSearchParams();
      params.set("page", String(p));
      params.set("size", String(pageSize));
      params.set("sort", "createdAt,desc");

      /* 액션 유형 필터 (서버 파라미터) */
      if (filterAction !== "__all__") {
        params.set("actionType", filterAction);
      }
      /* 시작일 필터 */
      if (filterStartDate) {
        params.set("startTime", `${filterStartDate}T00:00:00`);
      }
      /* 종료일 필터 */
      if (filterEndDate) {
        params.set("endTime", `${filterEndDate}T23:59:59`);
      }

      const res = await apiGet<PageResponse<AuditLogItem>>(
        `/audit-logs?${params.toString()}`
      );

      if (res.success && res.data) {
        /* 결과 상태 클라이언트 필터링 (백엔드 미지원) */
        const filtered = filterResult !== "__all__"
          ? res.data.content.filter((log) => log.resultStatus === filterResult)
          : res.data.content;

        setLogs(filtered);
        setPage(res.data.page);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      } else {
        toast.error(res.error?.message ?? "감사 로그를 불러올 수 없습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, [filterAction, filterResult, filterStartDate, filterEndDate]);

  /* 초기 로드 */
  useEffect(() => {
    loadAuditLogs(0);
  }, [loadAuditLogs]);

  /* 페이지 변경 */
  const handlePageChange = (newPage: number) => {
    loadAuditLogs(newPage);
  };

  /* 검색 */
  const handleSearch = () => {
    loadAuditLogs(0);
  };

  /* 필터 초기화 */
  const handleReset = () => {
    setFilterAction("__all__");
    setFilterResult("__all__");
    setFilterStartDate("");
    setFilterEndDate("");
  };

  /** 액션 유형 배지 렌더링 */
  const renderActionBadge = (actionType: string) => {
    const info = ACTION_TYPE_MAP[actionType];
    if (!info) return <span className="text-xs text-muted-foreground">{actionType}</span>;
    return (
      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${info.className}`}>
        {info.label}
      </span>
    );
  };

  /** 메타데이터 JSON 포맷팅 */
  const formatMetadata = (json: string | null): string => {
    if (!json) return "-";
    try {
      return JSON.stringify(JSON.parse(json), null, 2);
    } catch {
      return json;
    }
  };

  return (
    <>
      <PageHeader
        title="감사 로그"
        subtitle={`전체 ${totalElements}건의 감사 로그를 조회합니다`}
      />

      <div className="p-5">
        {/* 필터 바 */}
        <div className="mb-4 flex flex-wrap items-end gap-3 rounded-xl border bg-card p-4 shadow-sm">
          {/* 액션 유형 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-muted-foreground">액션 유형</label>
            <Select value={filterAction} onValueChange={setFilterAction}>
              <SelectTrigger className="h-9 w-[150px] text-sm">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {ACTION_TYPE_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value} className="text-sm">
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* 결과 상태 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-muted-foreground">결과</label>
            <Select value={filterResult} onValueChange={setFilterResult}>
              <SelectTrigger className="h-9 w-[110px] text-sm">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {RESULT_STATUS_OPTIONS.map((opt) => (
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

          {/* 검색 / 초기화 버튼 */}
          <div className="flex items-center gap-2">
            <Button variant="default" size="sm" onClick={handleSearch}>
              <MagnifyingGlass size={14} className="mr-1" />
              검색
            </Button>
            <Button variant="outline" size="sm" onClick={handleReset}>
              <ArrowCounterClockwise size={14} className="mr-1" />
              초기화
            </Button>
          </div>
        </div>

        {/* 감사 로그 테이블 */}
        <div className="rounded-xl border bg-card shadow-sm">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          ) : logs.length === 0 ? (
            <div className="py-12 text-center text-sm text-muted-foreground">
              조회된 감사 로그가 없습니다.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="px-5">일시</TableHead>
                  <TableHead className="px-5">행위자</TableHead>
                  <TableHead className="px-5">액션</TableHead>
                  <TableHead className="px-5">대상</TableHead>
                  <TableHead className="px-5">결과</TableHead>
                  <TableHead className="px-5 max-w-[200px]">메시지</TableHead>
                  <TableHead className="px-5">IP</TableHead>
                  <TableHead className="px-5 text-right">상세</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {logs.map((log) => (
                  <TableRow key={log.id}>
                    {/* 일시 */}
                    <TableCell className="px-5 text-xs text-muted-foreground whitespace-nowrap">
                      {new Date(log.createdAt).toLocaleString("ko-KR")}
                    </TableCell>
                    {/* 행위자 */}
                    <TableCell className="px-5">
                      <Badge variant="secondary" className="text-xs">
                        {ACTOR_TYPE_MAP[log.actorType] ?? log.actorType}
                      </Badge>
                    </TableCell>
                    {/* 액션 */}
                    <TableCell className="px-5">
                      {renderActionBadge(log.actionType)}
                    </TableCell>
                    {/* 대상 */}
                    <TableCell className="px-5 text-xs text-muted-foreground">
                      {log.targetType ? (TARGET_TYPE_MAP[log.targetType] ?? log.targetType) : "-"}
                    </TableCell>
                    {/* 결과 */}
                    <TableCell className="px-5">
                      {log.resultStatus === "SUCCESS" ? (
                        <Badge className="bg-success text-white text-xs">성공</Badge>
                      ) : (
                        <Badge variant="destructive" className="text-xs">실패</Badge>
                      )}
                    </TableCell>
                    {/* 메시지 */}
                    <TableCell className="px-5 max-w-[200px] truncate text-xs text-muted-foreground" title={log.message ?? ""}>
                      {log.message ?? "-"}
                    </TableCell>
                    {/* IP */}
                    <TableCell className="px-5 font-mono text-xs text-muted-foreground">
                      {log.ipAddress ?? "-"}
                    </TableCell>
                    {/* 상세 */}
                    <TableCell className="px-5">
                      <div className="flex justify-end">
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          title="상세 보기"
                          onClick={() => setDetailLog(log)}
                        >
                          <Eye size={14} />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>

        {/* 페이지네이션 */}
        {totalPages > 1 && (
          <div className="mt-4 flex justify-center">
            <Pagination
              page={page}
              totalPages={totalPages}
              onPageChange={handlePageChange}
            />
          </div>
        )}
      </div>

      {/* 상세 Dialog */}
      <Dialog open={!!detailLog} onOpenChange={(open) => !open && setDetailLog(null)}>
        <DialogContent className="sm:max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>감사 로그 상세</DialogTitle>
          </DialogHeader>

          {detailLog && (
            <div className="space-y-4">
              {/* 2열 그리드 기본 정보 */}
              <div className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
                <div>
                  <span className="text-muted-foreground">일시</span>
                  <p className="font-medium">{new Date(detailLog.createdAt).toLocaleString("ko-KR")}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">행위자 유형</span>
                  <p className="font-medium">{ACTOR_TYPE_MAP[detailLog.actorType] ?? detailLog.actorType}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">행위자 ID</span>
                  <p className="font-mono text-xs">{detailLog.actorUserId ?? "-"}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">액션</span>
                  <div className="mt-0.5">{renderActionBadge(detailLog.actionType)}</div>
                </div>
                <div>
                  <span className="text-muted-foreground">대상 유형</span>
                  <p className="font-medium">{detailLog.targetType ? (TARGET_TYPE_MAP[detailLog.targetType] ?? detailLog.targetType) : "-"}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">대상 ID</span>
                  <p className="font-mono text-xs">{detailLog.targetId ?? "-"}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">결과</span>
                  <div className="mt-0.5">
                    {detailLog.resultStatus === "SUCCESS" ? (
                      <Badge className="bg-success text-white text-xs">성공</Badge>
                    ) : (
                      <Badge variant="destructive" className="text-xs">실패</Badge>
                    )}
                  </div>
                </div>
                <div>
                  <span className="text-muted-foreground">IP 주소</span>
                  <p className="font-mono text-xs">{detailLog.ipAddress ?? "-"}</p>
                </div>
              </div>

              {/* 메시지 */}
              <div className="text-sm">
                <span className="text-muted-foreground">메시지</span>
                <p className="mt-1">{detailLog.message ?? "-"}</p>
              </div>

              {/* User-Agent */}
              <div className="text-sm">
                <span className="text-muted-foreground">User-Agent</span>
                <p className="mt-1 font-mono text-xs break-all">{detailLog.userAgent ?? "-"}</p>
              </div>

              {/* 메타데이터 JSON */}
              {detailLog.metadataJson && (
                <div className="text-sm">
                  <span className="text-muted-foreground">메타데이터</span>
                  <pre className="mt-1 rounded-md bg-muted p-3 text-xs overflow-x-auto">
                    {formatMetadata(detailLog.metadataJson)}
                  </pre>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
