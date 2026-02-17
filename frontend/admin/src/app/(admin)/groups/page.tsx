"use client";

import { useEffect, useState, useCallback, type FormEvent } from "react";
import Link from "next/link";
import { apiGet, apiPost, apiDelete, type ApiResponse } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
import {
  Card,
  CardBody,
  Button,
  Badge,
  Modal,
  Input,
  FormGroup,
  Alert,
} from "@/components/ui";
import { Plus, Pencil, Trash } from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

/** 그룹 응답 */
interface Group {
  id: string;
  groupCode: string;
  groupName: string;
  description: string | null;
  isSystem: boolean;
  memberCount: number;
}

/** 그룹 목록 페이지 */
export default function GroupsPage() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /* 생성 모달 상태 */
  const [createOpen, setCreateOpen] = useState(false);
  const [createCode, setCreateCode] = useState("");
  const [createName, setCreateName] = useState("");
  const [createDesc, setCreateDesc] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  /* 삭제 확인 모달 상태 */
  const [deleteTarget, setDeleteTarget] = useState<Group | null>(null);
  const [deleting, setDeleting] = useState(false);

  /* 그룹 목록 로드 */
  const loadGroups = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<Group[]>("/groups");
      if (res.success && res.data) {
        setGroups(res.data);
      } else {
        setError(res.error?.message ?? "그룹 목록을 불러올 수 없습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadGroups();
  }, [loadGroups]);

  /* 그룹 생성 */
  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setCreateError(null);
    setCreating(true);

    try {
      const res: ApiResponse = await apiPost("/groups", {
        groupCode: createCode,
        name: createName,
        description: createDesc || null,
      });

      if (res.success) {
        setCreateOpen(false);
        setCreateCode("");
        setCreateName("");
        setCreateDesc("");
        await loadGroups();
      } else {
        setCreateError(res.error?.message ?? "그룹 생성에 실패했습니다.");
      }
    } catch {
      setCreateError("서버에 연결할 수 없습니다.");
    } finally {
      setCreating(false);
    }
  };

  /* 그룹 삭제 */
  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);

    try {
      const res = await apiDelete(`/groups/${deleteTarget.id}`);
      if (res.success) {
        setDeleteTarget(null);
        await loadGroups();
      }
    } catch {
      /* 삭제 실패 무시 */
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="그룹 관리"
        subtitle="시스템 그룹 및 사용자 그룹을 관리합니다"
        actions={
          <Button variant="primary" size="sm" onClick={() => setCreateOpen(true)}>
            <Plus size={16} className="mr-1.5" />
            그룹 생성
          </Button>
        }
      />

      <div className="p-5">
        {/* 에러 */}
        {error && (
          <Alert variant="danger" dismissible className="mb-4">
            {error}
          </Alert>
        )}

        {/* 그룹 테이블 */}
        <Card>
          <CardBody className="!p-0">
            {loading ? (
              <div className="flex items-center justify-center py-12">
                <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              </div>
            ) : groups.length === 0 ? (
              <div className="py-12 text-center text-sm text-gray-500">
                등록된 그룹이 없습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="px-5 py-3 text-left font-medium text-gray-600">코드</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">이름</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">유형</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">멤버 수</th>
                      <th className="px-5 py-3 text-right font-medium text-gray-600">작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {groups.map((group) => (
                      <tr key={group.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="px-5 py-3 font-mono text-xs text-gray-600">{group.groupCode}</td>
                        <td className="px-5 py-3">
                          <Link
                            href={`/groups/${group.id}`}
                            className="text-primary hover:underline font-medium"
                          >
                            {group.groupName}
                          </Link>
                          {group.description && (
                            <p className="mt-0.5 text-xs text-gray-400">{group.description}</p>
                          )}
                        </td>
                        <td className="px-5 py-3">
                          {group.isSystem ? (
                            <Badge variant="warning" pill>시스템</Badge>
                          ) : (
                            <Badge variant="light" pill>사용자</Badge>
                          )}
                        </td>
                        <td className="px-5 py-3 text-gray-600">{group.memberCount}</td>
                        <td className="px-5 py-3">
                          <div className="flex items-center justify-end gap-1">
                            <Link href={`/groups/${group.id}`}>
                              <Button variant="flat-primary" size="sm" iconOnly title="수정">
                                <Pencil size={14} />
                              </Button>
                            </Link>
                            {!group.isSystem && (
                              <Button
                                variant="flat-danger"
                                size="sm"
                                iconOnly
                                title="삭제"
                                onClick={() => setDeleteTarget(group)}
                              >
                                <Trash size={14} />
                              </Button>
                            )}
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

      {/* 그룹 생성 모달 */}
      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="그룹 생성"
        footer={
          <>
            <Button variant="light" onClick={() => setCreateOpen(false)}>취소</Button>
            <Button variant="primary" loading={creating} onClick={() => {
              /* 폼 submit 트리거 */
              document.getElementById("create-group-form")?.dispatchEvent(
                new Event("submit", { bubbles: true, cancelable: true })
              );
            }}>
              생성
            </Button>
          </>
        }
      >
        {createError && (
          <Alert variant="danger" dismissible className="mb-4">
            {createError}
          </Alert>
        )}
        <form id="create-group-form" onSubmit={handleCreate} className="space-y-4">
          <FormGroup label="그룹 코드" htmlFor="groupCode" required>
            <Input
              id="groupCode"
              type="text"
              placeholder="영문 소문자, 하이픈 (예: dev-team)"
              value={createCode}
              onChange={(e) => setCreateCode(e.target.value)}
              required
            />
          </FormGroup>
          <FormGroup label="그룹 이름" htmlFor="groupName" required>
            <Input
              id="groupName"
              type="text"
              placeholder="그룹 표시 이름"
              value={createName}
              onChange={(e) => setCreateName(e.target.value)}
              required
            />
          </FormGroup>
          <FormGroup label="설명" htmlFor="groupDesc">
            <Input
              id="groupDesc"
              type="text"
              placeholder="그룹 설명 (선택)"
              value={createDesc}
              onChange={(e) => setCreateDesc(e.target.value)}
            />
          </FormGroup>
        </form>
      </Modal>

      {/* 삭제 확인 모달 */}
      <Modal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="그룹 삭제"
        size="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setDeleteTarget(null)}>취소</Button>
            <Button variant="danger" loading={deleting} onClick={handleDelete}>삭제</Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          <span className="font-semibold">{deleteTarget?.groupName}</span> 그룹을 삭제하시겠습니까?
          이 작업은 되돌릴 수 없습니다.
        </p>
      </Modal>
    </>
  );
}
