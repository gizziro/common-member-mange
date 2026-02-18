"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { apiGet } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { PostDetail } from "./PostDetail";
import { PostForm } from "./PostForm";
import {
  PencilSimpleLine,
  MagnifyingGlass,
  Lock,
  Megaphone,
  Paperclip,
  CaretLeft,
  CaretRight,
  ChatCircle,
} from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

/** 게시글 목록 항목 */
interface PostListItem {
  id: string;
  boardInstanceId: string;
  categoryId: string | null;
  categoryName: string | null;
  title: string;
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
  hasFiles: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Spring Page 응답 */
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** 컴포넌트 Props */
interface BoardViewerProps {
  /** 게시판 인스턴스 ID */
  boardId: string;
  /** 게시판 이름 */
  boardName: string;
  /** 게시판 slug */
  boardSlug: string;
  /** 모듈 slug */
  moduleSlug: string;
  /** 리소스별 권한 목록 */
  permissions: Record<string, string[]>;
  /** URL 하위 경로 (null → 목록, "write" → 작성, "{id}" → 상세, "{id}/edit" → 수정) */
  subPath: string | null;
}

/* ===========================
 * BoardViewer — 게시판 뷰어 (라우팅 분기)
 * =========================== */

export function BoardViewer({
  boardId,
  boardName,
  boardSlug,
  moduleSlug,
  permissions,
  subPath,
}: BoardViewerProps) {
  const router = useRouter();

  // subPath 파싱으로 뷰 결정
  const navigate = (path: string | null) => {
    if (path === null) {
      router.push(`/${moduleSlug}/${boardSlug}`);
    } else {
      router.push(`/${moduleSlug}/${boardSlug}/${path}`);
    }
  };

  // 뷰 결정
  if (subPath === "write") {
    return (
      <PostForm
        boardId={boardId}
        boardName={boardName}
        permissions={permissions}
        onBack={() => navigate(null)}
        onSaved={(postId) => navigate(postId)}
      />
    );
  }

  if (subPath && subPath.endsWith("/edit")) {
    const editPostId = subPath.replace("/edit", "");
    return (
      <PostForm
        boardId={boardId}
        boardName={boardName}
        postId={editPostId}
        permissions={permissions}
        onBack={() => navigate(null)}
        onSaved={(postId) => navigate(postId)}
      />
    );
  }

  if (subPath && subPath !== "write") {
    return (
      <PostDetail
        boardId={boardId}
        postId={subPath}
        boardName={boardName}
        moduleSlug={moduleSlug}
        boardSlug={boardSlug}
        permissions={permissions}
        onBack={() => navigate(null)}
        onEdit={(postId) => navigate(`${postId}/edit`)}
      />
    );
  }

  // 기본: 게시글 목록
  return (
    <PostListView
      boardId={boardId}
      boardName={boardName}
      boardSlug={boardSlug}
      moduleSlug={moduleSlug}
      permissions={permissions}
      onNavigate={navigate}
    />
  );
}

/* ===========================
 * PostListView — 게시글 목록
 * =========================== */

interface PostListViewProps {
  boardId: string;
  boardName: string;
  boardSlug: string;
  moduleSlug: string;
  permissions: Record<string, string[]>;
  onNavigate: (path: string | null) => void;
}

function PostListView({
  boardId,
  boardName,
  boardSlug,
  moduleSlug,
  permissions,
  onNavigate,
}: PostListViewProps) {
  const [posts, setPosts] = useState<PostListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // 검색
  const [searchKeyword, setSearchKeyword] = useState("");
  const [activeKeyword, setActiveKeyword] = useState("");

  // 카테고리 필터
  const [categories, setCategories] = useState<string[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>("");

  // 인증 토큰
  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") ?? undefined : undefined;

  // 글쓰기 권한
  const canWrite = permissions?.post?.includes("write") ?? false;

  // 게시글 목록 로드
  const loadPosts = useCallback(async (p: number, keyword?: string, categoryId?: string) => {
    setLoading(true);
    try {
      let path: string;
      if (keyword) {
        path = `/boards/${boardId}/posts/search?keyword=${encodeURIComponent(keyword)}&page=${p}&size=20&sort=createdAt,desc`;
      } else {
        path = `/boards/${boardId}/posts?page=${p}&size=20&sort=createdAt,desc`;
        if (categoryId) path += `&categoryId=${categoryId}`;
      }

      const res = await apiGet<PageResponse<PostListItem>>(path, token);
      if (res.success && res.data) {
        setPosts(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
        setPage(res.data.number);

        // 카테고리 목록 추출 (첫 로드 시)
        if (categories.length === 0 && !keyword) {
          const catNames = Array.from(
            new Set(res.data.content.map((p) => p.categoryName).filter(Boolean))
          ) as string[];
          setCategories(catNames);
        }
      }
    } catch {
      // 로드 실패
    } finally {
      setLoading(false);
    }
  }, [boardId, token, categories.length]);

  useEffect(() => {
    loadPosts(0);
  }, [loadPosts]);

  // 검색 실행
  const handleSearch = () => {
    setActiveKeyword(searchKeyword);
    setSelectedCategory("");
    loadPosts(0, searchKeyword);
  };

  // 카테고리 필터
  const handleCategoryFilter = (cat: string) => {
    setSelectedCategory(cat);
    setActiveKeyword("");
    setSearchKeyword("");
    // 카테고리 ID가 아닌 이름으로 필터링 — 서버에서 categoryId로 필터하므로 여기선 클라이언트 필터
    // 실제로는 서버에 categoryId를 보내야 하지만, 목록에서 categoryId가 있으므로 전체 재로드 후 클라이언트 필터
    loadPosts(0, undefined, undefined);
  };

  // 날짜 포맷
  const formatDate = (d: string) => {
    const date = new Date(d);
    const today = new Date();
    if (date.toDateString() === today.toDateString()) {
      return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
    }
    return date.toLocaleDateString("ko-KR", { month: "2-digit", day: "2-digit" });
  };

  // 공지글 분리
  const noticePosts = posts.filter((p) => p.isNotice);
  const normalPosts = posts.filter((p) => !p.isNotice);
  // 카테고리 클라이언트 필터 적용
  const filteredNormal = selectedCategory
    ? normalPosts.filter((p) => p.categoryName === selectedCategory)
    : normalPosts;

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">{boardName}</h1>
        {canWrite && (
          <Button size="sm" onClick={() => onNavigate("write")}>
            <PencilSimpleLine size={16} />
            글쓰기
          </Button>
        )}
      </div>

      {/* 카테고리 필터 */}
      {categories.length > 0 && (
        <div className="flex items-center gap-2 mb-4 flex-wrap">
          <button
            onClick={() => { setSelectedCategory(""); loadPosts(0); }}
            className={`px-3 py-1 text-sm rounded-full border transition-colors ${
              !selectedCategory ? "bg-primary text-primary-foreground" : "hover:bg-muted"
            }`}
          >
            전체
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => handleCategoryFilter(cat)}
              className={`px-3 py-1 text-sm rounded-full border transition-colors ${
                selectedCategory === cat ? "bg-primary text-primary-foreground" : "hover:bg-muted"
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      )}

      {/* 검색 */}
      <div className="flex gap-2 mb-4">
        <div className="relative flex-1 max-w-sm">
          <input
            type="text"
            placeholder="검색..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") handleSearch(); }}
            className="w-full rounded-lg border px-3 py-2 pl-9 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          />
          <MagnifyingGlass size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        </div>
        <Button variant="outline" size="sm" onClick={handleSearch}>검색</Button>
        {activeKeyword && (
          <Button variant="ghost" size="sm" onClick={() => {
            setActiveKeyword("");
            setSearchKeyword("");
            loadPosts(0);
          }}>
            초기화
          </Button>
        )}
      </div>

      {/* 게시글 목록 */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : (
        <div className="rounded-lg border overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/50">
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">제목</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground w-24">작성자</th>
                <th className="px-4 py-3 text-center font-medium text-muted-foreground w-16">조회</th>
                <th className="px-4 py-3 text-center font-medium text-muted-foreground w-16">추천</th>
                <th className="px-4 py-3 text-right font-medium text-muted-foreground w-20">날짜</th>
              </tr>
            </thead>
            <tbody>
              {/* 공지글 (항상 상단) */}
              {noticePosts.map((post) => (
                <tr key={post.id} className="border-b bg-primary/5 hover:bg-primary/10 transition-colors cursor-pointer"
                  onClick={() => onNavigate(post.id)}>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1.5">
                      <Badge variant="default" className="shrink-0">공지</Badge>
                      <span className="font-medium truncate">{post.title}</span>
                      {post.commentCount > 0 && (
                        <span className="flex items-center gap-0.5 text-xs text-muted-foreground shrink-0">
                          <ChatCircle size={12} />{post.commentCount}
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground text-xs">{post.authorName}</td>
                  <td className="px-4 py-3 text-center text-muted-foreground">{post.viewCount}</td>
                  <td className="px-4 py-3 text-center text-muted-foreground">{post.voteUpCount}</td>
                  <td className="px-4 py-3 text-right text-muted-foreground text-xs">{formatDate(post.createdAt)}</td>
                </tr>
              ))}

              {/* 일반 게시글 */}
              {filteredNormal.length === 0 && noticePosts.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-muted-foreground">
                    {activeKeyword ? `"${activeKeyword}" 검색 결과가 없습니다.` : "게시글이 없습니다."}
                  </td>
                </tr>
              ) : (
                filteredNormal.map((post) => (
                  <tr key={post.id} className="border-b hover:bg-muted/50 transition-colors cursor-pointer"
                    onClick={() => onNavigate(post.id)}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1.5">
                        {post.categoryName && (
                          <Badge variant="outline" className="shrink-0 text-xs">{post.categoryName}</Badge>
                        )}
                        <span className="truncate">{post.title}</span>
                        {post.isSecret && <Lock size={14} className="text-muted-foreground shrink-0" />}
                        {post.hasFiles && <Paperclip size={14} className="text-muted-foreground shrink-0" />}
                        {post.commentCount > 0 && (
                          <span className="flex items-center gap-0.5 text-xs text-muted-foreground shrink-0">
                            <ChatCircle size={12} />{post.commentCount}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{post.authorName}</td>
                    <td className="px-4 py-3 text-center text-muted-foreground">{post.viewCount}</td>
                    <td className="px-4 py-3 text-center text-muted-foreground">{post.voteUpCount}</td>
                    <td className="px-4 py-3 text-right text-muted-foreground text-xs">{formatDate(post.createdAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-1 mt-6">
          <Button
            variant="outline" size="icon-sm" disabled={page === 0}
            onClick={() => loadPosts(page - 1, activeKeyword || undefined)}
          >
            <CaretLeft size={16} />
          </Button>
          {getVisiblePages(page, totalPages).map((p, i) =>
            p === -1 ? (
              <span key={`dots-${i}`} className="px-2 text-muted-foreground">...</span>
            ) : (
              <Button
                key={p}
                variant={p === page ? "default" : "outline"}
                size="sm"
                onClick={() => loadPosts(p, activeKeyword || undefined)}
              >
                {p + 1}
              </Button>
            )
          )}
          <Button
            variant="outline" size="icon-sm" disabled={page === totalPages - 1}
            onClick={() => loadPosts(page + 1, activeKeyword || undefined)}
          >
            <CaretRight size={16} />
          </Button>
        </div>
      )}

      {/* 총 게시글 수 */}
      <p className="mt-4 text-center text-xs text-muted-foreground">
        총 {totalElements}건
      </p>
    </div>
  );
}

/** 표시할 페이지 번호 배열 계산 (-1은 dots) */
function getVisiblePages(current: number, total: number): number[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  if (current <= 2) return [0, 1, 2, 3, -1, total - 1];
  if (current >= total - 3) return [0, -1, total - 4, total - 3, total - 2, total - 1];
  return [0, -1, current - 1, current, current + 1, -1, total - 1];
}
