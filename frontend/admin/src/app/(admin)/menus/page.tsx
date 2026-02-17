"use client";

import { useEffect, useState, useCallback, type FormEvent } from "react";
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
} from "@/components/ui";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Plus, Pencil, Trash, ArrowUp, ArrowDown, Eye, EyeSlash, FileText } from "@phosphor-icons/react";
import { toast } from "sonner";

/* ===========================
 * 타입 정의
 * =========================== */

/** 메뉴 응답 */
interface Menu {
  id: string;
  name: string;
  icon: string | null;
  menuType: string;
  url: string | null;
  moduleInstanceId: string | null;
  customUrl: string | null;
  requiredRole: string | null;
  sortOrder: number;
  isVisible: boolean;
  children: Menu[] | null;
}

/** 등록된 모듈 */
interface ModuleDef {
  code: string;
  name: string;
  slug: string;
  type: string; // "SINGLE" | "MULTI"
}

/** 모듈 인스턴스 */
interface ModuleInstance {
  instanceId: string;
  moduleCode: string;
  instanceName: string;
  slug: string;
}

/** 모듈 권한 정의 */
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

/** 페이지 목록 항목 (LINK 페이지 선택 도우미용) */
interface PageItem {
  id: string;
  slug: string;
  title: string;
  isPublished: boolean;
}

/** 메뉴 관리 페이지 */
export default function MenusPage() {
  const [menus, setMenus] = useState<Menu[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /* 생성/수정 모달 상태 */
  const [editOpen, setEditOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [editName, setEditName] = useState("");
  const [editIcon, setEditIcon] = useState("");
  const [editType, setEditType] = useState("MODULE");
  const [editInstanceId, setEditInstanceId] = useState("");
  const [editCustomUrl, setEditCustomUrl] = useState("");
  const [editRequiredRole, setEditRequiredRole] = useState("");
  const [editParentId, setEditParentId] = useState("");
  const [editSortOrder, setEditSortOrder] = useState("0");
  const [editError, setEditError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  /* 모듈/인스턴스 캐스케이드 선택 상태 */
  const [modules, setModules] = useState<ModuleDef[]>([]);
  const [selectedModuleCode, setSelectedModuleCode] = useState("");
  const [moduleInstances, setModuleInstances] = useState<ModuleInstance[]>([]);

  /* 페이지 선택 도우미 (LINK 타입용) */
  const [pages, setPages] = useState<PageItem[]>([]);
  const [pageSelectOpen, setPageSelectOpen] = useState(false);

  /* 권한 매트릭스 상태 */
  const [availablePerms, setAvailablePerms] = useState<PermissionDef[]>([]);
  const [groupPerms, setGroupPerms] = useState<GroupPermission[]>([]);
  const [permSaving, setPermSaving] = useState(false);

  /* 삭제 확인 */
  const [deleteTarget, setDeleteTarget] = useState<Menu | null>(null);
  const [deleting, setDeleting] = useState(false);

  /* 메뉴 트리 로드 */
  const loadMenus = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiGet<Menu[]>("/menus");
      if (res.success && res.data) {
        setMenus(res.data);
      } else {
        setError(res.error?.message ?? "메뉴를 불러올 수 없습니다.");
      }
    } catch {
      setError("서버에 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  /* 모듈 목록 로드 (페이지 마운트 시 1회) */
  const loadModules = useCallback(async () => {
    try {
      const res = await apiGet<ModuleDef[]>("/menus/modules");
      if (res.success && res.data) {
        setModules(res.data);
      }
    } catch {
      /* 모듈 목록 실패 — 무시 */
    }
  }, []);

  useEffect(() => {
    loadMenus();
    loadModules();
  }, [loadMenus, loadModules]);

  /* 모듈 선택 시 인스턴스 목록 로드 */
  const handleModuleChange = useCallback(async (code: string) => {
    setSelectedModuleCode(code);
    setEditInstanceId("");
    setModuleInstances([]);

    if (!code) return;

    try {
      const res = await apiGet<ModuleInstance[]>(`/menus/modules/${code}/instances`);
      if (res.success && res.data) {
        setModuleInstances(res.data);
        // SINGLE 모듈은 인스턴스가 1개 — 자동 선택
        const mod = modules.find((m) => m.code === code);
        if (mod?.type === "SINGLE" && res.data.length === 1) {
          setEditInstanceId(res.data[0].instanceId);
        }
      }
    } catch {
      /* 인스턴스 목록 로드 실패 */
    }
  }, [modules]);

  /* instanceId → 역방향으로 모듈 코드 찾기 (수정 모달 열 때 사용) */
  const resolveModuleFromInstance = useCallback(async (instanceId: string) => {
    // 모든 모듈의 인스턴스를 순회하여 매칭
    for (const mod of modules) {
      try {
        const res = await apiGet<ModuleInstance[]>(`/menus/modules/${mod.code}/instances`);
        if (res.success && res.data) {
          const found = res.data.find((inst) => inst.instanceId === instanceId);
          if (found) {
            setSelectedModuleCode(mod.code);
            setModuleInstances(res.data);
            setEditInstanceId(instanceId);
            return;
          }
        }
      } catch {
        /* 무시 */
      }
    }
  }, [modules]);

  /* 권한 데이터 로드 (moduleCode + instanceId 변경 시) */
  const loadPermissions = useCallback(async (moduleCode: string, instanceId: string) => {
    if (!moduleCode || !instanceId) {
      setAvailablePerms([]);
      setGroupPerms([]);
      return;
    }

    try {
      // 권한 정의 + 그룹별 현황 병렬 로드
      const [permsRes, groupRes] = await Promise.all([
        apiGet<PermissionDef[]>(`/menus/modules/${moduleCode}/permissions`),
        apiGet<GroupPermission[]>(`/menus/instances/${instanceId}/permissions`),
      ]);

      if (permsRes.success && permsRes.data) {
        setAvailablePerms(permsRes.data);
      }
      if (groupRes.success && groupRes.data) {
        setGroupPerms(groupRes.data);
      }
    } catch {
      /* 권한 로드 실패 */
    }
  }, []);

  /* 인스턴스 변경 시 권한 데이터 로드 */
  useEffect(() => {
    if (editOpen && editType === "MODULE" && selectedModuleCode && editInstanceId) {
      loadPermissions(selectedModuleCode, editInstanceId);
    } else {
      setAvailablePerms([]);
      setGroupPerms([]);
    }
  }, [editOpen, editType, selectedModuleCode, editInstanceId, loadPermissions]);

  /* 페이지 목록 로드 (LINK 타입 페이지 선택 도우미용) */
  const loadPages = useCallback(async () => {
    try {
      const res = await apiGet<PageItem[]>("/pages");
      if (res.success && res.data) {
        setPages(res.data);
      }
    } catch {
      /* 페이지 목록 실패 */
    }
  }, []);

  /* 생성 모달 열기 */
  const openCreate = (parentId?: string) => {
    setEditId(null);
    setEditName("");
    setEditIcon("");
    setEditType("MODULE");
    setEditInstanceId("");
    setEditCustomUrl("");
    setEditRequiredRole("");
    setEditParentId(parentId ?? "");
    setEditSortOrder("0");
    setEditError(null);
    setSelectedModuleCode("");
    setModuleInstances([]);
    setAvailablePerms([]);
    setGroupPerms([]);
    setEditOpen(true);
  };

  /* 수정 모달 열기 */
  const openEdit = async (menu: Menu) => {
    setEditId(menu.id);
    setEditName(menu.name);
    setEditIcon(menu.icon ?? "");
    setEditType(menu.menuType);
    setEditInstanceId(menu.moduleInstanceId ?? "");
    setEditCustomUrl(menu.customUrl ?? "");
    setEditRequiredRole(menu.requiredRole ?? "");
    setEditParentId("");
    setEditSortOrder(String(menu.sortOrder));
    setEditError(null);
    setSelectedModuleCode("");
    setModuleInstances([]);
    setAvailablePerms([]);
    setGroupPerms([]);
    setEditOpen(true);

    // MODULE 타입이면 역방향 조회하여 모듈/인스턴스 셀렉트 초기값 설정
    if (menu.menuType === "MODULE" && menu.moduleInstanceId) {
      await resolveModuleFromInstance(menu.moduleInstanceId);
    }
  };

  /* 저장 (생성 또는 수정) */
  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    setEditError(null);
    setSaving(true);

    try {
      const body = {
        name: editName,
        icon: editIcon || null,
        menuType: editType,
        moduleInstanceId: editType === "MODULE" ? editInstanceId : null,
        customUrl: editType === "LINK" ? editCustomUrl : null,
        requiredRole: editType === "LINK" ? (editRequiredRole || null) : null,
        parentId: editParentId || null,
        sortOrder: parseInt(editSortOrder) || 0,
      };

      let res: ApiResponse;
      if (editId) {
        res = await apiPut(`/menus/${editId}`, body);
      } else {
        res = await apiPost("/menus", body);
      }

      if (res.success) {
        setEditOpen(false);
        toast.success(editId ? "메뉴가 수정되었습니다." : "메뉴가 생성되었습니다.");
        await loadMenus();
      } else {
        setEditError(res.error?.message ?? "저장에 실패했습니다.");
      }
    } catch {
      setEditError("서버에 연결할 수 없습니다.");
    } finally {
      setSaving(false);
    }
  };

  /* 삭제 */
  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      const res = await apiDelete(`/menus/${deleteTarget.id}`);
      if (res.success) {
        setDeleteTarget(null);
        toast.success("메뉴가 삭제되었습니다.");
        await loadMenus();
      }
    } catch {
      /* 삭제 실패 */
    } finally {
      setDeleting(false);
    }
  };

  /* 가시성 토글 */
  const toggleVisibility = async (id: string) => {
    await apiPatch(`/menus/${id}/toggle`);
    await loadMenus();
  };

  /* 정렬 변경 */
  const changeOrder = async (id: string, sortOrder: number, delta: number) => {
    await apiPatch(`/menus/${id}/order?sortOrder=${sortOrder + delta}`);
    await loadMenus();
  };

  /* 권한 체크박스 토글 */
  const togglePermission = (groupId: string, permId: string) => {
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

  /* 권한 저장 (전체 그룹 일괄) */
  const savePermissions = async () => {
    if (!editInstanceId) return;
    setPermSaving(true);
    try {
      // 모든 그룹에 대해 순차 저장
      for (const gp of groupPerms) {
        await apiPut(`/menus/instances/${editInstanceId}/permissions`, {
          groupId: gp.groupId,
          permissionIds: gp.grantedPermissionIds,
        });
      }
      toast.success("권한이 저장되었습니다.");
    } catch {
      toast.error("권한 저장에 실패했습니다.");
    } finally {
      setPermSaving(false);
    }
  };

  /* 메뉴 트리를 플랫 행으로 변환 (들여쓰기 표시) */
  const flattenMenus = (items: Menu[], depth = 0): { menu: Menu; depth: number }[] => {
    const result: { menu: Menu; depth: number }[] = [];
    for (const item of items) {
      result.push({ menu: item, depth });
      if (item.children) {
        result.push(...flattenMenus(item.children, depth + 1));
      }
    }
    return result;
  };

  /* 메뉴 유형 뱃지 색상 */
  const typeBadge = (type: string) => {
    switch (type) {
      case "MODULE": return <Badge variant="primary" pill>모듈</Badge>;
      case "LINK": return <Badge variant="info" pill>링크</Badge>;
      case "SEPARATOR": return <Badge variant="light" pill>구분선</Badge>;
      default: return <Badge variant="light" pill>{type}</Badge>;
    }
  };

  /* 부모 메뉴 선택 옵션 (플랫 목록) */
  const flatMenuOptions = flattenMenus(menus);

  /* 선택된 모듈이 SINGLE인지 확인 */
  const selectedModule = modules.find((m) => m.code === selectedModuleCode);
  const isSingleModule = selectedModule?.type === "SINGLE";

  return (
    <>
      <PageHeader
        title="메뉴 관리"
        subtitle="사이트 네비게이션 메뉴를 관리합니다"
        actions={
          <Button variant="primary" size="sm" onClick={() => openCreate()}>
            <Plus size={16} className="mr-1.5" />
            메뉴 추가
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
            ) : menus.length === 0 ? (
              <div className="py-12 text-center text-sm text-gray-500">
                등록된 메뉴가 없습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="px-5 py-3 text-left font-medium text-gray-600">메뉴명</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">유형</th>
                      <th className="px-5 py-3 text-left font-medium text-gray-600">URL</th>
                      <th className="px-5 py-3 text-center font-medium text-gray-600">순서</th>
                      <th className="px-5 py-3 text-center font-medium text-gray-600">상태</th>
                      <th className="px-5 py-3 text-right font-medium text-gray-600">작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {flattenMenus(menus).map(({ menu, depth }) => (
                      <tr key={menu.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="px-5 py-3">
                          <span style={{ paddingLeft: `${depth * 24}px` }} className="inline-flex items-center gap-2">
                            {depth > 0 && <span className="text-gray-300">└</span>}
                            {menu.icon && <span className="text-gray-400">{menu.icon}</span>}
                            <span className="font-medium">{menu.name}</span>
                          </span>
                        </td>
                        <td className="px-5 py-3">{typeBadge(menu.menuType)}</td>
                        <td className="px-5 py-3 text-xs font-mono text-gray-500">
                          {menu.url ?? "-"}
                        </td>
                        <td className="px-5 py-3 text-center">
                          <div className="inline-flex items-center gap-1">
                            <button
                              onClick={() => changeOrder(menu.id, menu.sortOrder, -1)}
                              className="p-0.5 text-gray-400 hover:text-gray-700"
                            >
                              <ArrowUp size={14} />
                            </button>
                            <span className="text-xs text-gray-500 min-w-[20px] text-center">{menu.sortOrder}</span>
                            <button
                              onClick={() => changeOrder(menu.id, menu.sortOrder, 1)}
                              className="p-0.5 text-gray-400 hover:text-gray-700"
                            >
                              <ArrowDown size={14} />
                            </button>
                          </div>
                        </td>
                        <td className="px-5 py-3 text-center">
                          <button
                            onClick={() => toggleVisibility(menu.id)}
                            className={`p-1 rounded ${menu.isVisible ? "text-green-600" : "text-gray-400"}`}
                            title={menu.isVisible ? "표시 중" : "숨김"}
                          >
                            {menu.isVisible ? <Eye size={16} /> : <EyeSlash size={16} />}
                          </button>
                        </td>
                        <td className="px-5 py-3">
                          <div className="flex items-center justify-end gap-1">
                            <Button variant="flat-primary" size="sm" iconOnly title="수정"
                              onClick={() => openEdit(menu)}>
                              <Pencil size={14} />
                            </Button>
                            <Button variant="flat-danger" size="sm" iconOnly title="삭제"
                              onClick={() => setDeleteTarget(menu)}>
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

      {/* 생성/수정 모달 */}
      <Modal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        title={editId ? "메뉴 수정" : "메뉴 추가"}
        size="xl"
        footer={
          <>
            <Button variant="light" onClick={() => setEditOpen(false)}>취소</Button>
            <Button variant="primary" loading={saving} onClick={() => {
              document.getElementById("edit-menu-form")?.dispatchEvent(
                new Event("submit", { bubbles: true, cancelable: true })
              );
            }}>
              {editId ? "수정" : "생성"}
            </Button>
          </>
        }
      >
        {editError && (
          <Alert variant="danger" dismissible className="mb-4">{editError}</Alert>
        )}
        <form id="edit-menu-form" onSubmit={handleSave} className="space-y-4">
          <FormGroup label="메뉴 이름" htmlFor="menuName" required>
            <Input
              id="menuName"
              type="text"
              placeholder="메뉴 표시 이름"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              required
            />
          </FormGroup>

          <FormGroup label="아이콘" htmlFor="menuIcon">
            <Input
              id="menuIcon"
              type="text"
              placeholder="아이콘 식별자 (선택)"
              value={editIcon}
              onChange={(e) => setEditIcon(e.target.value)}
            />
          </FormGroup>

          <div className="space-y-1.5">
            <Label>메뉴 유형</Label>
            <Select value={editType} onValueChange={(v) => {
              setEditType(v);
              // 유형 변경 시 관련 필드 초기화
              if (v !== "MODULE") {
                setSelectedModuleCode("");
                setModuleInstances([]);
                setEditInstanceId("");
              }
            }}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="MODULE">모듈 연결</SelectItem>
                <SelectItem value="LINK">커스텀 링크</SelectItem>
                <SelectItem value="SEPARATOR">구분선</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* MODULE 타입: 모듈 → 인스턴스 캐스케이드 선택 */}
          {editType === "MODULE" && (
            <>
              <div className="space-y-1.5">
                <Label>모듈 선택 <span className="text-red-500">*</span></Label>
                <Select
                  value={selectedModuleCode || "__none__"}
                  onValueChange={(v) => handleModuleChange(v === "__none__" ? "" : v)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="모듈을 선택하세요" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="__none__" disabled>모듈을 선택하세요</SelectItem>
                    {modules.map((mod) => (
                      <SelectItem key={mod.code} value={mod.code}>
                        {mod.name} ({mod.type === "SINGLE" ? "단일" : "다중"})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* MULTI 모듈일 때만 인스턴스 선택 표시 */}
              {selectedModuleCode && !isSingleModule && (
                <div className="space-y-1.5">
                  <Label>인스턴스 선택 <span className="text-red-500">*</span></Label>
                  <Select
                    value={editInstanceId || "__none__"}
                    onValueChange={(v) => setEditInstanceId(v === "__none__" ? "" : v)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="인스턴스를 선택하세요" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="__none__" disabled>인스턴스를 선택하세요</SelectItem>
                      {moduleInstances.map((inst) => (
                        <SelectItem key={inst.instanceId} value={inst.instanceId}>
                          {inst.instanceName} (/{inst.slug})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}

              {/* SINGLE 모듈 선택 시 자동 연결 안내 */}
              {selectedModuleCode && isSingleModule && editInstanceId && (
                <p className="text-xs text-gray-500">
                  단일 모듈이므로 시스템 인스턴스가 자동 선택되었습니다.
                </p>
              )}
            </>
          )}

          {/* LINK 타입: URL 입력 + 페이지 선택 도우미 */}
          {editType === "LINK" && (
            <>
              <div className="space-y-1.5">
                <Label htmlFor="customUrl">URL <span className="text-red-500">*</span></Label>
                <div className="flex gap-2">
                  <Input
                    id="customUrl"
                    type="text"
                    placeholder="https://example.com 또는 /page/about"
                    value={editCustomUrl}
                    onChange={(e) => setEditCustomUrl(e.target.value)}
                    required
                    className="flex-1"
                  />
                  <Button
                    type="button"
                    variant="light"
                    size="sm"
                    onClick={() => {
                      loadPages();
                      setPageSelectOpen(true);
                    }}
                    title="페이지에서 선택"
                  >
                    <FileText size={16} className="mr-1" />
                    페이지 선택
                  </Button>
                </div>
              </div>
              <FormGroup label="필요 역할" htmlFor="requiredRole">
                <Input
                  id="requiredRole"
                  type="text"
                  placeholder="비워두면 전체 공개"
                  value={editRequiredRole}
                  onChange={(e) => setEditRequiredRole(e.target.value)}
                />
              </FormGroup>
            </>
          )}

          {!editId && (
            <div className="space-y-1.5">
              <Label>상위 메뉴</Label>
              <Select value={editParentId || "__none__"} onValueChange={(v) => setEditParentId(v === "__none__" ? "" : v)}>
                <SelectTrigger>
                  <SelectValue placeholder="최상위" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__none__">최상위 (없음)</SelectItem>
                  {flatMenuOptions.map(({ menu, depth }) => (
                    <SelectItem key={menu.id} value={menu.id}>
                      {"─".repeat(depth)} {menu.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <FormGroup label="정렬 순서" htmlFor="sortOrder">
            <Input
              id="sortOrder"
              type="number"
              value={editSortOrder}
              onChange={(e) => setEditSortOrder(e.target.value)}
            />
          </FormGroup>
        </form>

        {/* ─── 권한 설정 패널 (MODULE 타입 + 인스턴스 선택됨) ─── */}
        {editType === "MODULE" && editInstanceId && availablePerms.length > 0 && (
          <div className="mt-6 border-t border-gray-200 pt-4">
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-sm font-semibold text-gray-900">권한 설정</h4>
              <Button
                type="button"
                variant="primary"
                size="sm"
                loading={permSaving}
                onClick={savePermissions}
              >
                권한 저장
              </Button>
            </div>

            {groupPerms.length === 0 ? (
              <p className="text-xs text-gray-500">그룹 데이터를 불러오는 중...</p>
            ) : (
              <div className="overflow-x-auto rounded border border-gray-200">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-gray-50">
                      <th className="px-3 py-2 text-left font-medium text-gray-600 sticky left-0 bg-gray-50 min-w-[120px]">
                        그룹
                      </th>
                      {availablePerms.map((perm) => (
                        <th key={perm.id} className="px-3 py-2 text-center font-medium text-gray-600 whitespace-nowrap">
                          {perm.name}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {groupPerms.map((gp) => (
                      <tr key={gp.groupId} className="border-t border-gray-100 hover:bg-gray-50">
                        <td className="px-3 py-2 font-medium text-gray-700 sticky left-0 bg-white">
                          {gp.groupName}
                          <span className="ml-1 text-gray-400">({gp.groupCode})</span>
                        </td>
                        {availablePerms.map((perm) => (
                          <td key={perm.id} className="px-3 py-2 text-center">
                            <input
                              type="checkbox"
                              className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary/50"
                              checked={gp.grantedPermissionIds.includes(perm.id)}
                              onChange={() => togglePermission(gp.groupId, perm.id)}
                            />
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* 페이지 선택 모달 (LINK 타입 도우미) */}
      <Modal
        open={pageSelectOpen}
        onClose={() => setPageSelectOpen(false)}
        title="페이지 선택"
        size="md"
      >
        {pages.length === 0 ? (
          <p className="py-4 text-center text-sm text-gray-500">등록된 페이지가 없습니다.</p>
        ) : (
          <div className="max-h-64 overflow-y-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="px-3 py-2 text-left font-medium text-gray-600">제목</th>
                  <th className="px-3 py-2 text-left font-medium text-gray-600">슬러그</th>
                  <th className="px-3 py-2 text-center font-medium text-gray-600">상태</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-600">선택</th>
                </tr>
              </thead>
              <tbody>
                {pages.map((page) => (
                  <tr key={page.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="px-3 py-2">{page.title}</td>
                    <td className="px-3 py-2 text-xs font-mono text-gray-500">/page/{page.slug}</td>
                    <td className="px-3 py-2 text-center">
                      <Badge variant={page.isPublished ? "success" : "light"} pill>
                        {page.isPublished ? "공개" : "비공개"}
                      </Badge>
                    </td>
                    <td className="px-3 py-2 text-right">
                      <Button
                        variant="flat-primary"
                        size="sm"
                        onClick={() => {
                          setEditCustomUrl(`/page/${page.slug}`);
                          setPageSelectOpen(false);
                        }}
                      >
                        선택
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Modal>

      {/* 삭제 확인 모달 */}
      <Modal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="메뉴 삭제"
        size="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setDeleteTarget(null)}>취소</Button>
            <Button variant="danger" loading={deleting} onClick={handleDelete}>삭제</Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          <span className="font-semibold">{deleteTarget?.name}</span> 메뉴를 삭제하시겠습니까?
          {deleteTarget?.children && deleteTarget.children.length > 0 && (
            <span className="block mt-1 text-red-500">하위 메뉴도 함께 삭제됩니다.</span>
          )}
        </p>
      </Modal>
    </>
  );
}
