"use client";

import { useEffect, useState, useCallback, type FormEvent } from "react";
import { useParams } from "next/navigation";
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
import {
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
} from "@/components/ui/tabs";
import { Label } from "@/components/ui/label";
import { ArrowLeft, FloppyDisk, Plus, Trash } from "@phosphor-icons/react";
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
  moduleInstanceId: string | null;
  createdBy: string;
  createdAt: string;
  updatedBy: string | null;
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

/** 사용자별 권한 현황 */
interface UserPermission {
  userId: string;
  loginId: string;
  username: string;
  grantedPermissionIds: string[];
}

/** 페이지 편집 페이지 (탭: 콘텐츠 / 권한 설정) */
export default function PageEditPage() {
  const params = useParams();
  const pageId = params.id as string;

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [pageData, setPageData] = useState<PageDetail | null>(null);

  /* 콘텐츠 탭 폼 상태 */
  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [content, setContent] = useState("");
  const [contentType, setContentType] = useState("HTML");

  /* 권한 탭 상태 */
  const [permLoading, setPermLoading] = useState(false);
  const [permLoaded, setPermLoaded] = useState(false);
  const [availablePerms, setAvailablePerms] = useState<PermissionDef[]>([]);
  const [groupPerms, setGroupPerms] = useState<GroupPermission[]>([]);
  const [userPerms, setUserPerms] = useState<UserPermission[]>([]);
  const [permSaving, setPermSaving] = useState<string | null>(null); // 저장 중인 그룹/사용자 ID
  const [addUserLoginId, setAddUserLoginId] = useState("");
  const [addingUser, setAddingUser] = useState(false);

  /* 페이지 데이터 로드 */
  useEffect(() => {
    const loadPage = async () => {
      setLoading(true);
      try {
        const res = await apiGet<PageDetail>(`/pages/${pageId}`);
        if (res.success && res.data) {
          setPageData(res.data);
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

  /* 권한 데이터 로드 (탭 전환 시 최초 1회) */
  const loadPermissions = useCallback(async () => {
    if (permLoaded || !pageData?.moduleInstanceId) return;
    setPermLoading(true);
    try {
      // 권한 정의 + 그룹 권한 + 사용자 권한 병렬 조회
      const [defsRes, grpRes, userRes] = await Promise.all([
        apiGet<PermissionDef[]>("/pages/permission-definitions"),
        apiGet<GroupPermission[]>(`/pages/${pageId}/permissions`),
        apiGet<UserPermission[]>(`/pages/${pageId}/user-permissions`),
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
  }, [pageId, pageData, permLoaded]);

  /* 콘텐츠 저장 */
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

  /* ─── 그룹 권한 체크박스 토글 ─── */
  const toggleGroupPermission = (groupId: string, permId: string) => {
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

  /* 그룹별 권한 저장 */
  const saveGroupPermission = async (gp: GroupPermission) => {
    setPermSaving(`group:${gp.groupId}`);
    try {
      const res = await apiPut(`/pages/${pageId}/permissions`, {
        groupId: gp.groupId,
        permissionIds: gp.grantedPermissionIds,
      });
      if (res.success) {
        toast.success(`${gp.groupName} 그룹 권한이 저장되었습니다.`);
      } else {
        toast.error(res.error?.message ?? "권한 저장에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setPermSaving(null);
    }
  };

  /* ─── 사용자 권한 체크박스 토글 ─── */
  const toggleUserPermission = (userId: string, permId: string) => {
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

  /* 사용자별 권한 저장 */
  const saveUserPermission = async (up: UserPermission) => {
    setPermSaving(`user:${up.userId}`);
    try {
      const res = await apiPut(`/pages/${pageId}/user-permissions`, {
        userId: up.userId,
        permissionIds: up.grantedPermissionIds,
      });
      if (res.success) {
        toast.success(`${up.username} 사용자 권한이 저장되었습니다.`);
        // 빈 권한이면 목록에서 제거
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

  /* 사용자 추가 (로그인 ID로 조회 후 빈 권한으로 추가) */
  const handleAddUser = async () => {
    if (!addUserLoginId.trim()) return;
    setAddingUser(true);
    try {
      // 로그인 ID로 사용자 조회 (GET /users?search= 대신, 빈 권한으로 PUT 시도)
      // 먼저 사용자 PK를 알아야 하므로, 사용자 목록에서 찾기
      const res = await apiGet<{ content: Array<{ id: string; userId: string; username: string }> }>(
        `/users?page=0&size=1&sort=createdAt,desc`
      );

      // 간단한 방식: 서버에서 로그인 ID → PK 변환
      // 하지만 현재 API에는 로그인 ID 검색이 없으므로, 전체 사용자 중 일치하는 것을 찾는다
      // 더 나은 방식으로, 전체 사용자 중에서 찾기
      const allUsersRes = await apiGet<{ content: Array<{ id: string; userId: string; username: string }> }>(
        `/users?page=0&size=1000&sort=createdAt,desc`
      );

      if (!allUsersRes.success || !allUsersRes.data) {
        toast.error("사용자 목록을 조회할 수 없습니다.");
        return;
      }

      const foundUser = allUsersRes.data.content.find(
        (u) => u.userId === addUserLoginId.trim()
      );

      if (!foundUser) {
        toast.error(`"${addUserLoginId}" 로그인 ID의 사용자를 찾을 수 없습니다.`);
        return;
      }

      // 이미 추가되어 있는지 확인
      if (userPerms.some((up) => up.userId === foundUser.id)) {
        toast.error("이미 권한이 설정된 사용자입니다.");
        return;
      }

      // 빈 권한으로 로컬 목록에 추가 (아직 서버 저장은 아님)
      setUserPerms((prev) => [
        ...prev,
        {
          userId: foundUser.id,
          loginId: foundUser.userId,
          username: foundUser.username,
          grantedPermissionIds: [],
        },
      ]);
      setAddUserLoginId("");
      toast.success(`${foundUser.username} 사용자가 추가되었습니다. 권한을 선택 후 저장하세요.`);
    } catch {
      toast.error("사용자 조회에 실패했습니다.");
    } finally {
      setAddingUser(false);
    }
  };

  /* 사용자 권한 삭제 (전체 권한 해제) */
  const removeUserPermission = async (up: UserPermission) => {
    setPermSaving(`user:${up.userId}`);
    try {
      const res = await apiPut(`/pages/${pageId}/user-permissions`, {
        userId: up.userId,
        permissionIds: [],
      });
      if (res.success) {
        setUserPerms((prev) => prev.filter((u) => u.userId !== up.userId));
        toast.success(`${up.username} 사용자 권한이 삭제되었습니다.`);
      } else {
        toast.error(res.error?.message ?? "권한 삭제에 실패했습니다.");
      }
    } catch {
      toast.error("서버에 연결할 수 없습니다.");
    } finally {
      setPermSaving(null);
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

        <Tabs defaultValue="content" onValueChange={(value) => {
          // 권한 탭 최초 진입 시 데이터 로드
          if (value === "permissions") loadPermissions();
        }}>
          <TabsList variant="line">
            <TabsTrigger value="content">콘텐츠</TabsTrigger>
            <TabsTrigger value="permissions">권한 설정</TabsTrigger>
          </TabsList>

          {/* ─── 콘텐츠 탭 ─── */}
          <TabsContent value="content">
            <form id="edit-page-form" onSubmit={handleSave} className="space-y-5 mt-4">
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
          </TabsContent>

          {/* ─── 권한 설정 탭 ─── */}
          <TabsContent value="permissions">
            <div className="space-y-5 mt-4">
              {!pageData?.moduleInstanceId ? (
                <Alert variant="warning">
                  이 페이지는 모듈 인스턴스가 연결되어 있지 않아 권한 관리를 지원하지 않습니다.
                </Alert>
              ) : permLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
              ) : (
                <>
                  <p className="text-xs text-gray-500">
                    권한을 설정하지 않으면 공개 페이지로 모든 사용자가 접근할 수 있습니다.
                    특정 그룹/사용자에 권한을 부여하면 해당 대상만 접근할 수 있습니다.
                  </p>

                  {/* 그룹 권한 섹션 */}
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">그룹 권한</h3>
                      {availablePerms.length === 0 ? (
                        <p className="py-4 text-center text-sm text-gray-500">
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
                                <th className="px-3 py-2.5 text-center font-medium text-gray-600">저장</th>
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
                                        onChange={() => toggleGroupPermission(gp.groupId, perm.id)}
                                        className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                                      />
                                    </td>
                                  ))}
                                  <td className="px-3 py-2.5 text-center">
                                    <Button
                                      variant="flat-primary"
                                      size="sm"
                                      loading={permSaving === `group:${gp.groupId}`}
                                      onClick={() => saveGroupPermission(gp)}
                                    >
                                      저장
                                    </Button>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </CardBody>
                  </Card>

                  {/* 사용자 권한 섹션 */}
                  <Card>
                    <CardBody>
                      <h3 className="text-sm font-semibold text-gray-700 mb-4">개인 사용자 권한</h3>

                      {/* 사용자 추가 */}
                      <div className="flex gap-2 mb-4">
                        <Input
                          type="text"
                          placeholder="로그인 ID를 입력하세요"
                          value={addUserLoginId}
                          onChange={(e) => setAddUserLoginId(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") {
                              e.preventDefault();
                              handleAddUser();
                            }
                          }}
                          className="max-w-xs"
                        />
                        <Button
                          variant="flat-primary"
                          size="sm"
                          loading={addingUser}
                          onClick={handleAddUser}
                        >
                          <Plus size={14} className="mr-1" />
                          추가
                        </Button>
                      </div>

                      {userPerms.length === 0 ? (
                        <p className="py-4 text-center text-sm text-gray-500">
                          개인 사용자에게 직접 부여된 권한이 없습니다.
                        </p>
                      ) : (
                        <div className="overflow-x-auto">
                          <table className="w-full text-sm">
                            <thead>
                              <tr className="border-b border-gray-200 bg-gray-50">
                                <th className="px-4 py-2.5 text-left font-medium text-gray-600">사용자</th>
                                {availablePerms.map((perm) => (
                                  <th
                                    key={perm.id}
                                    className="px-3 py-2.5 text-center font-medium text-gray-600 whitespace-nowrap"
                                    title={perm.flatCode}
                                  >
                                    {perm.name}
                                  </th>
                                ))}
                                <th className="px-3 py-2.5 text-center font-medium text-gray-600">작업</th>
                              </tr>
                            </thead>
                            <tbody>
                              {userPerms.map((up) => (
                                <tr key={up.userId} className="border-b border-gray-100 hover:bg-gray-50">
                                  <td className="px-4 py-2.5 font-medium text-gray-700 whitespace-nowrap">
                                    {up.username}
                                    <span className="ml-1.5 text-xs text-gray-400">({up.loginId})</span>
                                  </td>
                                  {availablePerms.map((perm) => (
                                    <td key={perm.id} className="px-3 py-2.5 text-center">
                                      <input
                                        type="checkbox"
                                        checked={up.grantedPermissionIds.includes(perm.id)}
                                        onChange={() => toggleUserPermission(up.userId, perm.id)}
                                        className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                                      />
                                    </td>
                                  ))}
                                  <td className="px-3 py-2.5 text-center">
                                    <div className="flex items-center justify-center gap-1">
                                      <Button
                                        variant="flat-primary"
                                        size="sm"
                                        loading={permSaving === `user:${up.userId}`}
                                        onClick={() => saveUserPermission(up)}
                                      >
                                        저장
                                      </Button>
                                      <Button
                                        variant="flat-danger"
                                        size="sm"
                                        iconOnly
                                        title="권한 삭제"
                                        onClick={() => removeUserPermission(up)}
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
                </>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </>
  );
}
