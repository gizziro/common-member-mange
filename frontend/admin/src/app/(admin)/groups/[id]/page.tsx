"use client";

import { useEffect, useState, useCallback, type FormEvent } from "react";
import { useParams, useRouter } from "next/navigation";
import { apiGet, apiPut, apiPost, apiDelete, type ApiResponse } from "@/lib/api";
import { Badge as ShadcnBadge } from "@/components/ui/Badge";
import { PageHeader } from "@/components/layout/PageHeader";
import {
  Card,
  CardHeader,
  CardBody,
  Button,
  Badge,
  Modal,
  Input,
  FormGroup,
  Alert,
} from "@/components/ui";
import { ArrowLeft, Plus, Trash, FloppyDisk } from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

interface Group {
  id: string;
  groupCode: string;
  groupName: string;
  description: string | null;
  isSystem: boolean;
  memberCount: number;
}

interface GroupMember {
  id: string;
  userId: string;
  username: string;
  email: string;
  joinedAt: string;
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

/** 그룹 상세 + 멤버 관리 페이지 */
export default function GroupDetailPage() {
  const params = useParams();
  const router = useRouter();
  const groupId = params.id as string;

  /* 그룹 정보 상태 */
  const [group, setGroup] = useState<Group | null>(null);
  const [editName, setEditName] = useState("");
  const [editDesc, setEditDesc] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  /* 멤버 목록 상태 */
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [membersLoading, setMembersLoading] = useState(true);

  /* 멤버 추가 모달 */
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [addMemberUserId, setAddMemberUserId] = useState("");
  const [addMemberError, setAddMemberError] = useState<string | null>(null);
  const [addingMember, setAddingMember] = useState(false);

  /* 멤버 삭제 확인 */
  const [removeMemberTarget, setRemoveMemberTarget] = useState<GroupMember | null>(null);
  const [removingMember, setRemovingMember] = useState(false);

  /* 권한 요약 상태 */
  const [permSummary, setPermSummary] = useState<PermissionSummary[]>([]);
  const [permSummaryLoading, setPermSummaryLoading] = useState(true);

  /* 그룹 정보 로드 */
  const loadGroup = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<Group>(`/groups/${groupId}`);
      if (res.success && res.data) {
        setGroup(res.data);
        setEditName(res.data.groupName);
        setEditDesc(res.data.description ?? "");
      } else {
        setError(res.error?.message ?? "그룹 정보를 불러올 수 없습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, [groupId]);

  /* 멤버 목록 로드 */
  const loadMembers = useCallback(async () => {
    setMembersLoading(true);
    try {
      const res = await apiGet<GroupMember[]>(`/groups/${groupId}/members`);
      if (res.success && res.data) {
        setMembers(res.data);
      }
    } catch {
      /* 멤버 로드 실패 무시 */
    } finally {
      setMembersLoading(false);
    }
  }, [groupId]);

  /* 권한 요약 로드 */
  const loadPermSummary = useCallback(async () => {
    setPermSummaryLoading(true);
    try {
      const res = await apiGet<PermissionSummary[]>(`/groups/${groupId}/permissions`);
      if (res.success && res.data) {
        setPermSummary(res.data);
      }
    } catch {
      /* 권한 요약 조회 실패 시 빈 배열 유지 */
    } finally {
      setPermSummaryLoading(false);
    }
  }, [groupId]);

  useEffect(() => {
    loadGroup();
    loadMembers();
    loadPermSummary();
  }, [loadGroup, loadMembers, loadPermSummary]);

  /* 그룹 정보 수정 */
  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!group) return;

    setSaving(true);
    setSaveSuccess(false);
    setError(null);

    try {
      const res: ApiResponse = await apiPut(`/groups/${groupId}`, {
        groupName: editName,
        description: editDesc || null,
      });

      if (res.success) {
        setSaveSuccess(true);
        await loadGroup();
        setTimeout(() => setSaveSuccess(false), 3000);
      } else {
        setError(res.error?.message ?? "수정에 실패했습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSaving(false);
    }
  };

  /* 멤버 추가 */
  const handleAddMember = async (e: FormEvent) => {
    e.preventDefault();
    setAddMemberError(null);
    setAddingMember(true);

    try {
      const res: ApiResponse = await apiPost(`/groups/${groupId}/members`, {
        userId: addMemberUserId,
      });

      if (res.success) {
        setAddMemberOpen(false);
        setAddMemberUserId("");
        await loadMembers();
        await loadGroup();
      } else {
        setAddMemberError(res.error?.message ?? "멤버 추가에 실패했습니다.");
      }
    } catch {
      setAddMemberError("서버에 연결할 수 없습니다.");
    } finally {
      setAddingMember(false);
    }
  };

  /* 멤버 제거 */
  const handleRemoveMember = async () => {
    if (!removeMemberTarget) return;
    setRemovingMember(true);

    try {
      const res = await apiDelete(`/groups/${groupId}/members/${removeMemberTarget.userId}`);
      if (res.success) {
        setRemoveMemberTarget(null);
        await loadMembers();
        await loadGroup();
      }
    } catch {
      /* 삭제 실패 무시 */
    } finally {
      setRemovingMember(false);
    }
  };

  /* 로딩 */
  if (loading) {
    return (
      <>
        <PageHeader title="그룹 상세" />
        <div className="flex items-center justify-center py-24">
          <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      </>
    );
  }

  if (!group) {
    return (
      <>
        <PageHeader title="그룹 상세" />
        <div className="p-5">
          <Alert variant="danger">그룹을 찾을 수 없습니다.</Alert>
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title={group.groupName}
        subtitle={`코드: ${group.groupCode}`}
        actions={
          <Button variant="flat-dark" size="sm" onClick={() => router.push("/groups")}>
            <ArrowLeft size={16} className="mr-1.5" />
            목록으로
          </Button>
        }
      />

      <div className="p-5 space-y-5">
        {/* 에러/성공 메시지 */}
        {error && <Alert variant="danger" dismissible>{error}</Alert>}
        {saveSuccess && <Alert variant="success" dismissible>저장되었습니다.</Alert>}

        {/* 그룹 정보 수정 카드 */}
        <Card>
          <CardHeader title="그룹 정보" />
          <CardBody>
            <form onSubmit={handleSave} className="space-y-4 max-w-lg">
              <FormGroup label="그룹 코드">
                <Input
                  type="text"
                  value={group.groupCode}
                  disabled
                />
              </FormGroup>
              <FormGroup label="그룹 이름" htmlFor="editName" required>
                <Input
                  id="editName"
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  disabled={group.isSystem}
                  required
                />
              </FormGroup>
              <FormGroup label="설명" htmlFor="editDesc">
                <Input
                  id="editDesc"
                  type="text"
                  value={editDesc}
                  onChange={(e) => setEditDesc(e.target.value)}
                  disabled={group.isSystem}
                />
              </FormGroup>
              <div className="flex items-center gap-3">
                {group.isSystem ? (
                  <Badge variant="warning">시스템 그룹은 수정할 수 없습니다</Badge>
                ) : (
                  <Button type="submit" variant="primary" size="sm" loading={saving}>
                    <FloppyDisk size={16} className="mr-1.5" />
                    저장
                  </Button>
                )}
              </div>
            </form>
          </CardBody>
        </Card>

        {/* 멤버 관리 카드 */}
        <Card>
          <CardHeader title={`멤버 (${members.length})`}>
            <Button variant="outline-primary" size="sm" onClick={() => setAddMemberOpen(true)}>
              <Plus size={14} className="mr-1" />
              멤버 추가
            </Button>
          </CardHeader>
          <CardBody className="!p-0">
            {membersLoading ? (
              <div className="flex items-center justify-center py-8">
                <div className="h-5 w-5 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              </div>
            ) : members.length === 0 ? (
              <div className="py-8 text-center text-sm text-gray-500">
                등록된 멤버가 없습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="px-5 py-3 text-left font-medium text-gray-600">아이디</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">이름</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">이메일</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">가입일</th>
                      <th className="px-5 py-3 text-right font-medium text-gray-600">작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {members.map((member) => (
                      <tr key={member.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="px-5 py-3 font-mono text-xs text-gray-600">{member.userId}</td>
                        <td className="px-5 py-3">{member.username}</td>
                        <td className="px-5 py-3 text-gray-500">{member.email}</td>
                        <td className="px-5 py-3 text-gray-500 text-xs">
                          {member.joinedAt ? new Date(member.joinedAt).toLocaleDateString("ko-KR") : "-"}
                        </td>
                        <td className="px-5 py-3">
                          <div className="flex justify-end">
                            <Button
                              variant="flat-danger"
                              size="sm"
                              iconOnly
                              title="멤버 제거"
                              onClick={() => setRemoveMemberTarget(member)}
                            >
                              <Trash size={14} />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardBody>
        </Card>
        {/* 권한 요약 카드 */}
        <Card>
          <CardHeader title={`권한 요약 (${permSummary.length})`} />
          <CardBody className="!p-0">
            {permSummaryLoading ? (
              <div className="flex items-center justify-center py-8">
                <div className="h-5 w-5 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              </div>
            ) : permSummary.length === 0 ? (
              <div className="py-8 text-center text-sm text-gray-500">
                부여된 권한이 없습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="px-5 py-3 text-left font-medium text-gray-600">모듈 인스턴스</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">권한</th>
                    </tr>
                  </thead>
                  <tbody>
                    {permSummary.map((summary, idx) => (
                      <tr key={`${summary.instanceId}-${idx}`} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="px-5 py-3">
                          <span className="text-gray-400">{summary.moduleName}</span>
                          <span className="mx-1 text-gray-400">&gt;</span>
                          <span className="font-medium">{summary.instanceName}</span>
                        </td>
                        <td className="px-5 py-3">
                          <div className="flex flex-wrap gap-1">
                            {summary.permissions.map((perm, pidx) => (
                              <ShadcnBadge key={pidx} variant="secondary" className="text-xs">
                                {perm.name}
                              </ShadcnBadge>
                            ))}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardBody>
        </Card>
      </div>

      {/* 멤버 추가 모달 */}
      <Modal
        open={addMemberOpen}
        onClose={() => setAddMemberOpen(false)}
        title="멤버 추가"
        size="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setAddMemberOpen(false)}>취소</Button>
            <Button variant="primary" loading={addingMember} onClick={() => {
              document.getElementById("add-member-form")?.dispatchEvent(
                new Event("submit", { bubbles: true, cancelable: true })
              );
            }}>
              추가
            </Button>
          </>
        }
      >
        {addMemberError && (
          <Alert variant="danger" dismissible className="mb-4">
            {addMemberError}
          </Alert>
        )}
        <form id="add-member-form" onSubmit={handleAddMember} className="space-y-4">
          <FormGroup label="사용자 ID" htmlFor="memberUserId" required>
            <Input
              id="memberUserId"
              type="text"
              placeholder="추가할 사용자의 로그인 ID"
              value={addMemberUserId}
              onChange={(e) => setAddMemberUserId(e.target.value)}
              required
            />
          </FormGroup>
        </form>
      </Modal>

      {/* 멤버 제거 확인 모달 */}
      <Modal
        open={!!removeMemberTarget}
        onClose={() => setRemoveMemberTarget(null)}
        title="멤버 제거"
        size="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setRemoveMemberTarget(null)}>취소</Button>
            <Button variant="danger" loading={removingMember} onClick={handleRemoveMember}>제거</Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          <span className="font-semibold">{removeMemberTarget?.username}</span> 멤버를 이 그룹에서 제거하시겠습니까?
        </p>
      </Modal>
    </>
  );
}
