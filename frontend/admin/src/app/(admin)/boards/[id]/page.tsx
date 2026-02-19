"use client";

import { useEffect, useState, useCallback, type FormEvent } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { apiGet, apiPost, apiPut, apiDelete, apiPatch, type ApiResponse } from "@/lib/api";
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
  Pagination,
} from "@/components/ui";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
} from "@/components/ui/tabs";
import { Label } from "@/components/ui/label";
import { ArrowLeft, FloppyDisk, Plus, Trash, Megaphone } from "@phosphor-icons/react";
import { toast } from "sonner";

/* ===========================
 * 타입 정의
 * =========================== */

/** 게시판 상세 응답 */
interface BoardDetail {
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

/** 카테고리 */
interface Category {
  id: string;
  boardInstanceId: string;
  name: string;
  slug: string;
  description: string | null;
  sortOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 게시글 목록 항목 */
interface PostItem {
  id: string;
  title: string;
  authorName: string;
  categoryName: string | null;
  isNotice: boolean;
  isSecret: boolean;
  isDraft: boolean;
  viewCount: number;
  commentCount: number;
  voteUpCount: number;
  createdAt: string;
}

/** 댓글 항목 (트리) */
interface CommentItem {
  id: string;
  postId: string;
  parentId: string | null;
  depth: number;
  content: string;
  authorId: string;
  authorName: string;
  isDeleted: boolean;
  children: CommentItem[];
  createdAt: string;
}

/** 권한 정의 */
interface PermissionDef {
  id: string;
  resource: string;
  action: string;
  name: string;
  flatCode: string;
}

/** 그룹별 권한 */
interface GroupPermission {
  groupId: string;
  groupName: string;
  groupCode: string;
  grantedPermissionIds: string[];
}

/** 사용자별 권한 */
interface UserPermission {
  userId: string;
  loginId: string;
  username: string;
  grantedPermissionIds: string[];
}

/** 부관리자 */
interface BoardAdmin {
  boardInstanceId: string;
  userId: string;
  username: string;
  createdAt: string;
}

/** 페이징 응답 (백엔드 PageResponseDto 매핑) */
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/* ===========================
 * 게시판 상세 페이지
 * =========================== */

export default function BoardDetailPage() {
  const params = useParams();
  const boardId = params.id as string;

  /* ─── 공통 상태 ─── */
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [board, setBoard] = useState<BoardDetail | null>(null);

  /* ─── 기본 정보 탭 ─── */
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);

  /* ─── 설정 탭 ─── */
  const [settings, setSettings] = useState<BoardSettings | null>(null);
  const [settingsLoaded, setSettingsLoaded] = useState(false);
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsSaving, setSettingsSaving] = useState(false);

  /* ─── 카테고리 탭 ─── */
  const [categories, setCategories] = useState<Category[]>([]);
  const [catLoaded, setCatLoaded] = useState(false);
  const [catLoading, setCatLoading] = useState(false);
  const [catModalOpen, setCatModalOpen] = useState(false);
  const [catEditTarget, setCatEditTarget] = useState<Category | null>(null);
  const [catName, setCatName] = useState("");
  const [catSlug, setCatSlug] = useState("");
  const [catDesc, setCatDesc] = useState("");
  const [catSort, setCatSort] = useState(0);
  const [catSaving, setCatSaving] = useState(false);
  const [catDeleteTarget, setCatDeleteTarget] = useState<Category | null>(null);

  /* ─── 게시글 탭 ─── */
  const [posts, setPosts] = useState<PostItem[]>([]);
  const [postsLoaded, setPostsLoaded] = useState(false);
  const [postsLoading, setPostsLoading] = useState(false);
  const [postPage, setPostPage] = useState(0);
  const [postTotalPages, setPostTotalPages] = useState(0);

  /* ─── 댓글 탭 ─── */
  const [commentsLoaded, setCommentsLoaded] = useState(false);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentPostList, setCommentPostList] = useState<PostItem[]>([]);
  const [selectedPostId, setSelectedPostId] = useState<string>("");
  const [comments, setComments] = useState<CommentItem[]>([]);

  /* ─── 권한 탭 ─── */
  const [permLoaded, setPermLoaded] = useState(false);
  const [permLoading, setPermLoading] = useState(false);
  const [availablePerms, setAvailablePerms] = useState<PermissionDef[]>([]);
  const [groupPerms, setGroupPerms] = useState<GroupPermission[]>([]);
  const [userPerms, setUserPerms] = useState<UserPermission[]>([]);
  const [permSaving, setPermSaving] = useState<string | null>(null);
  const [addUserLoginId, setAddUserLoginId] = useState("");
  const [addingUser, setAddingUser] = useState(false);

  /* ─── 부관리자 탭 ─── */
  const [admins, setAdmins] = useState<BoardAdmin[]>([]);
  const [adminsLoaded, setAdminsLoaded] = useState(false);
  const [adminsLoading, setAdminsLoading] = useState(false);
  const [addAdminId, setAddAdminId] = useState("");
  const [addingAdmin, setAddingAdmin] = useState(false);

  /* ─── 날짜 포맷 ─── */
  const formatDate = (d: string) =>
    new Date(d).toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" });
  const formatDateTime = (d: string) =>
    new Date(d).toLocaleString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });

  /* ============================================
   * 기본 정보 로드
   * ============================================ */
  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const res = await apiGet<BoardDetail>(`/boards/${boardId}`);
        if (res.success && res.data) {
          setBoard(res.data);
          setName(res.data.name);
          setSlug(res.data.slug);
          setDescription(res.data.description ?? "");
        } else {
          setError(res.error?.message ?? "게시판을 불러올 수 없습니다.");
        }
      } catch {
        setError("서버에 연결할 수 없습니다.");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [boardId]);

  /* 기본 정보 저장 */
  const handleSaveBasic = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res: ApiResponse = await apiPut(`/boards/${boardId}`, { name, slug, description });
      if (res.success) {
        toast.success("게시판 정보가 저장되었습니다.");
      } else {
        toast.error(res.error?.message ?? "저장에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setSaving(false);
    }
  };

  /* ============================================
   * 설정 탭
   * ============================================ */
  const loadSettings = useCallback(async () => {
    if (settingsLoaded) return;
    setSettingsLoading(true);
    try {
      const res = await apiGet<BoardSettings>(`/boards/${boardId}/settings`);
      if (res.success && res.data) {
        setSettings(res.data);
        setSettingsLoaded(true);
      }
    } catch {
      toast.error("설정을 불러올 수 없습니다.");
    } finally {
      setSettingsLoading(false);
    }
  }, [boardId, settingsLoaded]);

  const handleSaveSettings = async () => {
    if (!settings) return;
    setSettingsSaving(true);
    try {
      const res = await apiPut(`/boards/${boardId}/settings`, settings);
      if (res.success) {
        toast.success("설정이 저장되었습니다.");
      } else {
        toast.error(res.error?.message ?? "설정 저장에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setSettingsSaving(false);
    }
  };

  /* 설정 값 변경 헬퍼 */
  const updateSetting = <K extends keyof BoardSettings>(key: K, value: BoardSettings[K]) => {
    setSettings((prev) => (prev ? { ...prev, [key]: value } : prev));
  };

  /* ============================================
   * 카테고리 탭
   * ============================================ */
  const loadCategories = useCallback(async () => {
    if (catLoaded) return;
    setCatLoading(true);
    try {
      const res = await apiGet<Category[]>(`/boards/${boardId}/categories`);
      if (res.success && res.data) {
        setCategories(res.data);
        setCatLoaded(true);
      }
    } catch {
      toast.error("카테고리를 불러올 수 없습니다.");
    } finally {
      setCatLoading(false);
    }
  }, [boardId, catLoaded]);

  const openCatModal = (cat?: Category) => {
    if (cat) {
      setCatEditTarget(cat);
      setCatName(cat.name);
      setCatSlug(cat.slug);
      setCatDesc(cat.description ?? "");
      setCatSort(cat.sortOrder);
    } else {
      setCatEditTarget(null);
      setCatName("");
      setCatSlug("");
      setCatDesc("");
      setCatSort(0);
    }
    setCatModalOpen(true);
  };

  const handleSaveCategory = async (e: FormEvent) => {
    e.preventDefault();
    setCatSaving(true);
    try {
      const body = { name: catName, slug: catSlug, description: catDesc || undefined, sortOrder: catSort };
      let res: ApiResponse;
      if (catEditTarget) {
        res = await apiPut(`/boards/${boardId}/categories/${catEditTarget.id}`, body);
      } else {
        res = await apiPost(`/boards/${boardId}/categories`, body);
      }
      if (res.success) {
        setCatModalOpen(false);
        toast.success(catEditTarget ? "카테고리가 수정되었습니다." : "카테고리가 생성되었습니다.");
        setCatLoaded(false);
        loadCategories();
      } else {
        toast.error(res.error?.message ?? "저장에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setCatSaving(false);
    }
  };

  const handleDeleteCategory = async () => {
    if (!catDeleteTarget) return;
    try {
      const res = await apiDelete(`/boards/${boardId}/categories/${catDeleteTarget.id}`);
      if (res.success) {
        toast.success("카테고리가 삭제되었습니다.");
        setCatDeleteTarget(null);
        setCatLoaded(false);
        loadCategories();
      }
    } catch {
      toast.error("삭제에 실패했습니다.");
    }
  };

  /* ============================================
   * 게시글 탭
   * ============================================ */
  const loadPosts = useCallback(async (page = 0) => {
    setPostsLoading(true);
    try {
      const res = await apiGet<PageResponse<PostItem>>(`/boards/${boardId}/posts?page=${page}&size=20&sort=createdAt,desc`);
      if (res.success && res.data) {
        setPosts(res.data.content);
        setPostTotalPages(res.data.totalPages);
        setPostPage(res.data.page);
        setPostsLoaded(true);
      }
    } catch {
      toast.error("게시글을 불러올 수 없습니다.");
    } finally {
      setPostsLoading(false);
    }
  }, [boardId]);

  const toggleNotice = async (postId: string) => {
    const res = await apiPatch(`/boards/${boardId}/posts/${postId}/notice?scope=BOARD`);
    if (res.success) {
      toast.success("공지 상태가 변경되었습니다.");
      loadPosts(postPage);
    }
  };

  const deletePost = async (postId: string) => {
    const res = await apiDelete(`/boards/${boardId}/posts/${postId}`);
    if (res.success) {
      toast.success("게시글이 삭제되었습니다.");
      loadPosts(postPage);
    }
  };

  /* ============================================
   * 댓글 탭
   * ============================================ */
  const loadCommentPostList = useCallback(async () => {
    if (commentsLoaded) return;
    setCommentsLoading(true);
    try {
      // 게시글 목록을 가져와서 드롭다운에 사용
      const res = await apiGet<PageResponse<PostItem>>(`/boards/${boardId}/posts?page=0&size=100&sort=createdAt,desc`);
      if (res.success && res.data) {
        setCommentPostList(res.data.content);
        setCommentsLoaded(true);
      }
    } catch {
      toast.error("게시글 목록을 불러올 수 없습니다.");
    } finally {
      setCommentsLoading(false);
    }
  }, [boardId, commentsLoaded]);

  const loadComments = async (postId: string) => {
    if (!postId) return;
    try {
      const res = await apiGet<CommentItem[]>(`/boards/${boardId}/posts/${postId}/comments`);
      if (res.success && res.data) {
        setComments(res.data);
      }
    } catch {
      toast.error("댓글을 불러올 수 없습니다.");
    }
  };

  const deleteComment = async (commentId: string) => {
    const res = await apiDelete(`/boards/${boardId}/comments/${commentId}`);
    if (res.success) {
      toast.success("댓글이 삭제되었습니다.");
      if (selectedPostId) loadComments(selectedPostId);
    }
  };

  /* ============================================
   * 권한 탭
   * ============================================ */
  const loadPermissions = useCallback(async () => {
    if (permLoaded) return;
    setPermLoading(true);
    try {
      const [defsRes, grpRes, userRes] = await Promise.all([
        apiGet<PermissionDef[]>("/boards/permission-definitions"),
        apiGet<GroupPermission[]>(`/boards/${boardId}/permissions`),
        apiGet<UserPermission[]>(`/boards/${boardId}/user-permissions`),
      ]);
      if (defsRes.success && defsRes.data) setAvailablePerms(defsRes.data);
      if (grpRes.success && grpRes.data) setGroupPerms(grpRes.data);
      if (userRes.success && userRes.data) setUserPerms(userRes.data);
      setPermLoaded(true);
    } catch {
      toast.error("권한 정보를 불러올 수 없습니다.");
    } finally {
      setPermLoading(false);
    }
  }, [boardId, permLoaded]);

  const toggleGroupPerm = (groupId: string, permId: string) => {
    setGroupPerms((prev) =>
      prev.map((gp) => {
        if (gp.groupId !== groupId) return gp;
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

  const saveGroupPerm = async (gp: GroupPermission) => {
    setPermSaving(`group:${gp.groupId}`);
    try {
      const res = await apiPut(`/boards/${boardId}/permissions`, {
        groupId: gp.groupId,
        permissionIds: gp.grantedPermissionIds,
      });
      if (res.success) toast.success(`${gp.groupName} 그룹 권한이 저장되었습니다.`);
      else toast.error(res.error?.message ?? "권한 저장에 실패했습니다.");
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setPermSaving(null);
    }
  };

  const toggleUserPerm = (userId: string, permId: string) => {
    setUserPerms((prev) =>
      prev.map((up) => {
        if (up.userId !== userId) return up;
        const has = up.grantedPermissionIds.includes(permId);
        return {
          ...up,
          grantedPermissionIds: has
            ? up.grantedPermissionIds.filter((id) => id !== permId)
            : [...up.grantedPermissionIds, permId],
        };
      })
    );
  };

  const saveUserPerm = async (up: UserPermission) => {
    setPermSaving(`user:${up.userId}`);
    try {
      const res = await apiPut(`/boards/${boardId}/user-permissions`, {
        userId: up.userId,
        permissionIds: up.grantedPermissionIds,
      });
      if (res.success) {
        toast.success(`${up.username} 사용자 권한이 저장되었습니다.`);
        if (up.grantedPermissionIds.length === 0) {
          setUserPerms((prev) => prev.filter((u) => u.userId !== up.userId));
        }
      } else {
        toast.error(res.error?.message ?? "권한 저장에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setPermSaving(null);
    }
  };

  const handleAddUser = async () => {
    if (!addUserLoginId.trim()) return;
    setAddingUser(true);
    try {
      const allUsersRes = await apiGet<{ content: Array<{ id: string; userId: string; username: string }> }>(
        `/users?page=0&size=1000&sort=createdAt,desc`
      );
      if (!allUsersRes.success || !allUsersRes.data) {
        toast.error("사용자 목록을 조회할 수 없습니다.");
        return;
      }
      const foundUser = allUsersRes.data.content.find((u) => u.userId === addUserLoginId.trim());
      if (!foundUser) {
        toast.error(`"${addUserLoginId}" 로그인 ID의 사용자를 찾을 수 없습니다.`);
        return;
      }
      if (userPerms.some((up) => up.userId === foundUser.id)) {
        toast.error("이미 권한이 설정된 사용자입니다.");
        return;
      }
      setUserPerms((prev) => [
        ...prev,
        { userId: foundUser.id, loginId: foundUser.userId, username: foundUser.username, grantedPermissionIds: [] },
      ]);
      setAddUserLoginId("");
      toast.success(`${foundUser.username} 사용자가 추가되었습니다.`);
    } catch {
      toast.error("사용자 조회에 실패했습니다.");
    } finally {
      setAddingUser(false);
    }
  };

  const removeUserPerm = async (up: UserPermission) => {
    setPermSaving(`user:${up.userId}`);
    try {
      const res = await apiPut(`/boards/${boardId}/user-permissions`, {
        userId: up.userId,
        permissionIds: [],
      });
      if (res.success) {
        setUserPerms((prev) => prev.filter((u) => u.userId !== up.userId));
        toast.success(`${up.username} 사용자 권한이 삭제되었습니다.`);
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setPermSaving(null);
    }
  };

  /* ============================================
   * 부관리자 탭
   * ============================================ */
  const loadAdmins = useCallback(async () => {
    if (adminsLoaded) return;
    setAdminsLoading(true);
    try {
      const res = await apiGet<BoardAdmin[]>(`/boards/${boardId}/admins`);
      if (res.success && res.data) {
        setAdmins(res.data);
        setAdminsLoaded(true);
      }
    } catch {
      toast.error("부관리자 목록을 불러올 수 없습니다.");
    } finally {
      setAdminsLoading(false);
    }
  }, [boardId, adminsLoaded]);

  const handleAddAdmin = async () => {
    if (!addAdminId.trim()) return;
    setAddingAdmin(true);
    try {
      const res = await apiPost<BoardAdmin>(`/boards/${boardId}/admins`, { userId: addAdminId.trim() });
      if (res.success && res.data) {
        setAdmins((prev) => [...prev, res.data!]);
        setAddAdminId("");
        toast.success("부관리자가 추가되었습니다.");
      } else {
        toast.error(res.error?.message ?? "추가에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setAddingAdmin(false);
    }
  };

  const removeAdmin = async (userId: string) => {
    const res = await apiDelete(`/boards/${boardId}/admins/${userId}`);
    if (res.success) {
      setAdmins((prev) => prev.filter((a) => a.userId !== userId));
      toast.success("부관리자가 삭제되었습니다.");
    } else {
      toast.error(res.error?.message ?? "삭제에 실패했습니다.");
    }
  };

  /* ============================================
   * 탭 변경 핸들러
   * ============================================ */
  const handleTabChange = (tab: string) => {
    if (tab === "settings") loadSettings();
    if (tab === "categories") loadCategories();
    if (tab === "posts" && !postsLoaded) loadPosts(0);
    if (tab === "comments") loadCommentPostList();
    if (tab === "permissions") loadPermissions();
    if (tab === "admins") loadAdmins();
  };

  /* ============================================
   * 로딩 상태
   * ============================================ */
  if (loading) {
    return (
      <>
        <PageHeader title="게시판 편집" subtitle="로딩 중..." />
        <div className="flex items-center justify-center py-24">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      </>
    );
  }

  /* ============================================
   * 렌더링
   * ============================================ */
  return (
    <>
      <PageHeader
        title="게시판 편집"
        subtitle={board ? `${board.name} (/board/${board.slug})` : ""}
        actions={
          <div className="flex gap-2">
            <Link href="/boards">
              <Button variant="light" size="sm">
                <ArrowLeft size={16} className="mr-1.5" />
                목록
              </Button>
            </Link>
          </div>
        }
      />

      <div className="p-5">
        {error && <Alert variant="danger" dismissible className="mb-4">{error}</Alert>}

        <Tabs defaultValue="basic" onValueChange={handleTabChange}>
          <TabsList variant="line">
            <TabsTrigger value="basic">기본 정보</TabsTrigger>
            <TabsTrigger value="settings">설정</TabsTrigger>
            <TabsTrigger value="categories">카테고리</TabsTrigger>
            <TabsTrigger value="posts">게시글</TabsTrigger>
            <TabsTrigger value="comments">댓글</TabsTrigger>
            <TabsTrigger value="permissions">권한</TabsTrigger>
            <TabsTrigger value="admins">부관리자</TabsTrigger>
          </TabsList>

          {/* ─── Tab 1: 기본 정보 ─── */}
          <TabsContent value="basic">
            <form id="edit-board-form" onSubmit={handleSaveBasic} className="space-y-5 mt-4">
              <Card>
                <CardBody>
                  <h3 className="text-sm font-semibold text-gray-700 mb-4">기본 정보</h3>
                  <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    <FormGroup label="이름" htmlFor="boardName" required>
                      <Input id="boardName" type="text" value={name} onChange={(e) => setName(e.target.value)} required />
                    </FormGroup>
                    <FormGroup label="슬러그" htmlFor="boardSlug" required>
                      <Input id="boardSlug" type="text" value={slug} onChange={(e) => setSlug(e.target.value)} required />
                      <p className="mt-1 text-xs text-gray-400">URL: /board/{slug}</p>
                    </FormGroup>
                  </div>
                  <div className="mt-4">
                    <FormGroup label="설명" htmlFor="boardDesc">
                      <Input id="boardDesc" type="text" value={description} onChange={(e) => setDescription(e.target.value)} />
                    </FormGroup>
                  </div>
                  <div className="mt-4 flex justify-end">
                    <Button variant="primary" size="sm" loading={saving} type="submit">
                      <FloppyDisk size={16} className="mr-1.5" />
                      저장
                    </Button>
                  </div>
                </CardBody>
              </Card>
            </form>
          </TabsContent>

          {/* ─── Tab 2: 설정 ─── */}
          <TabsContent value="settings">
            <div className="space-y-5 mt-4">
              {settingsLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : settings ? (
                <>
                  {/* 에디터/표시 설정 */}
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">에디터 / 표시</h3>
                      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                        <div className="space-y-1.5">
                          <Label>에디터 유형</Label>
                          <Select value={settings.editorType} onValueChange={(v) => updateSetting("editorType", v)}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                              <SelectItem value="MARKDOWN">Markdown</SelectItem>
                              <SelectItem value="HTML">HTML</SelectItem>
                              <SelectItem value="TEXT">텍스트</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="space-y-1.5">
                          <Label>표시 형식</Label>
                          <Select value={settings.displayFormat} onValueChange={(v) => updateSetting("displayFormat", v)}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                              <SelectItem value="LIST">리스트</SelectItem>
                              <SelectItem value="GALLERY">갤러리</SelectItem>
                              <SelectItem value="CARD">카드</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="space-y-1.5">
                          <Label>페이지네이션</Label>
                          <Select value={settings.paginationType} onValueChange={(v) => updateSetting("paginationType", v)}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                              <SelectItem value="NUMBERED">번호형</SelectItem>
                              <SelectItem value="INFINITE_SCROLL">무한 스크롤</SelectItem>
                              <SelectItem value="LOAD_MORE">더 보기</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        <FormGroup label="페이지당 게시글 수" htmlFor="postsPerPage">
                          <Input id="postsPerPage" type="number" min={1} max={100}
                            value={settings.postsPerPage} onChange={(e) => updateSetting("postsPerPage", Number(e.target.value))} />
                        </FormGroup>
                      </div>
                    </CardBody>
                  </Card>

                  {/* 파일 업로드 설정 */}
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">파일 업로드</h3>
                      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                        <div className="flex items-center gap-2">
                          <input type="checkbox" id="allowFileUpload" checked={settings.allowFileUpload}
                            onChange={(e) => updateSetting("allowFileUpload", e.target.checked)}
                            className="h-4 w-4 rounded border-gray-300 text-primary" />
                          <Label htmlFor="allowFileUpload">파일 업로드 허용</Label>
                        </div>
                        <FormGroup label="허용 파일 유형" htmlFor="allowedFileTypes">
                          <Input id="allowedFileTypes" type="text" value={settings.allowedFileTypes ?? ""}
                            onChange={(e) => updateSetting("allowedFileTypes", e.target.value)}
                            placeholder="jpg,png,pdf" />
                        </FormGroup>
                        <FormGroup label="최대 파일 크기(MB)" htmlFor="maxFileSize">
                          <Input id="maxFileSize" type="number" min={1}
                            value={Math.round((settings.maxFileSize ?? 0) / 1048576)}
                            onChange={(e) => updateSetting("maxFileSize", Number(e.target.value) * 1048576)} />
                        </FormGroup>
                        <FormGroup label="게시글당 최대 파일 수" htmlFor="maxFilesPerPost">
                          <Input id="maxFilesPerPost" type="number" min={0} max={50}
                            value={settings.maxFilesPerPost} onChange={(e) => updateSetting("maxFilesPerPost", Number(e.target.value))} />
                        </FormGroup>
                      </div>
                    </CardBody>
                  </Card>

                  {/* 기능 토글 */}
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">기능</h3>
                      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-6">
                        {([
                          ["allowAnonymousAccess", "비로그인 접근"],
                          ["allowSecretPosts", "비밀글"],
                          ["allowDraft", "임시 저장"],
                          ["allowTags", "태그"],
                          ["allowVote", "추천/비추천"],
                          ["useCategory", "카테고리"],
                        ] as const).map(([key, label]) => (
                          <div key={key} className="flex items-center gap-2">
                            <input type="checkbox" id={key}
                              checked={settings[key] as boolean}
                              onChange={(e) => updateSetting(key, e.target.checked)}
                              className="h-4 w-4 rounded border-gray-300 text-primary" />
                            <Label htmlFor={key}>{label}</Label>
                          </div>
                        ))}
                      </div>
                    </CardBody>
                  </Card>

                  {/* 깊이 제한 */}
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">깊이 제한</h3>
                      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 max-w-md">
                        <FormGroup label="최대 답글 깊이" htmlFor="maxReplyDepth">
                          <Input id="maxReplyDepth" type="number" min={0} max={10}
                            value={settings.maxReplyDepth} onChange={(e) => updateSetting("maxReplyDepth", Number(e.target.value))} />
                        </FormGroup>
                        <FormGroup label="최대 댓글 깊이" htmlFor="maxCommentDepth">
                          <Input id="maxCommentDepth" type="number" min={0} max={10}
                            value={settings.maxCommentDepth} onChange={(e) => updateSetting("maxCommentDepth", Number(e.target.value))} />
                        </FormGroup>
                      </div>
                    </CardBody>
                  </Card>

                  <div className="flex justify-end">
                    <Button variant="primary" size="sm" loading={settingsSaving} onClick={handleSaveSettings}>
                      <FloppyDisk size={16} className="mr-1.5" />
                      설정 저장
                    </Button>
                  </div>
                </>
              ) : null}
            </div>
          </TabsContent>

          {/* ─── Tab 3: 카테고리 ─── */}
          <TabsContent value="categories">
            <div className="space-y-4 mt-4">
              <div className="flex justify-end">
                <Button variant="primary" size="sm" onClick={() => openCatModal()}>
                  <Plus size={16} className="mr-1.5" />
                  카테고리 추가
                </Button>
              </div>

              {catLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : (
                <Card>
                  <CardBody className="!p-0">
                    {categories.length === 0 ? (
                      <div className="py-12 text-center text-sm text-gray-500">등록된 카테고리가 없습니다.</div>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                          <thead>
                            <tr className="border-b border-gray-200 bg-gray-50">
                              <th className="px-5 py-3 text-left font-medium text-gray-600">이름</th>
                              <th className="px-5 py-3 text-left font-medium text-gray-600">슬러그</th>
                              <th className="px-5 py-3 text-center font-medium text-gray-600">정렬</th>
                              <th className="px-5 py-3 text-center font-medium text-gray-600">활성</th>
                              <th className="px-5 py-3 text-right font-medium text-gray-600">작업</th>
                            </tr>
                          </thead>
                          <tbody>
                            {categories.map((cat) => (
                              <tr key={cat.id} className="border-b border-gray-100 hover:bg-gray-50">
                                <td className="px-5 py-3 font-medium">{cat.name}</td>
                                <td className="px-5 py-3 font-mono text-xs text-gray-500">{cat.slug}</td>
                                <td className="px-5 py-3 text-center text-gray-500">{cat.sortOrder}</td>
                                <td className="px-5 py-3 text-center">
                                  <Badge variant={cat.isActive ? "success" : "light"} pill>
                                    {cat.isActive ? "활성" : "비활성"}
                                  </Badge>
                                </td>
                                <td className="px-5 py-3">
                                  <div className="flex items-center justify-end gap-1">
                                    <Button variant="flat-primary" size="sm" onClick={() => openCatModal(cat)}>수정</Button>
                                    <Button variant="flat-danger" size="sm" iconOnly onClick={() => setCatDeleteTarget(cat)}>
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
              )}
            </div>
          </TabsContent>

          {/* ─── Tab 4: 게시글 ─── */}
          <TabsContent value="posts">
            <div className="space-y-4 mt-4">
              {postsLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : (
                <Card>
                  <CardBody className="!p-0">
                    {posts.length === 0 ? (
                      <div className="py-12 text-center text-sm text-gray-500">게시글이 없습니다.</div>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                          <thead>
                            <tr className="border-b border-gray-200 bg-gray-50">
                              <th className="px-5 py-3 text-left font-medium text-gray-600">제목</th>
                              <th className="px-5 py-3 text-left font-medium text-gray-600">작성자</th>
                              <th className="px-5 py-3 text-left font-medium text-gray-600">카테고리</th>
                              <th className="px-5 py-3 text-center font-medium text-gray-600">상태</th>
                              <th className="px-5 py-3 text-center font-medium text-gray-600">조회</th>
                              <th className="px-5 py-3 text-center font-medium text-gray-600">댓글</th>
                              <th className="px-5 py-3 text-left font-medium text-gray-600">작성일</th>
                              <th className="px-5 py-3 text-right font-medium text-gray-600">작업</th>
                            </tr>
                          </thead>
                          <tbody>
                            {posts.map((post) => (
                              <tr key={post.id} className="border-b border-gray-100 hover:bg-gray-50">
                                <td className="px-5 py-3 font-medium max-w-xs truncate">{post.title}</td>
                                <td className="px-5 py-3 text-gray-500 text-xs">{post.authorName}</td>
                                <td className="px-5 py-3 text-xs">
                                  {post.categoryName ? <Badge variant="light" pill>{post.categoryName}</Badge> : "-"}
                                </td>
                                <td className="px-5 py-3 text-center">
                                  <div className="flex items-center justify-center gap-1">
                                    {post.isNotice && <Badge variant="primary" pill>공지</Badge>}
                                    {post.isSecret && <Badge variant="warning" pill>비밀</Badge>}
                                    {post.isDraft && <Badge variant="light" pill>임시</Badge>}
                                  </div>
                                </td>
                                <td className="px-5 py-3 text-center text-gray-500">{post.viewCount}</td>
                                <td className="px-5 py-3 text-center text-gray-500">{post.commentCount}</td>
                                <td className="px-5 py-3 text-xs text-gray-500">{formatDate(post.createdAt)}</td>
                                <td className="px-5 py-3">
                                  <div className="flex items-center justify-end gap-1">
                                    <Button variant="flat-primary" size="sm" iconOnly title="공지 토글" onClick={() => toggleNotice(post.id)}>
                                      <Megaphone size={14} />
                                    </Button>
                                    <Button variant="flat-danger" size="sm" iconOnly title="삭제" onClick={() => deletePost(post.id)}>
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
              )}
              <Pagination page={postPage} totalPages={postTotalPages} onPageChange={(p) => loadPosts(p)} />
            </div>
          </TabsContent>

          {/* ─── Tab 5: 댓글 ─── */}
          <TabsContent value="comments">
            <div className="space-y-4 mt-4">
              {commentsLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : (
                <>
                  <Card>
                    <CardBody>
                      <div className="max-w-md space-y-1.5">
                        <Label>게시글 선택</Label>
                        <Select value={selectedPostId} onValueChange={(v) => { setSelectedPostId(v); loadComments(v); }}>
                          <SelectTrigger><SelectValue placeholder="게시글을 선택하세요" /></SelectTrigger>
                          <SelectContent>
                            {commentPostList.map((p) => (
                              <SelectItem key={p.id} value={p.id}>{p.title}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </CardBody>
                  </Card>

                  {selectedPostId && (
                    <Card>
                      <CardBody>
                        {comments.length === 0 ? (
                          <p className="py-4 text-center text-sm text-gray-500">댓글이 없습니다.</p>
                        ) : (
                          <div className="space-y-1">
                            {renderCommentTree(comments, 0, deleteComment, formatDateTime)}
                          </div>
                        )}
                      </CardBody>
                    </Card>
                  )}
                </>
              )}
            </div>
          </TabsContent>

          {/* ─── Tab 6: 권한 ─── */}
          <TabsContent value="permissions">
            <div className="space-y-5 mt-4">
              {permLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : (
                <>
                  {/* 그룹 권한 */}
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">그룹 권한</h3>
                      {availablePerms.length === 0 ? (
                        <p className="py-4 text-center text-sm text-gray-500">정의된 권한이 없습니다.</p>
                      ) : (
                        <div className="overflow-x-auto">
                          <table className="w-full text-sm">
                            <thead>
                              <tr className="border-b border-gray-200 bg-gray-50">
                                <th className="px-4 py-2.5 text-left font-medium text-gray-600">그룹</th>
                                {availablePerms.map((p) => (
                                  <th key={p.id} className="px-3 py-2.5 text-center font-medium text-gray-600 whitespace-nowrap" title={p.flatCode}>
                                    {p.name}
                                  </th>
                                ))}
                                <th className="px-3 py-2.5 text-center font-medium text-gray-600">저장</th>
                              </tr>
                            </thead>
                            <tbody>
                              {groupPerms.map((gp) => (
                                <tr key={gp.groupId} className="border-b border-gray-100 hover:bg-gray-50">
                                  <td className="px-4 py-2.5 font-medium text-gray-700 whitespace-nowrap">
                                    {gp.groupName} <span className="ml-1.5 text-xs text-gray-400">({gp.groupCode})</span>
                                  </td>
                                  {availablePerms.map((p) => (
                                    <td key={p.id} className="px-3 py-2.5 text-center">
                                      <input type="checkbox"
                                        checked={gp.grantedPermissionIds.includes(p.id)}
                                        onChange={() => toggleGroupPerm(gp.groupId, p.id)}
                                        className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer" />
                                    </td>
                                  ))}
                                  <td className="px-3 py-2.5 text-center">
                                    <Button variant="flat-primary" size="sm"
                                      loading={permSaving === `group:${gp.groupId}`}
                                      onClick={() => saveGroupPerm(gp)}>저장</Button>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </CardBody>
                  </Card>

                  {/* 사용자 권한 */}
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">개인 사용자 권한</h3>
                      <div className="flex gap-2 mb-4">
                        <Input type="text" placeholder="로그인 ID를 입력하세요" value={addUserLoginId}
                          onChange={(e) => setAddUserLoginId(e.target.value)}
                          onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddUser(); } }}
                          className="max-w-xs" />
                        <Button variant="flat-primary" size="sm" loading={addingUser} onClick={handleAddUser}>
                          <Plus size={14} className="mr-1" />추가
                        </Button>
                      </div>
                      {userPerms.length === 0 ? (
                        <p className="py-4 text-center text-sm text-gray-500">개인 사용자에게 직접 부여된 권한이 없습니다.</p>
                      ) : (
                        <div className="overflow-x-auto">
                          <table className="w-full text-sm">
                            <thead>
                              <tr className="border-b border-gray-200 bg-gray-50">
                                <th className="px-4 py-2.5 text-left font-medium text-gray-600">사용자</th>
                                {availablePerms.map((p) => (
                                  <th key={p.id} className="px-3 py-2.5 text-center font-medium text-gray-600 whitespace-nowrap" title={p.flatCode}>
                                    {p.name}
                                  </th>
                                ))}
                                <th className="px-3 py-2.5 text-center font-medium text-gray-600">작업</th>
                              </tr>
                            </thead>
                            <tbody>
                              {userPerms.map((up) => (
                                <tr key={up.userId} className="border-b border-gray-100 hover:bg-gray-50">
                                  <td className="px-4 py-2.5 font-medium text-gray-700 whitespace-nowrap">
                                    {up.username} <span className="ml-1.5 text-xs text-gray-400">({up.loginId})</span>
                                  </td>
                                  {availablePerms.map((p) => (
                                    <td key={p.id} className="px-3 py-2.5 text-center">
                                      <input type="checkbox"
                                        checked={up.grantedPermissionIds.includes(p.id)}
                                        onChange={() => toggleUserPerm(up.userId, p.id)}
                                        className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer" />
                                    </td>
                                  ))}
                                  <td className="px-3 py-2.5 text-center">
                                    <div className="flex items-center justify-center gap-1">
                                      <Button variant="flat-primary" size="sm"
                                        loading={permSaving === `user:${up.userId}`}
                                        onClick={() => saveUserPerm(up)}>저장</Button>
                                      <Button variant="flat-danger" size="sm" iconOnly title="삭제"
                                        onClick={() => removeUserPerm(up)}>
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
                </>
              )}
            </div>
          </TabsContent>

          {/* ─── Tab 7: 부관리자 ─── */}
          <TabsContent value="admins">
            <div className="space-y-4 mt-4">
              {adminsLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : (
                <>
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">부관리자 관리</h3>
                      <div className="flex gap-2 mb-4">
                        <Input type="text" placeholder="사용자 PK(UUID)를 입력하세요" value={addAdminId}
                          onChange={(e) => setAddAdminId(e.target.value)}
                          onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddAdmin(); } }}
                          className="max-w-sm" />
                        <Button variant="flat-primary" size="sm" loading={addingAdmin} onClick={handleAddAdmin}>
                          <Plus size={14} className="mr-1" />추가
                        </Button>
                      </div>
                      {admins.length === 0 ? (
                        <p className="py-4 text-center text-sm text-gray-500">등록된 부관리자가 없습니다.</p>
                      ) : (
                        <div className="overflow-x-auto">
                          <table className="w-full text-sm">
                            <thead>
                              <tr className="border-b border-gray-200 bg-gray-50">
                                <th className="px-5 py-3 text-left font-medium text-gray-600">사용자명</th>
                                <th className="px-5 py-3 text-left font-medium text-gray-600">사용자 ID</th>
                                <th className="px-5 py-3 text-left font-medium text-gray-600">등록일</th>
                                <th className="px-5 py-3 text-right font-medium text-gray-600">작업</th>
                              </tr>
                            </thead>
                            <tbody>
                              {admins.map((admin) => (
                                <tr key={admin.userId} className="border-b border-gray-100 hover:bg-gray-50">
                                  <td className="px-5 py-3 font-medium">{admin.username}</td>
                                  <td className="px-5 py-3 font-mono text-xs text-gray-500">{admin.userId}</td>
                                  <td className="px-5 py-3 text-xs text-gray-500">{formatDate(admin.createdAt)}</td>
                                  <td className="px-5 py-3">
                                    <div className="flex justify-end">
                                      <Button variant="flat-danger" size="sm" iconOnly title="삭제"
                                        onClick={() => removeAdmin(admin.userId)}>
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
                </>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* ─── 카테고리 생성/수정 모달 ─── */}
      <Modal
        open={catModalOpen}
        onClose={() => setCatModalOpen(false)}
        title={catEditTarget ? "카테고리 수정" : "카테고리 생성"}
        footer={
          <>
            <Button variant="light" onClick={() => setCatModalOpen(false)}>취소</Button>
            <Button variant="primary" loading={catSaving} onClick={() => {
              document.getElementById("cat-form")?.dispatchEvent(
                new Event("submit", { bubbles: true, cancelable: true })
              );
            }}>
              {catEditTarget ? "수정" : "생성"}
            </Button>
          </>
        }
      >
        <form id="cat-form" onSubmit={handleSaveCategory} className="space-y-4">
          <FormGroup label="이름" htmlFor="catName" required>
            <Input id="catName" type="text" value={catName} onChange={(e) => setCatName(e.target.value)} required />
          </FormGroup>
          <FormGroup label="슬러그" htmlFor="catSlug" required>
            <Input id="catSlug" type="text" placeholder="영소문자-숫자-하이픈" value={catSlug}
              onChange={(e) => setCatSlug(e.target.value)} required />
          </FormGroup>
          <FormGroup label="설명" htmlFor="catDesc">
            <Input id="catDesc" type="text" value={catDesc} onChange={(e) => setCatDesc(e.target.value)} />
          </FormGroup>
          <FormGroup label="정렬 순서" htmlFor="catSortOrder">
            <Input id="catSortOrder" type="number" value={catSort} onChange={(e) => setCatSort(Number(e.target.value))} />
          </FormGroup>
        </form>
      </Modal>

      {/* ─── 카테고리 삭제 확인 모달 ─── */}
      <Modal
        open={!!catDeleteTarget}
        onClose={() => setCatDeleteTarget(null)}
        title="카테고리 삭제"
        size="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setCatDeleteTarget(null)}>취소</Button>
            <Button variant="danger" onClick={handleDeleteCategory}>삭제</Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          <span className="font-semibold">{catDeleteTarget?.name}</span> 카테고리를 삭제하시겠습니까?
        </p>
      </Modal>
    </>
  );
}

/* ===========================
 * 댓글 트리 재귀 렌더링 헬퍼
 * =========================== */
function renderCommentTree(
  comments: CommentItem[],
  depth: number,
  onDelete: (id: string) => void,
  formatDateTime: (d: string) => string,
): React.ReactNode {
  return comments.map((c) => (
    <div key={c.id} style={{ marginLeft: depth * 24 }}
      className="border-l-2 border-gray-200 pl-3 py-2">
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 text-xs text-gray-500">
            <span className="font-medium text-gray-700">{c.authorName}</span>
            <span>{formatDateTime(c.createdAt)}</span>
            {c.isDeleted && <Badge variant="light" pill>삭제됨</Badge>}
          </div>
          <p className="mt-1 text-sm text-gray-700">
            {c.isDeleted ? <span className="italic text-gray-400">삭제된 댓글입니다.</span> : c.content}
          </p>
        </div>
        {!c.isDeleted && (
          <Button variant="flat-danger" size="sm" iconOnly title="삭제" onClick={() => onDelete(c.id)}>
            <Trash size={12} />
          </Button>
        )}
      </div>
      {c.children && c.children.length > 0 && renderCommentTree(c.children, depth + 1, onDelete, formatDateTime)}
    </div>
  ));
}
