"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useParams, useRouter } from "next/navigation";
import { apiGet, apiPut, type ApiResponse } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
import {
  Card,
  CardBody,
  Button,
  Input,
  FormGroup,
  Alert,
} from "@/components/ui";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { ArrowLeft, FloppyDisk } from "@phosphor-icons/react";
import { toast } from "sonner";
import Link from "next/link";

/* ===========================
 * 타입 정의
 * =========================== */

/** 페이지 상세 응답 */
interface PageDetail {
  id: string;
  slug: string;
  title: string;
  content: string | null;
  contentType: string;
  isPublished: boolean;
  sortOrder: number;
  createdBy: string;
  createdAt: string;
  updatedBy: string | null;
  updatedAt: string;
}

/** 페이지 편집 페이지 */
export default function PageEditPage() {
  const params = useParams();
  const router = useRouter();
  const pageId = params.id as string;

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  /* 폼 상태 */
  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [content, setContent] = useState("");
  const [contentType, setContentType] = useState("HTML");

  /* 페이지 데이터 로드 */
  useEffect(() => {
    const loadPage = async () => {
      setLoading(true);
      try {
        const res = await apiGet<PageDetail>(`/pages/${pageId}`);
        if (res.success && res.data) {
          setTitle(res.data.title);
          setSlug(res.data.slug);
          setContent(res.data.content ?? "");
          setContentType(res.data.contentType);
        } else {
          setError(res.error?.message ?? "페이지를 불러올 수 없습니다.");
        }
      } catch {
        setError("서버에 연결할 수 없습니다.");
      } finally {
        setLoading(false);
      }
    };

    loadPage();
  }, [pageId]);

  /* 저장 */
  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaving(true);

    try {
      const res: ApiResponse = await apiPut(`/pages/${pageId}`, {
        title,
        slug,
        content,
        contentType,
      });

      if (res.success) {
        toast.success("페이지가 저장되었습니다.");
      } else {
        setError(res.error?.message ?? "저장에 실패했습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <>
        <PageHeader title="페이지 편집" subtitle="로딩 중..." />
        <div className="flex items-center justify-center py-24">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="페이지 편집"
        subtitle={`${title} (/${slug})`}
        actions={
          <div className="flex gap-2">
            <Link href="/pages">
              <Button variant="light" size="sm">
                <ArrowLeft size={16} className="mr-1.5" />
                목록
              </Button>
            </Link>
            <Button variant="primary" size="sm" loading={saving} onClick={() => {
              document.getElementById("edit-page-form")?.dispatchEvent(
                new Event("submit", { bubbles: true, cancelable: true })
              );
            }}>
              <FloppyDisk size={16} className="mr-1.5" />
              저장
            </Button>
          </div>
        }
      />

      <div className="p-5">
        {error && (
          <Alert variant="danger" dismissible className="mb-4">{error}</Alert>
        )}

        <form id="edit-page-form" onSubmit={handleSave} className="space-y-5">
          {/* 기본 정보 카드 */}
          <Card>
            <CardBody>
              <h3 className="text-sm font-semibold text-gray-700 mb-4">기본 정보</h3>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <FormGroup label="제목" htmlFor="editTitle" required>
                  <Input
                    id="editTitle"
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                  />
                </FormGroup>
                <FormGroup label="슬러그" htmlFor="editSlug" required>
                  <Input
                    id="editSlug"
                    type="text"
                    value={slug}
                    onChange={(e) => setSlug(e.target.value)}
                    required
                  />
                  <p className="mt-1 text-xs text-gray-400">URL: /page/{slug}</p>
                </FormGroup>
              </div>
              <div className="mt-4 max-w-xs">
                <div className="space-y-1.5">
                  <Label>콘텐츠 유형</Label>
                  <Select value={contentType} onValueChange={setContentType}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="HTML">HTML</SelectItem>
                      <SelectItem value="MARKDOWN">Markdown</SelectItem>
                      <SelectItem value="TEXT">텍스트</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </CardBody>
          </Card>

          {/* 콘텐츠 에디터 카드 */}
          <Card>
            <CardBody>
              <h3 className="text-sm font-semibold text-gray-700 mb-4">콘텐츠</h3>
              <textarea
                className="w-full min-h-[400px] rounded-md border border-gray-300 p-3 font-mono text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder={
                  contentType === "HTML"
                    ? "<h1>페이지 제목</h1>\n<p>페이지 내용을 입력하세요.</p>"
                    : contentType === "MARKDOWN"
                    ? "# 페이지 제목\n\n페이지 내용을 입력하세요."
                    : "페이지 내용을 입력하세요."
                }
              />
            </CardBody>
          </Card>
        </form>
      </div>
    </>
  );
}
