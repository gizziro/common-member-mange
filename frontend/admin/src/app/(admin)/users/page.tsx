"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { apiGet, apiPut, apiDelete, type ApiResponse } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
import { Pagination } from "@/components/ui";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
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
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Pencil, Trash } from "@phosphor-icons/react";
import { toast } from "sonner";

/* ===========================
 * 타입 정의
 * =========================== */

/** 사용자 목록 항목 */
interface UserListItem {
  id: string;
  userId: string;
  username: string;
  email: string;
  provider: string;
  userStatus: string;
  isLocked: boolean;
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

/** 상태 옵션 */
const STATUS_OPTIONS = [
  { value: "ACTIVE", label: "활성" },
  { value: "PENDING", label: "대기" },
  { value: "SUSPENDED", label: "정지" },
];

/** 상태별 배지 스타일 */
function statusBadgeClass(status: string): string {
  switch (status) {
    case "ACTIVE":
      return "bg-success text-white";
    case "PENDING":
      return "bg-warning text-white";
    case "SUSPENDED":
      return "bg-destructive text-white";
    default:
      return "";
  }
}

/** 회원 목록 페이지 */
export default function UsersPage() {
  const [users, setUsers] = useState<UserListItem[]>([]);
  const [loading, setLoading] = useState(true);

  /* 페이지네이션 상태 */
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 20;

  /* 삭제 확인 다이얼로그 상태 */
  const [deleteTarget, setDeleteTarget] = useState<UserListItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  /* 상태 변경 중인 사용자 ID 추적 */
  const [changingStatusId, setChangingStatusId] = useState<string | null>(null);

  /* 사용자 목록 로드 */
  const loadUsers = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const res = await apiGet<PageResponse<UserListItem>>(
        `/users?page=${p}&size=${pageSize}&sort=createdAt,desc`
      );
      if (res.success && res.data) {
        setUsers(res.data.content);
        setPage(res.data.page);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      } else {
        toast.error(res.error?.message ?? "회원 목록을 불러올 수 없습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers(0);
  }, [loadUsers]);

  /* 페이지 변경 */
  const handlePageChange = (newPage: number) => {
    loadUsers(newPage);
  };

  /* 인라인 상태 변경 */
  const handleStatusChange = async (user: UserListItem, newStatus: string) => {
    /* 동일 상태면 무시 */
    if (user.userStatus === newStatus) return;

    setChangingStatusId(user.id);

    try {
      const res: ApiResponse = await apiPut(`/users/${user.id}`, {
        username: user.username,
        email: user.email,
        userStatus: newStatus,
      });

      if (res.success) {
        /* 로컬 상태 즉시 반영 */
        setUsers((prev) =>
          prev.map((u) =>
            u.id === user.id ? { ...u, userStatus: newStatus } : u
          )
        );
        const label = STATUS_OPTIONS.find((o) => o.value === newStatus)?.label ?? newStatus;
        toast.success(`${user.username}님의 상태가 "${label}"(으)로 변경되었습니다.`);
      } else {
        toast.error(res.error?.message ?? "상태 변경에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setChangingStatusId(null);
    }
  };

  /* 사용자 삭제 */
  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);

    try {
      const res = await apiDelete(`/users/${deleteTarget.id}`);
      if (res.success) {
        toast.success(`${deleteTarget.username} 회원이 삭제되었습니다.`);
        setDeleteTarget(null);
        await loadUsers(page);
      } else {
        toast.error(res.error?.message ?? "삭제에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="회원 관리"
        subtitle={`전체 ${totalElements}명의 회원을 관리합니다`}
      />

      <div className="p-5">
        {/* 사용자 테이블 */}
        <div className="rounded-xl border bg-card shadow-sm">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          ) : users.length === 0 ? (
            <div className="py-12 text-center text-sm text-muted-foreground">
              등록된 회원이 없습니다.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="px-5">아이디</TableHead>
                  <TableHead className="px-5">이름</TableHead>
                  <TableHead className="px-5">이메일</TableHead>
                  <TableHead className="px-5">상태</TableHead>
                  <TableHead className="px-5">잠금</TableHead>
                  <TableHead className="px-5">가입일</TableHead>
                  <TableHead className="px-5 text-right">작업</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="px-5 font-mono text-xs text-muted-foreground">
                      {user.userId}
                    </TableCell>
                    <TableCell className="px-5">
                      <Link
                        href={`/users/${user.id}`}
                        className="text-primary hover:underline font-medium"
                      >
                        {user.username}
                      </Link>
                    </TableCell>
                    <TableCell className="px-5 text-muted-foreground">
                      {user.email}
                    </TableCell>
                    {/* 상태 — 인라인 Select로 바로 변경 가능 */}
                    <TableCell className="px-5">
                      <Select
                        value={user.userStatus}
                        onValueChange={(val) => handleStatusChange(user, val)}
                        disabled={changingStatusId === user.id}
                      >
                        <SelectTrigger className="h-7 w-[90px] text-xs px-2 gap-1">
                          <span className={`inline-block size-2 rounded-full ${statusBadgeClass(user.userStatus)}`} />
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {STATUS_OPTIONS.map((opt) => (
                            <SelectItem key={opt.value} value={opt.value} className="text-xs">
                              {opt.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </TableCell>
                    <TableCell className="px-5">
                      {user.isLocked ? (
                        <Badge variant="destructive">잠금</Badge>
                      ) : (
                        <Badge variant="secondary">정상</Badge>
                      )}
                    </TableCell>
                    <TableCell className="px-5 text-muted-foreground text-xs">
                      {new Date(user.createdAt).toLocaleDateString("ko-KR")}
                    </TableCell>
                    <TableCell className="px-5">
                      <div className="flex items-center justify-end gap-1">
                        <Link href={`/users/${user.id}`}>
                          <Button variant="ghost" size="icon-sm" title="상세/수정">
                            <Pencil size={14} />
                          </Button>
                        </Link>
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          title="삭제"
                          className="text-destructive hover:text-destructive"
                          onClick={() => setDeleteTarget(user)}
                        >
                          <Trash size={14} />
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

      {/* 삭제 확인 다이얼로그 */}
      <AlertDialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent size="sm">
          <AlertDialogHeader>
            <AlertDialogTitle>회원 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              <span className="font-semibold">{deleteTarget?.username}</span> ({deleteTarget?.userId}) 회원을 삭제하시겠습니까?
              이 작업은 되돌릴 수 없으며, 소속된 그룹 멤버십도 함께 삭제됩니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>취소</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deleting}
              onClick={(e) => {
                e.preventDefault();
                handleDelete();
              }}
            >
              {deleting ? "삭제 중..." : "삭제"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
