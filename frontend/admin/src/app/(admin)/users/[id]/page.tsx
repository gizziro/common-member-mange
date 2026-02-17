"use client";

import { useEffect, useState, useCallback, type FormEvent } from "react";
import { useParams, useRouter } from "next/navigation";
import { apiGet, apiPut, type ApiResponse } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ArrowLeft, FloppyDisk, LockOpen, Key } from "@phosphor-icons/react";
import { toast } from "sonner";

/* ===========================
 * 타입 정의
 * =========================== */

/** 사용자 상세 응답 */
interface UserDetail {
  id: string;
  userId: string;
  username: string;
  email: string;
  provider: string;
  emailVerified: boolean;
  phone: string | null;
  phoneVerified: boolean;
  isOtpUse: boolean;
  loginFailCount: number;
  isLocked: boolean;
  lockedAt: string | null;
  userStatus: string;
  createdAt: string;
  updatedAt: string;
}

/** 소속 그룹 */
interface Group {
  id: string;
  groupCode: string;
  groupName: string;
  description: string | null;
  isSystem: boolean;
  memberCount: number;
}

/** 소셜 연동 정보 */
interface UserIdentity {
  id: string;
  providerCode: string;
  providerName: string;
  providerSubject: string;
  linkedAt: string;
}

/** 권한 항목 */
interface PermissionItem {
  resource: string;
  action: string;
  name: string;
}

/** 권한 요약 */
interface PermissionSummary {
  instanceId: string;
  instanceName: string;
  instanceSlug: string;
  moduleCode: string;
  moduleName: string;
  source: string;
  permissions: PermissionItem[];
}

/** 사용자 상태 옵션 */
const STATUS_OPTIONS = [
  { value: "ACTIVE", label: "활성" },
  { value: "PENDING", label: "대기" },
  { value: "SUSPENDED", label: "정지" },
];

/** 회원 상세 + 수정 페이지 */
export default function UserDetailPage() {
  const params = useParams();
  const router = useRouter();
  const userId = params.id as string;

  /* 사용자 정보 상태 */
  const [user, setUser] = useState<UserDetail | null>(null);
  const [editUsername, setEditUsername] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editStatus, setEditStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  /* 잠금 해제 상태 */
  const [unlocking, setUnlocking] = useState(false);

  /* 소속 그룹 상태 */
  const [groups, setGroups] = useState<Group[]>([]);
  const [groupsLoading, setGroupsLoading] = useState(true);

  /* 소셜 연동 상태 */
  const [identities, setIdentities] = useState<UserIdentity[]>([]);
  const [identitiesLoading, setIdentitiesLoading] = useState(true);

  /* 권한 요약 상태 */
  const [permSummary, setPermSummary] = useState<PermissionSummary[]>([]);
  const [permSummaryLoading, setPermSummaryLoading] = useState(true);

  /* 비밀번호 변경 다이얼로그 상태 */
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  /* 사용자 정보 로드 */
  const loadUser = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<UserDetail>(`/users/${userId}`);
      if (res.success && res.data) {
        setUser(res.data);
        setEditUsername(res.data.username);
        setEditEmail(res.data.email);
        setEditStatus(res.data.userStatus);
      } else {
        toast.error(res.error?.message ?? "사용자 정보를 불러올 수 없습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, [userId]);

  /* 소속 그룹 로드 */
  const loadGroups = useCallback(async () => {
    setGroupsLoading(true);
    try {
      const res = await apiGet<Group[]>(`/users/${userId}/groups`);
      if (res.success && res.data) {
        setGroups(res.data);
      } else {
        toast.error(res.error?.message ?? "소속 그룹을 불러올 수 없습니다.");
      }
    } catch {
      toast.error("그룹 목록 로드 중 오류가 발생했습니다.");
    } finally {
      setGroupsLoading(false);
    }
  }, [userId]);

  /* 소셜 연동 로드 */
  const loadIdentities = useCallback(async () => {
    setIdentitiesLoading(true);
    try {
      const res = await apiGet<UserIdentity[]>(`/users/${userId}/identities`);
      if (res.success && res.data) {
        setIdentities(res.data);
      }
    } catch {
      /* 소셜 연동 조회 실패 시 빈 배열 유지 */
    } finally {
      setIdentitiesLoading(false);
    }
  }, [userId]);

  /* 권한 요약 로드 */
  const loadPermSummary = useCallback(async () => {
    setPermSummaryLoading(true);
    try {
      const res = await apiGet<PermissionSummary[]>(`/users/${userId}/permissions`);
      if (res.success && res.data) {
        setPermSummary(res.data);
      }
    } catch {
      /* 권한 요약 조회 실패 시 빈 배열 유지 */
    } finally {
      setPermSummaryLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    loadUser();
    loadGroups();
    loadIdentities();
    loadPermSummary();
  }, [loadUser, loadGroups, loadIdentities, loadPermSummary]);

  /* 사용자 정보 수정 */
  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!user) return;

    setSaving(true);

    try {
      const res: ApiResponse = await apiPut(`/users/${userId}`, {
        username: editUsername,
        email: editEmail,
        userStatus: editStatus,
      });

      if (res.success) {
        toast.success("저장되었습니다.");
        await loadUser();
      } else {
        toast.error(res.error?.message ?? "수정에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setSaving(false);
    }
  };

  /* 잠금 해제 */
  const handleUnlock = async () => {
    setUnlocking(true);

    try {
      const res: ApiResponse = await apiPut(`/users/${userId}/unlock`);
      if (res.success) {
        toast.success("계정 잠금이 해제되었습니다.");
        await loadUser();
      } else {
        toast.error(res.error?.message ?? "잠금 해제에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setUnlocking(false);
    }
  };

  /* 비밀번호 변경 */
  const handleChangePassword = async () => {
    if (!newPassword || newPassword.length < 8) {
      toast.error("비밀번호는 8자 이상이어야 합니다.");
      return;
    }

    setChangingPassword(true);

    try {
      const res: ApiResponse = await apiPut(`/users/${userId}/password`, {
        newPassword,
      });

      if (res.success) {
        toast.success("비밀번호가 변경되었습니다.");
        setPasswordDialogOpen(false);
        setNewPassword("");
      } else {
        toast.error(res.error?.message ?? "비밀번호 변경에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setChangingPassword(false);
    }
  };

  /* 로딩 */
  if (loading) {
    return (
      <>
        <PageHeader title="회원 상세" />
        <div className="flex items-center justify-center py-24">
          <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      </>
    );
  }

  if (!user) {
    return (
      <>
        <PageHeader title="회원 상세" />
        <div className="p-5">
          <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive">
            사용자를 찾을 수 없습니다.
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title={user.username}
        subtitle={`아이디: ${user.userId}`}
        actions={
          <Button variant="ghost" size="sm" onClick={() => router.push("/users")}>
            <ArrowLeft size={16} className="mr-1.5" />
            목록으로
          </Button>
        }
      />

      <div className="p-5 space-y-5">
        {/* Card 1: 사용자 정보 수정 */}
        <Card>
          <CardHeader className="border-b">
            <CardTitle>사용자 정보</CardTitle>
          </CardHeader>
          <CardContent className="pt-6">
            <form onSubmit={handleSave} className="space-y-4 max-w-lg">
              {/* 로그인 ID (읽기 전용) */}
              <div className="space-y-2">
                <Label>로그인 ID</Label>
                <Input type="text" value={user.userId} disabled />
              </div>

              {/* 이름 */}
              <div className="space-y-2">
                <Label htmlFor="editUsername">
                  이름 <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="editUsername"
                  type="text"
                  value={editUsername}
                  onChange={(e) => setEditUsername(e.target.value)}
                  required
                />
              </div>

              {/* 이메일 */}
              <div className="space-y-2">
                <Label htmlFor="editEmail">
                  이메일 <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="editEmail"
                  type="email"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
                  required
                />
              </div>

              {/* 상태 */}
              <div className="space-y-2">
                <Label>
                  상태 <span className="text-destructive">*</span>
                </Label>
                <Select value={editStatus} onValueChange={setEditStatus}>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {STATUS_OPTIONS.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 저장 버튼 */}
              <div>
                <Button type="submit" size="sm" disabled={saving}>
                  <FloppyDisk size={16} className="mr-1.5" />
                  {saving ? "저장 중..." : "저장"}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Card 2: 계정 상태 */}
        <Card>
          <CardHeader className="border-b">
            <CardTitle>계정 상태</CardTitle>
          </CardHeader>
          <CardContent className="pt-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 max-w-lg">
              <div>
                <p className="text-xs text-muted-foreground mb-1">이메일 인증</p>
                <p className="text-sm">
                  {user.emailVerified ? (
                    <Badge className="bg-success text-white">인증됨</Badge>
                  ) : (
                    <Badge variant="secondary">미인증</Badge>
                  )}
                </p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">전화번호 인증</p>
                <p className="text-sm">
                  {user.phoneVerified ? (
                    <Badge className="bg-success text-white">인증됨</Badge>
                  ) : (
                    <Badge variant="secondary">미인증</Badge>
                  )}
                </p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">OTP 사용</p>
                <p className="text-sm">
                  {user.isOtpUse ? (
                    <Badge className="bg-success text-white">사용 중</Badge>
                  ) : (
                    <Badge variant="secondary">미사용</Badge>
                  )}
                </p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">로그인 실패 횟수</p>
                <p className="text-sm font-mono">{user.loginFailCount}회</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">잠금 상태</p>
                <div className="flex items-center gap-2">
                  {user.isLocked ? (
                    <>
                      <Badge variant="destructive">잠금</Badge>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={unlocking}
                        onClick={handleUnlock}
                      >
                        <LockOpen size={14} className="mr-1" />
                        {unlocking ? "해제 중..." : "해제"}
                      </Button>
                    </>
                  ) : (
                    <Badge variant="secondary">정상</Badge>
                  )}
                </div>
                {user.lockedAt && (
                  <p className="mt-1 text-xs text-muted-foreground">
                    잠금 일시: {new Date(user.lockedAt).toLocaleString("ko-KR")}
                  </p>
                )}
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">가입 제공자</p>
                <p className="text-sm">{user.provider}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">가입일</p>
                <p className="text-sm text-muted-foreground">
                  {new Date(user.createdAt).toLocaleString("ko-KR")}
                </p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">최근 수정일</p>
                <p className="text-sm text-muted-foreground">
                  {new Date(user.updatedAt).toLocaleString("ko-KR")}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Card 3: 소셜 계정 연동 */}
        {identities.length > 0 && (
          <Card>
            <CardHeader className="border-b">
              <CardTitle>소셜 계정 연동 ({identities.length})</CardTitle>
            </CardHeader>
            <CardContent className="!p-0">
              {identitiesLoading ? (
                <div className="flex items-center justify-center py-8">
                  <div className="h-5 w-5 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/50">
                      <TableHead className="px-5">제공자</TableHead>
                      <TableHead className="px-5">소셜 ID</TableHead>
                      <TableHead className="px-5">연동 일시</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {identities.map((identity) => (
                      <TableRow key={identity.id}>
                        <TableCell className="px-5 font-medium">
                          {identity.providerName}
                          <span className="ml-2 text-xs text-muted-foreground font-mono">
                            ({identity.providerCode})
                          </span>
                        </TableCell>
                        <TableCell className="px-5 font-mono text-xs text-muted-foreground">
                          {identity.providerSubject}
                        </TableCell>
                        <TableCell className="px-5 text-muted-foreground text-sm">
                          {new Date(identity.linkedAt).toLocaleString("ko-KR")}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        )}

        {/* Card 4: 소속 그룹 */}
        <Card>
          <CardHeader className="border-b">
            <CardTitle>소속 그룹 ({groups.length})</CardTitle>
          </CardHeader>
          <CardContent className="!p-0">
            {groupsLoading ? (
              <div className="flex items-center justify-center py-8">
                <div className="h-5 w-5 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              </div>
            ) : groups.length === 0 ? (
              <div className="py-8 text-center text-sm text-muted-foreground">
                소속된 그룹이 없습니다.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/50">
                    <TableHead className="px-5">코드</TableHead>
                    <TableHead className="px-5">이름</TableHead>
                    <TableHead className="px-5">유형</TableHead>
                    <TableHead className="px-5">멤버 수</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {groups.map((group) => (
                    <TableRow key={group.id}>
                      <TableCell className="px-5 font-mono text-xs text-muted-foreground">
                        {group.groupCode}
                      </TableCell>
                      <TableCell className="px-5 font-medium">{group.groupName}</TableCell>
                      <TableCell className="px-5">
                        {group.isSystem ? (
                          <Badge className="bg-warning text-white">시스템</Badge>
                        ) : (
                          <Badge variant="secondary">사용자</Badge>
                        )}
                      </TableCell>
                      <TableCell className="px-5 text-muted-foreground">
                        {group.memberCount}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        {/* Card 5: 권한 요약 */}
        <Card>
          <CardHeader className="border-b">
            <CardTitle>권한 요약 ({permSummary.length})</CardTitle>
          </CardHeader>
          <CardContent className="!p-0">
            {permSummaryLoading ? (
              <div className="flex items-center justify-center py-8">
                <div className="h-5 w-5 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              </div>
            ) : permSummary.length === 0 ? (
              <div className="py-8 text-center text-sm text-muted-foreground">
                부여된 권한이 없습니다.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/50">
                    <TableHead className="px-5">모듈 인스턴스</TableHead>
                    <TableHead className="px-5">권한</TableHead>
                    <TableHead className="px-5">출처</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {permSummary.map((summary, idx) => (
                    <TableRow key={`${summary.instanceId}-${summary.source}-${idx}`}>
                      <TableCell className="px-5">
                        <span className="text-muted-foreground">{summary.moduleName}</span>
                        <span className="mx-1 text-muted-foreground">&gt;</span>
                        <span className="font-medium">{summary.instanceName}</span>
                      </TableCell>
                      <TableCell className="px-5">
                        <div className="flex flex-wrap gap-1">
                          {summary.permissions.map((perm, pidx) => (
                            <Badge key={pidx} variant="secondary" className="text-xs">
                              {perm.name}
                            </Badge>
                          ))}
                        </div>
                      </TableCell>
                      <TableCell className="px-5">
                        {summary.source === "DIRECT" ? (
                          <Badge className="bg-blue-100 text-blue-700 text-xs">직접</Badge>
                        ) : (
                          <Badge variant="secondary" className="text-xs">
                            {summary.source.replace("GROUP:", "")}
                          </Badge>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        {/* Card 6: 비밀번호 변경 */}
        <Card>
          <CardHeader className="border-b">
            <CardTitle>비밀번호 관리</CardTitle>
          </CardHeader>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground mb-4">
              관리자 권한으로 이 사용자의 비밀번호를 강제 변경할 수 있습니다.
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPasswordDialogOpen(true)}
            >
              <Key size={16} className="mr-1.5" />
              비밀번호 변경
            </Button>
          </CardContent>
        </Card>
      </div>

      {/* 비밀번호 변경 다이얼로그 */}
      <Dialog open={passwordDialogOpen} onOpenChange={setPasswordDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>비밀번호 변경</DialogTitle>
            <DialogDescription>
              {user.username} ({user.userId}) 사용자의 새 비밀번호를 입력하세요.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label htmlFor="newPassword">새 비밀번호</Label>
              <Input
                id="newPassword"
                type="password"
                placeholder="8자 이상 입력"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                minLength={8}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setPasswordDialogOpen(false);
                setNewPassword("");
              }}
            >
              취소
            </Button>
            <Button
              disabled={changingPassword || newPassword.length < 8}
              onClick={handleChangePassword}
            >
              {changingPassword ? "변경 중..." : "변경"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
