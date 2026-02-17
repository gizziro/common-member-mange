"use client";

import { useEffect, useState, useCallback, type FormEvent } from "react";
import Link from "next/link";
import { apiGet, apiPost, apiDelete, apiPatch, apiPut, type ApiResponse } from "@/lib/api";
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
import { Plus, Pencil, Trash, Eye, EyeSlash, ShieldCheck } from "@phosphor-icons/react";
import { toast } from "sonner";

/* ===========================
 * 타입 정의
 * =========================== */

/** 페이지 목록 항목 */
interface PageItem {
  id: string;
  slug: string;
  title: string;
  contentType: string;
  isPublished: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

/** 권한 정의 (모듈에서 정의한 리소스-액션) */
interface PermissionDef {
  id: string;
  resource: string;
  action: string;
  name: string;
  flatCode: string;
}

/** 그룹별 권한 현황 */
interface GroupPermission {
  groupId: string;
  groupName: string;
  groupCode: string;
  grantedPermissionIds: string[];
}

/** 페이지 관리 목록 페이지 */
export default function PagesPage() {
  const [pages, setPages] = useState<PageItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /* 생성 모달 */
  const [createOpen, setCreateOpen] = useState(false);
  const [createSlug, setCreateSlug] = useState("");
  const [createTitle, setCreateTitle] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  /* 삭제 확인 */
  const [deleteTarget, setDeleteTarget] = useState<PageItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  /* 권한 설정 모달 */
  const [permOpen, setPermOpen] = useState(false);
  const [permLoading, setPermLoading] = useState(false);
  const [permSaving, setPermSaving] = useState(false);
  const [availablePerms, setAvailablePerms] = useState<PermissionDef[]>([]);
  const [groupPerms, setGroupPerms] = useState<GroupPermission[]>([]);

  /* 페이지 목록 로드 */
  const loadPages = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<PageItem[]>("/pages");
      if (res.success && res.data) {
        setPages(res.data);
      } else {
        setError(res.error?.message ?? "페이지 목록을 불러올 수 없습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPages();
  }, [loadPages]);

  /* 제목에서 slug 자동 생성 */
  const generateSlug = (title: string) => {
    return title
      .toLowerCase()
      .replace(/[^a-z0-9가-힣\s-]/g, "")
      .replace(/\s+/g, "-")
      .replace(/-+/g, "-")
      .replace(/^-|-$/g, "");
  };

  const handleTitleChange = (title: string) => {
    setCreateTitle(title);
    // 영문 입력 시에만 slug 자동 생성
    const slug = generateSlug(title);
    if (/^[a-z0-9-]+$/.test(slug) && slug.length >= 2) {
      setCreateSlug(slug);
    }
  };

  /* 페이지 생성 */
  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setCreateError(null);
    setCreating(true);

    try {
      const res: ApiResponse = await apiPost("/pages", {
        slug: createSlug,
        title: createTitle,
        content: "",
        contentType: "HTML",
      });

      if (res.success) {
        setCreateOpen(false);
        setCreateSlug("");
        setCreateTitle("");
        toast.success("페이지가 생성되었습니다.");
        await loadPages();
      } else {
        setCreateError(res.error?.message ?? "페이지 생성에 실패했습니다.");
      }
    } catch {
      setCreateError("서버에 연결할 수 없습니다.");
    } finally {
      setCreating(false);
    }
  };

  /* 삭제 */
  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      const res = await apiDelete(`/pages/${deleteTarget.id}`);
      if (res.success) {
        setDeleteTarget(null);
        toast.success("페이지가 삭제되었습니다.");
        await loadPages();
      }
    } catch {
      /* 삭제 실패 */
    } finally {
      setDeleting(false);
    }
  };

  /* 공개/비공개 토글 */
  const togglePublish = async (id: string) => {
    const res = await apiPatch(`/pages/${id}/publish`);
    if (res.success) {
      toast.success("공개 상태가 변경되었습니다.");
      await loadPages();
    }
  };

  /* ─── 권한 관리 ─── */

  /* 권한 모달 열기 — 권한 정의 + 그룹별 현황 동시 로드 */
  const openPermModal = async () => {
    setPermOpen(true);
    setPermLoading(true);

    try {
      // 권한 정의 + 그룹별 현황 병렬 조회
      const [defsRes, grpRes] = await Promise.all([
        apiGet<PermissionDef[]>("/pages/permissions"),
        apiGet<GroupPermission[]>("/pages/permissions/groups"),
      ]);

      if (defsRes.success && defsRes.data) {
        setAvailablePerms(defsRes.data);
      }
      if (grpRes.success && grpRes.data) {
        setGroupPerms(grpRes.data);
      }
    } catch {
      toast.error("권한 정보를 불러올 수 없습니다.");
    } finally {
      setPermLoading(false);
    }
  };

  /* 그룹의 권한 체크박스 토글 (로컬 상태만 변경) */
  const togglePermission = (groupId: string, permId: string) => {
    setGroupPerms((prev) =>
      prev.map((gp) => {
        if (gp.groupId !== groupId) return gp;

        // 이미 부여된 권한이면 제거, 없으면 추가
        const has = gp.grantedPermissionIds.includes(permId);
        return {
          ...gp,
          grantedPermissionIds: has
            ? gp.grantedPermissionIds.filter((id) => id !== permId)
            : [...gp.grantedPermissionIds, permId],
        };
      })
    );
  };

  /* 권한 저장 — 그룹별로 순차 저장 */
  const savePermissions = async () => {
    setPermSaving(true);
    try {
      // 모든 그룹의 권한을 순차 저장
      for (const gp of groupPerms) {
        await apiPut("/pages/permissions/groups", {
          groupId: gp.groupId,
          permissionIds: gp.grantedPermissionIds,
        });
      }
      toast.success("권한이 저장되었습니다.");
      setPermOpen(false);
    } catch {
      toast.error("권한 저장에 실패했습니다.");
    } finally {
      setPermSaving(false);
    }
  };

  /* 날짜 포맷 */
  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("ko-KR", {
      year: "numeric", month: "2-digit", day: "2-digit",
    });
  };

  return (
    <>
      <PageHeader
        title="페이지 관리"
        subtitle="텍스트/HTML 페이지를 관리합니다"
        actions={
          <div className="flex items-center gap-2">
            <Button variant="light" size="sm" onClick={openPermModal}>
              <ShieldCheck size={16} className="mr-1.5" />
              권한 설정
            </Button>
            <Button variant="primary" size="sm" onClick={() => setCreateOpen(true)}>
              <Plus size={16} className="mr-1.5" />
              페이지 생성
            </Button>
          </div>
        }
      />

      <div className="p-5">
        {error && (
          <Alert variant="danger" dismissible className="mb-4">{error}</Alert>
        )}

        <Card>
          <CardBody className="!p-0">
            {loading ? (
              <div className="flex items-center justify-center py-12">
                <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              </div>
            ) : pages.length === 0 ? (
              <div className="py-12 text-center text-sm text-gray-500">
                등록된 페이지가 없습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="px-5 py-3 text-left font-medium text-gray-600">제목</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">슬러그</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">유형</th>
                      <th className="px-5 py-3 text-center font-medium text-gray-600">공개</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">생성일</th>
                      <th className="px-5 py-3 text-right font-medium text-gray-600">작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pages.map((page) => (
                      <tr key={page.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="px-5 py-3">
                          <Link
                            href={`/pages/${page.id}`}
                            className="text-primary hover:underline font-medium"
                          >
                            {page.title}
                          </Link>
                        </td>
                        <td className="px-5 py-3 font-mono text-xs text-gray-500">
                          /page/{page.slug}
                        </td>
                        <td className="px-5 py-3">
                          <Badge variant="light" pill>{page.contentType}</Badge>
                        </td>
                        <td className="px-5 py-3 text-center">
                          <button
                            onClick={() => togglePublish(page.id)}
                            className={`p-1 rounded ${page.isPublished ? "text-green-600" : "text-gray-400"}`}
                            title={page.isPublished ? "공개" : "비공개"}
                          >
                            {page.isPublished ? <Eye size={16} /> : <EyeSlash size={16} />}
                          </button>
                        </td>
                        <td className="px-5 py-3 text-gray-500 text-xs">{formatDate(page.createdAt)}</td>
                        <td className="px-5 py-3">
                          <div className="flex items-center justify-end gap-1">
                            <Link href={`/pages/${page.id}`}>
                              <Button variant="flat-primary" size="sm" iconOnly title="수정">
                                <Pencil size={14} />
                              </Button>
                            </Link>
                            <Button
                              variant="flat-danger" size="sm" iconOnly title="삭제"
                              onClick={() => setDeleteTarget(page)}
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
      </div>

      {/* 생성 모달 */}
      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="페이지 생성"
        footer={
          <>
            <Button variant="light" onClick={() => setCreateOpen(false)}>취소</Button>
            <Button variant="primary" loading={creating} onClick={() => {
              document.getElementById("create-page-form")?.dispatchEvent(
                new Event("submit", { bubbles: true, cancelable: true })
              );
            }}>
              생성
            </Button>
          </>
        }
      >
        {createError && (
          <Alert variant="danger" dismissible className="mb-4">{createError}</Alert>
        )}
        <form id="create-page-form" onSubmit={handleCreate} className="space-y-4">
          <FormGroup label="제목" htmlFor="pageTitle" required>
            <Input
              id="pageTitle"
              type="text"
              placeholder="페이지 제목"
              value={createTitle}
              onChange={(e) => handleTitleChange(e.target.value)}
              required
            />
          </FormGroup>
          <FormGroup label="슬러그" htmlFor="pageSlug" required>
            <Input
              id="pageSlug"
              type="text"
              placeholder="영소문자-숫자-하이픈 (예: about-us)"
              value={createSlug}
              onChange={(e) => setCreateSlug(e.target.value)}
              required
            />
            <p className="mt-1 text-xs text-gray-400">URL: /page/{createSlug || "..."}</p>
          </FormGroup>
        </form>
      </Modal>

      {/* 삭제 확인 모달 */}
      <Modal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="페이지 삭제"
        size="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setDeleteTarget(null)}>취소</Button>
            <Button variant="danger" loading={deleting} onClick={handleDelete}>삭제</Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          <span className="font-semibold">{deleteTarget?.title}</span> 페이지를 삭제하시겠습니까?
          이 작업은 되돌릴 수 없습니다.
        </p>
      </Modal>

      {/* 권한 설정 모달 */}
      <Modal
        open={permOpen}
        onClose={() => setPermOpen(false)}
        title="페이지 모듈 권한 설정"
        size="lg"
        footer={
          <>
            <Button variant="light" onClick={() => setPermOpen(false)}>취소</Button>
            <Button variant="primary" loading={permSaving} onClick={savePermissions}>
              저장
            </Button>
          </>
        }
      >
        {permLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : availablePerms.length === 0 ? (
          <p className="py-8 text-center text-sm text-gray-500">
            정의된 권한이 없습니다.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="px-4 py-2.5 text-left font-medium text-gray-600">그룹</th>
                  {availablePerms.map((perm) => (
                    <th
                      key={perm.id}
                      className="px-3 py-2.5 text-center font-medium text-gray-600 whitespace-nowrap"
                      title={perm.flatCode}
                    >
                      {perm.name}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {groupPerms.map((gp) => (
                  <tr key={gp.groupId} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="px-4 py-2.5 font-medium text-gray-700 whitespace-nowrap">
                      {gp.groupName}
                      <span className="ml-1.5 text-xs text-gray-400">({gp.groupCode})</span>
                    </td>
                    {availablePerms.map((perm) => (
                      <td key={perm.id} className="px-3 py-2.5 text-center">
                        <input
                          type="checkbox"
                          checked={gp.grantedPermissionIds.includes(perm.id)}
                          onChange={() => togglePermission(gp.groupId, perm.id)}
                          className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                        />
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Modal>
    </>
  );
}
