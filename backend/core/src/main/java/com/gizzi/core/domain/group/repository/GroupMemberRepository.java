package com.gizzi.core.domain.group.repository;

import com.gizzi.core.domain.group.entity.GroupMemberEntity;
import com.gizzi.core.domain.group.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 그룹 멤버 리포지토리 (tb_group_members 테이블 접근)
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, GroupMemberId> {

	// 특정 그룹의 멤버 목록 조회
	List<GroupMemberEntity> findByGroupId(String groupId);

	// 특정 사용자의 소속 그룹 멤버십 목록 조회
	List<GroupMemberEntity> findByUserId(String userId);

	// 특정 그룹에 특정 사용자가 소속되어 있는지 확인
	boolean existsByGroupIdAndUserId(String groupId, String userId);

	// 특정 그룹에서 특정 사용자 멤버십 삭제
	void deleteByGroupIdAndUserId(String groupId, String userId);

	// 특정 그룹의 멤버 수 조회
	long countByGroupId(String groupId);
}
