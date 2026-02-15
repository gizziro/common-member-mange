package com.gizzi.core.domain.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 그룹 멤버 엔티티 (tb_group_members 테이블 매핑)
// 그룹과 사용자의 다대다 관계를 복합키로 관리한다
@Entity
@Table(name = "tb_group_members")
@IdClass(GroupMemberId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMemberEntity {

	// 그룹 PK (복합키 구성 요소)
	@Id
	@Column(name = "group_id", length = 36)
	private String groupId;

	// 사용자 PK (복합키 구성 요소)
	@Id
	@Column(name = "user_id", length = 36)
	private String userId;

	// 가입 일시
	@Column(name = "joined_at", nullable = false)
	private LocalDateTime joinedAt;

	// 저장 전 가입 일시 자동 설정
	@PrePersist
	private void setJoinedAt() {
		// 가입 일시가 없을 때만 현재 시각으로 설정
		if (this.joinedAt == null) {
			this.joinedAt = LocalDateTime.now();
		}
	}

	// 그룹 멤버 생성 팩토리 메서드
	public static GroupMemberEntity create(String groupId, String userId) {
		GroupMemberEntity member = new GroupMemberEntity();
		member.groupId           = groupId;
		member.userId            = userId;
		return member;
	}
}
