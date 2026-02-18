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
import { toast } from "sonner";

/* ===========================
 * 타입 정의
 * =========================== */

/** 게시판 목록 항목 */
interface BoardItem {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  ownerId: string;
  enabled: boolean;
  postCount: number;
  createdAt: string;
  updatedAt: string;
}

/** 게시판 관리 목록 페이지 */
export default function BoardsPage() {
  const [boards, setBoards] = useState<BoardItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /* 생성 모달 */
  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState("");
  const [createSlug, setCreateSlug] = useState("");
  const [createDesc, setCreateDesc] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  /* 삭제 확인 */
  const [deleteTarget, setDeleteTarget] = useState<BoardItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  /* 게시판 목록 로드 */
  const loadBoards = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<BoardItem[]>("/boards");
      if (res.success && res.data) {
        setBoards(res.data);
      } else {
        setError(res.error?.message ?? "게시판 목록을 불러올 수 없습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBoards();
  }, [loadBoards]);

  /* 이름에서 slug 자동 생성 */
  const handleNameChange = (name: string) => {
    setCreateName(name);
    // 영문 입력 시에만 slug 자동 생성
    const slug = name
      .toLowerCase()
      .replace(/[^a-z0-9가-힣\s-]/g, "")
      .replace(/\s+/g, "-")
      .replace(/-+/g, "-")
      .replace(/^-|-$/g, "");
    if (/^[a-z0-9-]+$/.test(slug) && slug.length >= 2) {
      setCreateSlug(slug);
    }
  };

  /* 게시판 생성 */
  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setCreateError(null);
    setCreating(true);

    try {
      const res: ApiResponse = await apiPost("/boards", {
        name: createName,
        slug: createSlug,
        description: createDesc || undefined,
      });

      if (res.success) {
        setCreateOpen(false);
        setCreateName("");
        setCreateSlug("");
        setCreateDesc("");
        toast.success("게시판이 생성되었습니다.");
        await loadBoards();
      } else {
        setCreateError(res.error?.message ?? "게시판 생성에 실패했습니다.");
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
      const res = await apiDelete(`/boards/${deleteTarget.id}`);
      if (res.success) {
        setDeleteTarget(null);
        toast.success("게시판이 삭제되었습니다.");
        await loadBoards();
      } else {
        toast.error(res.error?.message ?? "삭제에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setDeleting(false);
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
        title="게시판 관리"
        subtitle="게시판 인스턴스를 관리합니다"
        actions={
          <Button variant="primary" size="sm" onClick={() => setCreateOpen(true)}>
            <Plus size={16} className="mr-1.5" />
            게시판 생성
          </Button>
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
            ) : boards.length === 0 ? (
              <div className="py-12 text-center text-sm text-gray-500">
                등록된 게시판이 없습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="px-5 py-3 text-left font-medium text-gray-600">이름</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">슬러그</th>
                      <th className="px-5 py-3 text-center font-medium text-gray-600">게시글 수</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">생성일</th>
                      <th className="px-5 py-3 text-right font-medium text-gray-600">작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {boards.map((board) => (
                      <tr key={board.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="px-5 py-3">
                          <Link
                            href={`/boards/${board.id}`}
                            className="text-primary hover:underline font-medium"
                          >
                            {board.name}
                          </Link>
                          {board.description && (
                            <p className="mt-0.5 text-xs text-gray-400 truncate max-w-xs">
                              {board.description}
                            </p>
                          )}
                        </td>
                        <td className="px-5 py-3 font-mono text-xs text-gray-500">
                          /board/{board.slug}
                        </td>
                        <td className="px-5 py-3 text-center">
                          <Badge variant="light" pill>{board.postCount ?? 0}</Badge>
                        </td>
                        <td className="px-5 py-3 text-gray-500 text-xs">{formatDate(board.createdAt)}</td>
                        <td className="px-5 py-3">
                          <div className="flex items-center justify-end gap-1">
                            <Link href={`/boards/${board.id}`}>
                              <Button variant="flat-primary" size="sm" iconOnly title="수정">
                                <Pencil size={14} />
                              </Button>
                            </Link>
                            <Button
                              variant="flat-danger" size="sm" iconOnly title="삭제"
                              onClick={() => setDeleteTarget(board)}
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
        title="게시판 생성"
        footer={
          <>
            <Button variant="light" onClick={() => setCreateOpen(false)}>취소</Button>
            <Button variant="primary" loading={creating} onClick={() => {
              document.getElementById("create-board-form")?.dispatchEvent(
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
        <form id="create-board-form" onSubmit={handleCreate} className="space-y-4">
          <FormGroup label="이름" htmlFor="boardName" required>
            <Input
              id="boardName"
              type="text"
              placeholder="게시판 이름"
              value={createName}
              onChange={(e) => handleNameChange(e.target.value)}
              required
            />
          </FormGroup>
          <FormGroup label="슬러그" htmlFor="boardSlug" required>
            <Input
              id="boardSlug"
              type="text"
              placeholder="영소문자-숫자-하이픈 (예: notice)"
              value={createSlug}
              onChange={(e) => setCreateSlug(e.target.value)}
              required
            />
            <p className="mt-1 text-xs text-gray-400">URL: /board/{createSlug || "..."}</p>
          </FormGroup>
          <FormGroup label="설명" htmlFor="boardDesc">
            <Input
              id="boardDesc"
              type="text"
              placeholder="게시판 설명 (선택)"
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
        title="게시판 삭제"
        size="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setDeleteTarget(null)}>취소</Button>
            <Button variant="danger" loading={deleting} onClick={handleDelete}>삭제</Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          <span className="font-semibold">{deleteTarget?.name}</span> 게시판을 삭제하시겠습니까?
          모든 게시글, 댓글, 파일이 함께 삭제되며 이 작업은 되돌릴 수 없습니다.
        </p>
      </Modal>
    </>
  );
}
