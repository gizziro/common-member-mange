"use client";

import { useEffect, useState } from "react";
import { apiGet, apiPost, apiPut } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ArrowLeft } from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

/** 게시글 상세 응답 (수정 시 기존 데이터 로드용) */
interface PostDto {
  id: string;
  title: string;
  content: string | null;
  contentType: string;
  categoryId: string | null;
  isSecret: boolean;
  isDraft: boolean;
  tags: string[];
}

/** 컴포넌트 Props */
interface PostFormProps {
  /** 게시판 인스턴스 ID */
  boardId: string;
  /** 게시판 이름 */
  boardName: string;
  /** 수정 시 게시글 ID (없으면 새 글 작성) */
  postId?: string;
  /** 권한 목록 */
  permissions: Record<string, string[]>;
  /** 목록으로 돌아가기 콜백 */
  onBack: () => void;
  /** 저장 완료 후 이동 콜백 (게시글 ID 전달) */
  onSaved: (postId: string) => void;
}

/* ===========================
 * 게시글 작성/수정 폼 컴포넌트
 * =========================== */

export function PostForm({
  boardId,
  boardName,
  postId,
  permissions,
  onBack,
  onSaved,
}: PostFormProps) {
  const isEdit = !!postId;

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [contentType, setContentType] = useState("MARKDOWN");
  const [tagInput, setTagInput] = useState("");
  const [isSecret, setIsSecret] = useState(false);
  const [isDraft, setIsDraft] = useState(false);
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 파일 업로드
  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);

  // 인증 토큰
  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") ?? undefined : undefined;

  // 수정 모드 시 기존 게시글 로드
  useEffect(() => {
    if (!isEdit || !postId) return;
    const loadPost = async () => {
      setLoading(true);
      try {
        const res = await apiGet<PostDto>(
          `/boards/${boardId}/posts/${postId}`,
          token
        );
        if (res.success && res.data) {
          setTitle(res.data.title);
          setContent(res.data.content ?? "");
          setContentType(res.data.contentType);
          setIsSecret(res.data.isSecret);
          setIsDraft(res.data.isDraft);
          setTagInput((res.data.tags ?? []).join(", "));
        } else {
          setError(res.error?.message ?? "게시글을 불러올 수 없습니다.");
        }
      } catch {
        setError("서버에 연결할 수 없습니다.");
      } finally {
        setLoading(false);
      }
    };
    loadPost();
  }, [boardId, postId, isEdit, token]);

  // 저장 처리
  const handleSubmit = async () => {
    if (!title.trim()) {
      setError("제목을 입력하세요.");
      return;
    }
    setSaving(true);
    setError(null);

    // 태그 파싱 (쉼표 구분)
    const tagNames = tagInput
      .split(",")
      .map((t) => t.trim())
      .filter(Boolean);

    const body = {
      title,
      content,
      contentType,
      isSecret,
      isDraft,
      tagNames: tagNames.length > 0 ? tagNames : undefined,
    };

    try {
      let res;
      if (isEdit && postId) {
        // 수정
        res = await apiPut<PostDto>(`/boards/${boardId}/posts/${postId}`, body, token);
      } else {
        // 생성
        res = await apiPost<PostDto>(`/boards/${boardId}/posts`, body, token);
      }

      if (res.success && res.data) {
        const savedPostId = res.data.id;

        // 파일 업로드 (저장된 게시글에 첨부)
        if (files.length > 0) {
          setUploading(true);
          for (const file of files) {
            await uploadFile(boardId, savedPostId, file);
          }
          setUploading(false);
        }

        onSaved(savedPostId);
      } else {
        setError(res.error?.message ?? "저장에 실패했습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSaving(false);
    }
  };

  // 개별 파일 업로드 (multipart/form-data)
  const uploadFile = async (bId: string, pId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("postId", pId);

    const headers: Record<string, string> = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;

    try {
      await fetch(`/api/boards/${bId}/files`, {
        method: "POST",
        headers,
        body: formData,
      });
    } catch {
      // 파일 업로드 실패 — 무시 (게시글 자체는 저장됨)
    }
  };

  // 파일 선택 핸들러
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setFiles(Array.from(e.target.files));
    }
  };

  // 파일 제거
  const removeFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-8">
      {/* 뒤로가기 */}
      <button
        onClick={onBack}
        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-6 transition-colors"
      >
        <ArrowLeft size={16} />
        {boardName} 목록
      </button>

      <h1 className="text-2xl font-bold mb-6">
        {isEdit ? "게시글 수정" : "게시글 작성"}
      </h1>

      {error && (
        <div className="mb-4 rounded-lg border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">
          {error}
        </div>
      )}

      <div className="space-y-5">
        {/* 제목 */}
        <div className="space-y-1.5">
          <Label htmlFor="postTitle">제목 *</Label>
          <Input
            id="postTitle"
            placeholder="게시글 제목을 입력하세요"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        </div>

        {/* 콘텐츠 유형 */}
        <div className="space-y-1.5">
          <Label>콘텐츠 유형</Label>
          <div className="flex gap-2">
            {(["MARKDOWN", "HTML", "TEXT"] as const).map((t) => (
              <Button
                key={t}
                variant={contentType === t ? "default" : "outline"}
                size="sm"
                onClick={() => setContentType(t)}
              >
                {t === "MARKDOWN" ? "Markdown" : t === "HTML" ? "HTML" : "텍스트"}
              </Button>
            ))}
          </div>
        </div>

        {/* 본문 */}
        <div className="space-y-1.5">
          <Label htmlFor="postContent">내용</Label>
          <textarea
            id="postContent"
            className="w-full min-h-[300px] rounded-lg border p-3 font-mono text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary resize-y"
            placeholder={
              contentType === "MARKDOWN"
                ? "# 제목\n\n내용을 입력하세요..."
                : contentType === "HTML"
                ? "<p>내용을 입력하세요...</p>"
                : "내용을 입력하세요..."
            }
            value={content}
            onChange={(e) => setContent(e.target.value)}
          />
        </div>

        {/* 태그 */}
        <div className="space-y-1.5">
          <Label htmlFor="postTags">태그 (쉼표로 구분)</Label>
          <Input
            id="postTags"
            placeholder="태그1, 태그2, 태그3"
            value={tagInput}
            onChange={(e) => setTagInput(e.target.value)}
          />
        </div>

        {/* 옵션 토글 */}
        <div className="flex items-center gap-6">
          {permissions?.post?.includes("write") && (
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={isSecret}
                onChange={(e) => setIsSecret(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300"
              />
              비밀글
            </label>
          )}
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              checked={isDraft}
              onChange={(e) => setIsDraft(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300"
            />
            임시저장
          </label>
        </div>

        {/* 파일 업로드 */}
        {!isEdit && (
          <div className="space-y-2">
            <Label>첨부파일</Label>
            <input
              type="file"
              multiple
              onChange={handleFileChange}
              className="block w-full text-sm text-muted-foreground file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90"
            />
            {files.length > 0 && (
              <div className="space-y-1 mt-2">
                {files.map((file, i) => (
                  <div key={i} className="flex items-center gap-2 text-sm">
                    <span className="truncate">{file.name}</span>
                    <span className="text-muted-foreground shrink-0">
                      ({(file.size / 1024).toFixed(1)} KB)
                    </span>
                    <button
                      onClick={() => removeFile(i)}
                      className="text-destructive hover:underline text-xs shrink-0"
                    >
                      제거
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 버튼 */}
        <div className="flex items-center justify-end gap-2 pt-4">
          <Button variant="outline" onClick={onBack}>취소</Button>
          <Button disabled={saving || uploading} onClick={handleSubmit}>
            {saving ? "저장 중..." : uploading ? "파일 업로드 중..." : isEdit ? "수정" : "등록"}
          </Button>
        </div>
      </div>
    </div>
  );
}
