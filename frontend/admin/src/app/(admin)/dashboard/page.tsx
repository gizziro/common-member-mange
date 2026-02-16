"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { apiGet } from "@/lib/api";
import { PageHeader } from "@/components/layout/PageHeader";
import { Card, CardHeader, CardBody } from "@/components/ui";
import { Users, UsersThree, ShieldCheck, Pulse } from "@phosphor-icons/react";

/** 통계 데이터 타입 */
interface Stats {
  totalUsers: number;
  totalGroups: number;
}

/** 대시보드 페이지 */
export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState<Stats>({ totalUsers: 0, totalGroups: 0 });

  /* 통계 데이터 로드 (그룹 목록에서 간접 조회) */
  useEffect(() => {
    async function loadStats() {
      try {
        const groupRes = await apiGet<{ content: unknown[]; totalElements: number }>("/groups");
        if (groupRes.success && groupRes.data) {
          setStats((prev) => ({
            ...prev,
            totalGroups: groupRes.data!.totalElements ?? groupRes.data!.content?.length ?? 0,
          }));
        }
      } catch {
        /* 통계 로드 실패해도 무시 */
      }
    }
    loadStats();
  }, []);

  return (
    <>
      <PageHeader
        title="대시보드"
        subtitle={`${user?.username}님, 환영합니다.`}
      />

      <div className="p-5">
        {/* 통계 카드 그리드 */}
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {/* 사용자 수 */}
          <StatCard
            icon={<Users size={24} />}
            iconBg="bg-primary/10 text-primary"
            label="전체 사용자"
            value="-"
          />

          {/* 그룹 수 */}
          <StatCard
            icon={<UsersThree size={24} />}
            iconBg="bg-success/10 text-success"
            label="전체 그룹"
            value={stats.totalGroups.toString()}
          />

          {/* 활성 세션 */}
          <StatCard
            icon={<Pulse size={24} />}
            iconBg="bg-info/10 text-info"
            label="활성 세션"
            value="-"
          />

          {/* 시스템 상태 */}
          <StatCard
            icon={<ShieldCheck size={24} />}
            iconBg="bg-warning/10 text-warning"
            label="시스템 상태"
            value="정상"
          />
        </div>

        {/* 최근 활동 카드 */}
        <div className="mt-5">
          <Card>
            <CardHeader title="최근 활동" />
            <CardBody>
              <p className="text-sm text-gray-500">
                아직 기록된 활동이 없습니다. 감사 로깅 기능이 구현되면 여기에 최근 활동이 표시됩니다.
              </p>
            </CardBody>
          </Card>
        </div>
      </div>
    </>
  );
}

/* ===========================
 * StatCard — 통계 카드 내부 컴포넌트
 * =========================== */

function StatCard({
  icon,
  iconBg,
  label,
  value,
}: {
  icon: React.ReactNode;
  iconBg: string;
  label: string;
  value: string;
}) {
  return (
    <Card>
      <CardBody>
        <div className="flex items-center gap-4">
          <div className={`inline-flex items-center justify-center w-12 h-12 rounded-lg ${iconBg}`}>
            {icon}
          </div>
          <div>
            <p className="text-sm text-gray-500">{label}</p>
            <p className="text-2xl font-semibold text-gray-900">{value}</p>
          </div>
        </div>
      </CardBody>
    </Card>
  );
}
