package com.gizzi.core.domain.group.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.GroupErrorCode;
import com.gizzi.core.common.exception.UserErrorCode;
import com.gizzi.core.domain.group.dto.CreateGroupRequestDto;
import com.gizzi.core.domain.group.dto.GroupMemberResponseDto;
import com.gizzi.core.domain.group.dto.GroupResponseDto;
import com.gizzi.core.domain.group.dto.UpdateGroupRequestDto;
import com.gizzi.core.domain.group.entity.GroupEntity;
import com.gizzi.core.domain.group.entity.GroupMemberEntity;
import com.gizzi.core.domain.group.repository.GroupMemberRepository;
import com.gizzi.core.domain.group.repository.GroupRepository;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 그룹 관련 비즈니스 로직을 처리하는 서비스
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupService {

	// 그룹 리포지토리
	private final GroupRepository       groupRepository;

	// 그룹 멤버 리포지토리
	private final GroupMemberRepository  groupMemberRepository;

	// 사용자 리포지토리
	private final UserRepository         userRepository;

	// 기본 그룹 코드 (회원가입 시 자동 배정)
	private static final String DEFAULT_GROUP_CODE = "user";

	// 그룹 생성 + 소유자 멤버 자동 등록
	@Transactional
	public GroupResponseDto createGroup(CreateGroupRequestDto request, String ownerUserId) {
		// 그룹 코드 중복 검증
		if (groupRepository.existsByGroupCode(request.getGroupCode())) {
			throw new BusinessException(GroupErrorCode.DUPLICATE_GROUP_CODE);
		}

		// 그룹 이름 중복 검증
		if (groupRepository.existsByName(request.getName())) {
			throw new BusinessException(GroupErrorCode.DUPLICATE_GROUP_NAME);
		}

		// 소유자 사용자 존재 검증
		userRepository.findById(ownerUserId)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 사용자 그룹 엔티티 생성
		GroupEntity group = GroupEntity.createUserGroup(
			request.getGroupCode(),
			request.getName(),
			request.getDescription(),
			ownerUserId
		);

		// DB 저장
		GroupEntity savedGroup = groupRepository.save(group);

		// 소유자를 멤버로 자동 등록
		GroupMemberEntity ownerMember = GroupMemberEntity.create(savedGroup.getId(), ownerUserId);
		groupMemberRepository.save(ownerMember);

		log.info("그룹 생성 완료: groupCode={}, name={}, owner={}",
			savedGroup.getGroupCode(), savedGroup.getName(), ownerUserId);

		// 멤버 수 1 (소유자)로 응답 반환
		return GroupResponseDto.from(savedGroup, 1L);
	}

	// 그룹 수정 (시스템 그룹은 이름/설명만 변경 가능)
	@Transactional
	public GroupResponseDto updateGroup(String groupId, UpdateGroupRequestDto request) {
		// 그룹 존재 검증
		GroupEntity group = groupRepository.findById(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		// 이름 변경 시 중복 검증 (자기 자신 제외)
		if (!group.getName().equals(request.getName()) && groupRepository.existsByName(request.getName())) {
			throw new BusinessException(GroupErrorCode.DUPLICATE_GROUP_NAME);
		}

		// 그룹 정보 수정
		group.updateInfo(request.getName(), request.getDescription());

		// 멤버 수 조회
		long memberCount = groupMemberRepository.countByGroupId(groupId);

		log.info("그룹 수정 완료: groupId={}, name={}", groupId, request.getName());

		// 응답 DTO 반환
		return GroupResponseDto.from(group, memberCount);
	}

	// 그룹 삭제 (시스템 그룹 삭제 불가)
	@Transactional
	public void deleteGroup(String groupId) {
		// 그룹 존재 검증
		GroupEntity group = groupRepository.findById(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		// 시스템 그룹 삭제 방지
		if (group.isSystemGroup()) {
			throw new BusinessException(GroupErrorCode.SYSTEM_GROUP_UNDELETABLE);
		}

		// 그룹 삭제 (CASCADE로 멤버 자동 삭제)
		groupRepository.delete(group);

		log.info("그룹 삭제 완료: groupId={}, groupCode={}", groupId, group.getGroupCode());
	}

	// 그룹 단건 조회
	public GroupResponseDto getGroup(String groupId) {
		// 그룹 존재 검증
		GroupEntity group = groupRepository.findById(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		// 멤버 수 조회
		long memberCount = groupMemberRepository.countByGroupId(groupId);

		// 응답 DTO 반환
		return GroupResponseDto.from(group, memberCount);
	}

	// 전체 그룹 목록 조회
	public List<GroupResponseDto> getAllGroups() {
		// 모든 그룹 조회
		List<GroupEntity> groups = groupRepository.findAll();

		// 각 그룹의 멤버 수를 포함하여 응답 DTO 목록 생성
		return groups.stream()
			.map(group -> {
				long memberCount = groupMemberRepository.countByGroupId(group.getId());
				return GroupResponseDto.from(group, memberCount);
			})
			.toList();
	}

	// 그룹 멤버 추가 (중복 검증)
	@Transactional
	public void addMember(String groupId, String userId) {
		// 그룹 존재 검증
		groupRepository.findById(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		// 대상 사용자 존재 검증
		userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 멤버 중복 검증
		if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
			throw new BusinessException(GroupErrorCode.MEMBER_ALREADY_EXISTS);
		}

		// 멤버 추가
		GroupMemberEntity member = GroupMemberEntity.create(groupId, userId);
		groupMemberRepository.save(member);

		log.info("그룹 멤버 추가: groupId={}, userId={}", groupId, userId);
	}

	// 그룹 멤버 제거
	@Transactional
	public void removeMember(String groupId, String userId) {
		// 그룹 존재 검증
		GroupEntity group = groupRepository.findById(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		// 멤버 존재 검증
		if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
			throw new BusinessException(GroupErrorCode.MEMBER_NOT_FOUND);
		}

		// 시스템 그룹의 멤버 제거 방지
		if (group.isSystemGroup()) {
			throw new BusinessException(GroupErrorCode.SYSTEM_GROUP_MEMBER_PROTECTED);
		}

		// 멤버 제거
		groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);

		log.info("그룹 멤버 제거: groupId={}, userId={}", groupId, userId);
	}

	// 그룹 멤버 목록 조회 (사용자 정보 포함)
	public List<GroupMemberResponseDto> getGroupMembers(String groupId) {
		// 그룹 존재 검증
		groupRepository.findById(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		// 그룹 멤버 목록 조회
		List<GroupMemberEntity> members = groupMemberRepository.findByGroupId(groupId);

		// 각 멤버의 사용자 정보를 조회하여 응답 DTO 생성
		return members.stream()
			.map(member -> {
				// 사용자 정보 조회 (탈퇴 등으로 사용자가 없을 수 있으므로 Optional 처리)
				UserEntity user = userRepository.findById(member.getUserId())
					.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
				return GroupMemberResponseDto.from(user, member.getJoinedAt());
			})
			.toList();
	}

	// 사용자 소속 그룹 목록 조회
	public List<GroupResponseDto> getUserGroups(String userId) {
		// 사용자의 멤버십 목록 조회
		List<GroupMemberEntity> memberships = groupMemberRepository.findByUserId(userId);

		// 각 멤버십의 그룹 정보를 조회하여 응답 DTO 생성
		return memberships.stream()
			.map(membership -> {
				GroupEntity group = groupRepository.findById(membership.getGroupId())
					.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
				long memberCount = groupMemberRepository.countByGroupId(group.getId());
				return GroupResponseDto.from(group, memberCount);
			})
			.toList();
	}

	// 기본 그룹(user) 자동 배정 (회원가입 시 호출)
	@Transactional
	public void assignToDefaultGroup(String userId) {
		// 기본 그룹(user) 조회 — DB에 시드 데이터가 없을 수 있으므로 graceful 처리
		groupRepository.findByGroupCode(DEFAULT_GROUP_CODE).ifPresentOrElse(
			group -> {
				// 이미 소속되어 있으면 스킵
				if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
					// 기본 그룹에 멤버 추가
					GroupMemberEntity member = GroupMemberEntity.create(group.getId(), userId);
					groupMemberRepository.save(member);
					log.info("기본 그룹 배정 완료: userId={}, groupCode={}", userId, DEFAULT_GROUP_CODE);
				}
			},
			() -> log.warn("기본 그룹이 존재하지 않습니다: groupCode={} — 시드 데이터를 확인하세요", DEFAULT_GROUP_CODE)
		);
	}
}
