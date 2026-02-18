"use client";

import { useEffect, useState, useCallback } from "react";
import DOMPurify from "dompurify";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { apiGet, apiPost, apiDelete } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { CommentSection } from "./CommentSection";
import {
  ThumbsUp,
  ThumbsDown,
  PencilSimple,
  Trash,
  ArrowLeft,
  DownloadSimple,
  Paperclip,
  Tag,
} from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

/** 게시글 상세 응답 DTO */
interface PostDto {
  id: string;
  boardInstanceId: string;
  categoryId: string | null;
  categoryName: string | null;
  parentId: string | null;
  depth: number;
  title: string;
  content: string | null;
  contentType: string;
  slug: string | null;
  isSecret: boolean;
  isNotice: boolean;
  noticeScope: string | null;
  isDraft: boolean;
  authorId: string;
  authorName: string;
  viewCount: number;
  voteUpCount: number;
  voteDownCount: number;
  commentCount: number;
  metaTitle: string | null;
  metaDescription: string | null;
  tags: string[];
  files: FileDto[];
  createdAt: string;
  updatedAt: string;
}

/** 첨부 파일 DTO */
interface FileDto {
  id: string;
  postId: string;
  originalName: string;
  storedName: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
  isImage: boolean;
  thumbnailPath: string | null;
  sortOrder: number;
  downloadCount: number;
  createdAt: string;
}

/** 투표 응답 DTO */
interface VoteResponse {
  voteUpCount: number;
  voteDownCount: number;
  userVoteType: string | null;
}

/** 게시판 설정 */
interface BoardSettings {
  editorType: string;
  postsPerPage: number;
  displayFormat: string;
  paginationType: string;
  allowAnonymousAccess: boolean;
  allowFileUpload: boolean;
  allowedFileTypes: string;
  maxFileSize: number;
  maxFilesPerPost: number;
  maxReplyDepth: number;
  maxCommentDepth: number;
  allowSecretPosts: boolean;
  allowDraft: boolean;
  allowTags: boolean;
  allowVote: boolean;
  useCategory: boolean;
}

/** 컴포넌트 Props */
interface PostDetailProps {
  /** 게시판 인스턴스 ID */
  boardId: string;
  /** 게시글 ID */
  postId: string;
  /** 게시판 이름 */
  boardName: string;
  /** 모듈 slug */
  moduleSlug: string;
  /** 게시판 slug */
  boardSlug: string;
  /** 권한 목록 */
  permissions: Record<string, string[]>;
  /** 게시판 설정 (null이면 모든 기능 표시) */
  settings: BoardSettings | null;
  /** 목록으로 돌아가기 콜백 */
  onBack: () => void;
  /** 수정 페이지로 이동 콜백 */
  onEdit: (postId: string) => void;
}

/* ===========================
 * 게시글 상세 컴포넌트
 * =========================== */

export function PostDetail({
  boardId,
  postId,
  boardName,
  moduleSlug,
  boardSlug,
  permissions,
  settings,
  onBack,
  onEdit,
}: PostDetailProps) {
  const [post, setPost] = useState<PostDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [voteUp, setVoteUp] = useState(0);
  const [voteDown, setVoteDown] = useState(0);
  const [userVoteType, setUserVoteType] = useState<string | null>(null);

  // 설정 기반 기능 토글
  const showVote = settings?.allowVote ?? true;
  const showTags = settings?.allowTags ?? true;

  // 인증 토큰 + 사용자 ID
  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") ?? undefined : undefined;
  const currentUserId = typeof window !== "undefined" ? localStorage.getItem("userPk") ?? null : null;

  // 게시글 상세 로드
  const loadPost = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiGet<PostDto>(
        `/boards/${boardId}/posts/${postId}`,
        token
      );
      if (res.success && res.data) {
        setPost(res.data);
        setVoteUp(res.data.voteUpCount);
        setVoteDown(res.data.voteDownCount);
      } else {
        setError(res.error?.message ?? "게시글을 불러올 수 없습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, [boardId, postId, token]);

  useEffect(() => {
    loadPost();
  }, [loadPost]);

  // 투표 처리
  const handleVote = async (voteType: "UP" | "DOWN") => {
    if (!token) return;
    try {
      const res = await apiPost<VoteResponse>(
        `/boards/${boardId}/posts/${postId}/vote`,
        { voteType },
        token
      );
      if (res.success && res.data) {
        setVoteUp(res.data.voteUpCount);
        setVoteDown(res.data.voteDownCount);
        setUserVoteType(res.data.userVoteType);
      }
    } catch {
      // 투표 실패
    }
  };

  // 게시글 삭제
  const handleDelete = async () => {
    if (!confirm("게시글을 삭제하시겠습니까?")) return;
    try {
      const res = await apiDelete(
        `/boards/${boardId}/posts/${postId}`,
        token
      );
      if (res.success) {
        onBack();
      }
    } catch {
      // 삭제 실패
    }
  };

  // 날짜 포맷
  const formatDate = (d: string) =>
    new Date(d).toLocaleString("ko-KR", {
      year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit",
    });

  // 파일 크기 포맷
  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  };

  // 수정/삭제 권한
  const canModify = (permissions?.post?.includes("modify") && currentUserId === post?.authorId) ?? false;
  const canDelete = permissions?.post?.includes("delete") || currentUserId === post?.authorId;

  // 로딩
  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  // 에러
  if (error || !post) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-24 text-center">
        <p className="text-muted-foreground">{error ?? "게시글을 찾을 수 없습니다."}</p>
        <Button variant="ghost" className="mt-4" onClick={onBack}>
          <ArrowLeft size={16} />
          목록으로
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-8">
      {/* 상단 네비게이션 */}
      <button
        onClick={onBack}
        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-6 transition-colors"
      >
        <ArrowLeft size={16} />
        {boardName} 목록
      </button>

      {/* 게시글 헤더 */}
      <div className="space-y-3">
        {/* 배지 */}
        <div className="flex items-center gap-2">
          {post.isNotice && <Badge>공지</Badge>}
          {post.isSecret && <Badge variant="secondary">비밀</Badge>}
          {post.isDraft && <Badge variant="outline">임시저장</Badge>}
          {post.categoryName && <Badge variant="outline">{post.categoryName}</Badge>}
        </div>

        {/* 제목 */}
        <h1 className="text-2xl font-bold">{post.title}</h1>

        {/* 메타 정보 */}
        <div className="flex items-center gap-4 text-sm text-muted-foreground">
          <span>{post.authorName}</span>
          <span>{formatDate(post.createdAt)}</span>
          <span>조회 {post.viewCount}</span>
        </div>

        {/* 태그 (allowTags 설정 시에만) */}
        {showTags && post.tags && post.tags.length > 0 && (
          <div className="flex items-center gap-1.5 flex-wrap">
            <Tag size={14} className="text-muted-foreground" />
            {post.tags.map((tag) => (
              <Badge key={tag} variant="secondary">{tag}</Badge>
            ))}
          </div>
        )}
      </div>

      {/* 구분선 */}
      <hr className="my-6" />

      {/* 콘텐츠 */}
      <div className="prose prose-gray max-w-none">
        {renderContent(post.content, post.contentType)}
      </div>

      {/* 첨부파일 */}
      {post.files && post.files.length > 0 && (
        <div className="mt-8 rounded-lg border p-4">
          <div className="flex items-center gap-2 mb-3">
            <Paperclip size={16} className="text-muted-foreground" />
            <span className="text-sm font-medium">첨부파일 ({post.files.length})</span>
          </div>
          <div className="space-y-2">
            {post.files.map((file) => (
              <a
                key={file.id}
                href={`/api/boards/files/${file.id}/download`}
                className="flex items-center gap-2 rounded-md border p-2 text-sm hover:bg-muted transition-colors"
              >
                <DownloadSimple size={16} className="text-muted-foreground shrink-0" />
                <span className="truncate">{file.originalName}</span>
                <span className="ml-auto text-xs text-muted-foreground shrink-0">{formatSize(file.fileSize)}</span>
              </a>
            ))}
          </div>
        </div>
      )}

      {/* 투표 버튼 (allowVote 설정 시에만) */}
      {showVote && (
        <div className="mt-8 flex items-center justify-center gap-4">
          <Button
            variant={userVoteType === "UP" ? "default" : "outline"}
            size="sm"
            onClick={() => handleVote("UP")}
            disabled={!token}
          >
            <ThumbsUp size={16} />
            추천 {voteUp}
          </Button>
          <Button
            variant={userVoteType === "DOWN" ? "destructive" : "outline"}
            size="sm"
            onClick={() => handleVote("DOWN")}
            disabled={!token}
          >
            <ThumbsDown size={16} />
            비추천 {voteDown}
          </Button>
        </div>
      )}

      {/* 수정/삭제 버튼 */}
      {(canModify || canDelete) && (
        <div className="mt-4 flex items-center justify-end gap-2">
          {canModify && (
            <Button variant="outline" size="sm" onClick={() => onEdit(post.id)}>
              <PencilSimple size={16} />
              수정
            </Button>
          )}
          {canDelete && (
            <Button variant="destructive" size="sm" onClick={handleDelete}>
              <Trash size={16} />
              삭제
            </Button>
          )}
        </div>
      )}

      {/* 구분선 */}
      <hr className="my-8" />

      {/* 댓글 섹션 */}
      <CommentSection
        boardId={boardId}
        postId={post.id}
        permissions={permissions}
        settings={settings}
        currentUserId={currentUserId}
      />
    </div>
  );
}

/**
 * 콘텐츠 유형에 따라 렌더링
 * - HTML: DOMPurify 위생 처리 후 렌더링
 * - MARKDOWN: react-markdown + remark-gfm
 * - TEXT: <pre> 텍스트 렌더링
 */
function renderContent(content: string | null, contentType: string) {
  if (!content) {
    return <p className="text-muted-foreground">콘텐츠가 없습니다.</p>;
  }

  switch (contentType) {
    case "HTML":
      return <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(content) }} />;
    case "MARKDOWN":
      return <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>;
    case "TEXT":
    default:
      return (
        <pre className="whitespace-pre-wrap rounded-lg bg-muted p-4 text-sm">
          {content}
        </pre>
      );
  }
}
