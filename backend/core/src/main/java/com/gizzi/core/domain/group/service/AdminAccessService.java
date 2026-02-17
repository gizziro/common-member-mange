package com.gizzi.core.domain.group.service;

import com.gizzi.core.domain.group.entity.GroupEntity;
import com.gizzi.core.domain.group.repository.GroupMemberRepository;
import com.gizzi.core.domain.group.repository.GroupRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 그룹 소속 여부 확인 전용 서비스
// admin-api 접근 제어에서 사용: administrator 그룹 소속자만 허용
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminAccessService {

	// 관리자 그룹 코드 상수
	private static final String ADMIN_GROUP_CODE = "administrator";

	// 그룹 리포지토리 (관리자 그룹 PK 조회용)
	private final GroupRepository       groupRepository;

	// 그룹 멤버 리포지토리 (소속 여부 확인용)
	private final GroupMemberRepository groupMemberRepository;

	// 관리자 그룹 PK 캐시 (애플리케이션 시작 시 1회 조회)
	private String adminGroupId;

	// 애플리케이션 시작 시 administrator 그룹 PK를 캐시
	@PostConstruct
	public void init() {
		groupRepository.findByGroupCode(ADMIN_GROUP_CODE)
			.ifPresentOrElse(
				group -> {
					// 관리자 그룹 PK 캐시 저장
					adminGroupId = group.getId();
					log.info("관리자 그룹 PK 캐시 완료: groupId={}", adminGroupId);
				},
				() -> log.warn("관리자 그룹이 존재하지 않습니다: groupCode={} — 시드 데이터를 확인하세요", ADMIN_GROUP_CODE)
			);
	}

	// 해당 사용자가 administrator 그룹에 소속되어 있는지 확인
	// userPk: 사용자 PK (UUID)
	public boolean isAdminMember(String userPk) {
		// 관리자 그룹이 DB에 없는 경우 (시드 데이터 누락) → 모든 접근 차단
		if (adminGroupId == null) {
			log.error("관리자 그룹 PK가 캐시되지 않음 — 접근 차단");
			return false;
		}

		// DB 조회: 관리자 그룹 멤버십 존재 여부 (요청당 1회 쿼리)
		return groupMemberRepository.existsByGroupIdAndUserId(adminGroupId, userPk);
	}
}
