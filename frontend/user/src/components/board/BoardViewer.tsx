"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import { apiGet } from "@/lib/api";
import type { PageResponse } from "@/types/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { PostDetail } from "./PostDetail";
import { PostForm } from "./PostForm";
import {
  PencilSimpleLine,
  MagnifyingGlass,
  Lock,
  Paperclip,
  CaretLeft,
  CaretRight,
  ChatCircle,
  SortAscending,
  Tag,
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

/** 게시판 설정 응답 */
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

/** 카테고리 응답 */
interface CategoryItem {
  id: string;
  boardInstanceId: string;
  name: string;
  slug: string;
  description: string | null;
  sortOrder: number;
  isActive: boolean;
}

/** 태그 응답 */
interface TagItem {
  id: string;
  name: string;
  slug: string;
  postCount: number;
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

/** 정렬 옵션 */
const SORT_OPTIONS = [
  { value: "newest", label: "최신순" },
  { value: "oldest", label: "오래된순" },
  { value: "viewCount", label: "조회순" },
  { value: "voteUp", label: "추천순" },
  { value: "commentCount", label: "댓글순" },
] as const;

/** 검색 유형 옵션 */
const SEARCH_TYPE_OPTIONS = [
  { value: "all", label: "전체" },
  { value: "title", label: "제목" },
  { value: "author", label: "작성자" },
] as const;

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

  // 게시판 설정 상태
  const [settings, setSettings] = useState<BoardSettings | null>(null);

  // 게시판 설정 로드
  useEffect(() => {
    const loadSettings = async () => {
      try {
        const res = await apiGet<BoardSettings>(`/boards/${boardId}/settings`);
        if (res.success && res.data) {
          setSettings(res.data);
        }
      } catch {
        // 설정 로드 실패 — 기본값 사용
      }
    };
    loadSettings();
  }, [boardId]);

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
        settings={settings}
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
        settings={settings}
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
        settings={settings}
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
      settings={settings}
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
  settings: BoardSettings | null;
  onNavigate: (path: string | null) => void;
}

function PostListView({
  boardId,
  boardName,
  boardSlug,
  moduleSlug,
  permissions,
  settings,
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
  const [searchType, setSearchType] = useState("all");

  // 정렬
  const [sortField, setSortField] = useState("newest");

  // 카테고리 필터
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>("");

  // 태그 필터
  const [tags, setTags] = useState<TagItem[]>([]);
  const [selectedTagId, setSelectedTagId] = useState<string>("");

  // 글쓰기 권한
  const canWrite = permissions?.post?.includes("write") ?? false;

  // 페이지당 게시글 수 (설정에서 가져오기, 기본 20)
  const pageSize = settings?.postsPerPage ?? 20;

  // 카테고리 + 태그 목록 로드
  useEffect(() => {
    const loadMeta = async () => {
      // 카테고리 로드 (useCategory가 true이거나 설정 미로드 시)
      if (settings === null || settings.useCategory) {
        try {
          const res = await apiGet<CategoryItem[]>(`/boards/${boardId}/categories`);
          if (res.success && res.data) {
            // 활성화된 카테고리만 필터
            setCategories(res.data.filter((c) => c.isActive));
          }
        } catch {
          // 카테고리 로드 실패
        }
      }

      // 태그 로드 (allowTags가 true이거나 설정 미로드 시)
      if (settings === null || settings.allowTags) {
        try {
          const res = await apiGet<TagItem[]>(`/boards/${boardId}/tags`);
          if (res.success && res.data) {
            setTags(res.data);
          }
        } catch {
          // 태그 로드 실패
        }
      }
    };
    loadMeta();
  }, [boardId, settings]);

  // 추가 로드 중 여부 (무한스크롤/더보기용)
  const [loadingMore, setLoadingMore] = useState(false);

  // 페이지네이션 유형 (설정에서 가져오기, 기본 OFFSET)
  const paginationType = settings?.paginationType ?? "OFFSET";

  // 게시글 목록 로드 (append: true이면 기존 목록에 추가)
  const loadPosts = useCallback(async (
    p: number,
    keyword?: string,
    currentSearchType?: string,
    categoryId?: string,
    tagId?: string,
    sort?: string,
    append?: boolean,
  ) => {
    if (append) {
      setLoadingMore(true);
    } else {
      setLoading(true);
    }
    try {
      let path: string;
      if (keyword) {
        // 검색 모드
        const st = currentSearchType || "all";
        path = `/boards/${boardId}/posts/search?keyword=${encodeURIComponent(keyword)}&searchType=${st}&page=${p}&size=${pageSize}`;
      } else {
        // 일반 목록 모드
        const s = sort || sortField;
        path = `/boards/${boardId}/posts?page=${p}&size=${pageSize}&sort=${s}`;
        if (categoryId) path += `&categoryId=${categoryId}`;
        if (tagId) path += `&tagId=${tagId}`;
      }

      const res = await apiGet<PageResponse<PostListItem>>(path);
      if (res.success && res.data) {
        if (append) {
          // 기존 목록에 새 게시글 추가 (무한스크롤/더보기)
          setPosts((prev) => [...prev, ...res.data!.content]);
        } else {
          setPosts(res.data.content);
        }
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
        setPage(res.data.number);
      }
    } catch {
      // 로드 실패
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [boardId, pageSize, sortField]);

  useEffect(() => {
    loadPosts(0);
  }, [loadPosts]);

  // 검색 실행
  const handleSearch = () => {
    setActiveKeyword(searchKeyword);
    setSelectedCategoryId("");
    setSelectedTagId("");
    loadPosts(0, searchKeyword, searchType);
  };

  // 검색 초기화
  const handleResetSearch = () => {
    setActiveKeyword("");
    setSearchKeyword("");
    setSearchType("all");
    loadPosts(0, undefined, undefined, selectedCategoryId, selectedTagId, sortField);
  };

  // 카테고리 필터 변경
  const handleCategoryFilter = (catId: string) => {
    setSelectedCategoryId(catId);
    setSelectedTagId("");
    setActiveKeyword("");
    setSearchKeyword("");
    loadPosts(0, undefined, undefined, catId, undefined, sortField);
  };

  // 태그 필터 변경
  const handleTagFilter = (tagId: string) => {
    // 같은 태그 클릭 시 해제
    const newTagId = selectedTagId === tagId ? "" : tagId;
    setSelectedTagId(newTagId);
    setSelectedCategoryId("");
    setActiveKeyword("");
    setSearchKeyword("");
    loadPosts(0, undefined, undefined, undefined, newTagId || undefined, sortField);
  };

  // 정렬 변경
  const handleSortChange = (newSort: string) => {
    setSortField(newSort);
    if (activeKeyword) {
      loadPosts(0, activeKeyword, searchType);
    } else {
      loadPosts(0, undefined, undefined, selectedCategoryId || undefined, selectedTagId || undefined, newSort);
    }
  };

  // 페이지 변경
  const handlePageChange = (p: number) => {
    if (activeKeyword) {
      loadPosts(p, activeKeyword, searchType);
    } else {
      loadPosts(p, undefined, undefined, selectedCategoryId || undefined, selectedTagId || undefined, sortField);
    }
  };

  // 다음 페이지 추가 로드 (무한스크롤/더보기 공용)
  const loadNextPage = () => {
    if (page < totalPages - 1 && !loadingMore) {
      const nextPage = page + 1;
      if (activeKeyword) {
        loadPosts(nextPage, activeKeyword, searchType, undefined, undefined, undefined, true);
      } else {
        loadPosts(nextPage, undefined, undefined, selectedCategoryId || undefined, selectedTagId || undefined, sortField, true);
      }
    }
  };

  // 무한 스크롤 IntersectionObserver 설정
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (paginationType !== "INFINITE_SCROLL") return;

    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        // 센티넬이 화면에 보이면 다음 페이지 로드
        if (entries[0].isIntersecting && !loadingMore && page < totalPages - 1) {
          loadNextPage();
        }
      },
      { threshold: 0.1 }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [paginationType, page, totalPages, loadingMore, activeKeyword, searchType, selectedCategoryId, selectedTagId, sortField]);

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

      {/* 카테고리 필터 (useCategory 설정 시에만 표시) */}
      {(settings === null || settings.useCategory) && categories.length > 0 && (
        <div className="flex items-center gap-2 mb-4 flex-wrap">
          <button
            onClick={() => handleCategoryFilter("")}
            className={`px-3 py-1 text-sm rounded-full border transition-colors ${
              !selectedCategoryId ? "bg-primary text-primary-foreground" : "hover:bg-muted"
            }`}
          >
            전체
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => handleCategoryFilter(cat.id)}
              className={`px-3 py-1 text-sm rounded-full border transition-colors ${
                selectedCategoryId === cat.id ? "bg-primary text-primary-foreground" : "hover:bg-muted"
              }`}
            >
              {cat.name}
            </button>
          ))}
        </div>
      )}

      {/* 태그 필터 (allowTags 설정 시에만 표시) */}
      {(settings === null || settings.allowTags) && tags.length > 0 && (
        <div className="flex items-center gap-1.5 mb-4 flex-wrap">
          <Tag size={16} className="text-muted-foreground shrink-0" />
          {tags.map((tag) => (
            <button
              key={tag.id}
              onClick={() => handleTagFilter(tag.id)}
              className={`px-2.5 py-0.5 text-xs rounded-full border transition-colors ${
                selectedTagId === tag.id
                  ? "bg-primary text-primary-foreground"
                  : "hover:bg-muted text-muted-foreground"
              }`}
            >
              {tag.name} ({tag.postCount})
            </button>
          ))}
        </div>
      )}

      {/* 검색 + 정렬 */}
      <div className="flex items-center gap-2 mb-4 flex-wrap">
        {/* 검색 유형 선택 */}
        <select
          value={searchType}
          onChange={(e) => setSearchType(e.target.value)}
          className="rounded-lg border px-2 py-2 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
        >
          {SEARCH_TYPE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>

        {/* 검색 입력 */}
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
          <Button variant="ghost" size="sm" onClick={handleResetSearch}>초기화</Button>
        )}

        {/* 정렬 드롭다운 (검색 중이 아닐 때만) */}
        {!activeKeyword && (
          <div className="ml-auto flex items-center gap-1.5">
            <SortAscending size={16} className="text-muted-foreground" />
            <select
              value={sortField}
              onChange={(e) => handleSortChange(e.target.value)}
              className="rounded-lg border px-2 py-2 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>
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
              {normalPosts.length === 0 && noticePosts.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-muted-foreground">
                    {activeKeyword ? `"${activeKeyword}" 검색 결과가 없습니다.` : "게시글이 없습니다."}
                  </td>
                </tr>
              ) : (
                normalPosts.map((post) => (
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

      {/* 페이지네이션 — 유형별 분기 렌더링 */}
      {paginationType === "INFINITE_SCROLL" ? (
        <>
          {/* 무한 스크롤: 스크롤 하단 감지용 센티넬 */}
          <div ref={sentinelRef} className="h-4" />
          {loadingMore && (
            <div className="flex items-center justify-center py-4">
              <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          )}
        </>
      ) : paginationType === "LOAD_MORE" ? (
        <>
          {/* 더보기 버튼 */}
          {page < totalPages - 1 && (
            <div className="flex items-center justify-center mt-6">
              <Button
                variant="outline"
                onClick={loadNextPage}
                disabled={loadingMore}
              >
                {loadingMore ? "로딩 중..." : "더보기"}
              </Button>
            </div>
          )}
        </>
      ) : (
        <>
          {/* OFFSET(기본): 번호 기반 페이지네이션 */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-1 mt-6">
              <Button
                variant="outline" size="icon-sm" disabled={page === 0}
                onClick={() => handlePageChange(page - 1)}
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
                    onClick={() => handlePageChange(p)}
                  >
                    {p + 1}
                  </Button>
                )
              )}
              <Button
                variant="outline" size="icon-sm" disabled={page === totalPages - 1}
                onClick={() => handlePageChange(page + 1)}
              >
                <CaretRight size={16} />
              </Button>
            </div>
          )}
        </>
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
