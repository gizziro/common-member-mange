package com.gizzi.core.domain.user.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.UserErrorCode;
import com.gizzi.core.domain.auth.repository.UserIdentityRepository;
import com.gizzi.core.domain.auth.service.RedisTokenService;
import com.gizzi.core.domain.group.entity.GroupEntity;
import com.gizzi.core.domain.group.repository.GroupMemberRepository;
import com.gizzi.core.domain.group.repository.GroupRepository;
import com.gizzi.core.domain.group.service.GroupService;
import com.gizzi.core.domain.user.dto.AvailabilityCheckResponseDto;
import com.gizzi.core.domain.user.dto.ChangePasswordRequestDto;
import com.gizzi.core.domain.user.dto.SignupRequestDto;
import com.gizzi.core.domain.user.dto.SignupResponseDto;
import com.gizzi.core.domain.user.dto.UpdateUserRequestDto;
import com.gizzi.core.domain.user.dto.UserListResponseDto;
import com.gizzi.core.domain.user.dto.UserResponseDto;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 사용자 관련 비즈니스 로직을 처리하는 서비스
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

	// 사용자 리포지토리
	private final UserRepository          userRepository;

	// 비밀번호 인코더 (BCrypt)
	private final PasswordEncoder         passwordEncoder;

	// 그룹 서비스 (기본 그룹 배정용)
	private final GroupService            groupService;

	// 그룹 리포지토리 (사용자 삭제 시 소유 그룹 owner 해제용)
	private final GroupRepository         groupRepository;

	// 그룹 멤버 리포지토리 (administrator 그룹 소속 확인용)
	private final GroupMemberRepository   groupMemberRepository;

	// Redis 토큰 서비스 (사용자 삭제 시 활성 토큰 정리용)
	private final RedisTokenService       redisTokenService;

	// 소셜 연동 리포지토리 (사용자 삭제 시 연동 정보 정리용)
	private final UserIdentityRepository  userIdentityRepository;

	// 로컬 회원가입 처리
	@Transactional
	public SignupResponseDto signup(SignupRequestDto request) {
		// 아이디 중복 검증
		if (userRepository.existsByUserId(request.getUserId())) {
			throw new BusinessException(UserErrorCode.DUPLICATE_USER_ID);
		}

		// 이메일 중복 검증
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
		}

		// 비밀번호 BCrypt 해싱
		String encodedPassword = passwordEncoder.encode(request.getPassword());

		// 사용자 엔티티 생성
		UserEntity user = UserEntity.createLocalUser(
			request.getUserId(),
			request.getUsername(),
			request.getEmail(),
			encodedPassword
		);

		// DB 저장
		UserEntity savedUser = userRepository.save(user);

		// 기본 그룹(user) 자동 배정
		groupService.assignToDefaultGroup(savedUser.getId());

		log.info("회원가입 완료: userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());

		// 응답 DTO 변환 후 반환
		return SignupResponseDto.from(savedUser);
	}

	// 로그인 실패 횟수 증가 (독립 트랜잭션)
	// 로그인 실패 시 BusinessException으로 외부 트랜잭션이 롤백되므로
	// REQUIRES_NEW로 별도 트랜잭션에서 실행하여 실패 횟수가 반드시 커밋되도록 한다
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void incrementLoginFailCount(String userPk) {
		userRepository.findById(userPk).ifPresent(user -> {
			// 실패 횟수 증가 (MAX 초과 시 자동 잠금)
			user.incrementLoginFailCount();
			log.warn("로그인 실패 카운트 증가: userId={}, failCount={}, locked={}",
				user.getUserId(), user.getLoginFailCount(), user.getIsLocked());
		});
	}

	// 아이디 사용 가능 여부 확인
	public AvailabilityCheckResponseDto checkUserIdAvailable(String userId) {
		// 아이디 존재 여부를 조회하여 사용 가능 여부 반환
		boolean exists = userRepository.existsByUserId(userId);
		return exists ? AvailabilityCheckResponseDto.unavailable() : AvailabilityCheckResponseDto.available();
	}

	// 이메일 사용 가능 여부 확인
	public AvailabilityCheckResponseDto checkEmailAvailable(String email) {
		// 이메일 존재 여부를 조회하여 사용 가능 여부 반환
		boolean exists = userRepository.existsByEmail(email);
		return exists ? AvailabilityCheckResponseDto.unavailable() : AvailabilityCheckResponseDto.available();
	}

	// ===========================
	// 관리자 전용 메서드
	// ===========================

	// 사용자 목록 페이지네이션 조회
	public Page<UserListResponseDto> getUsers(Pageable pageable) {
		// 전체 사용자를 페이지네이션하여 조회 후 DTO 변환
		return userRepository.findAll(pageable)
			.map(UserListResponseDto::from);
	}

	// 사용자 단건 상세 조회
	public UserResponseDto getUser(String id) {
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 상세 응답 DTO 반환
		return UserResponseDto.from(user);
	}

	// 관리자에 의한 사용자 정보 수정
	@Transactional
	public UserResponseDto updateUser(String id, UpdateUserRequestDto request) {
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 이메일 변경 시 중복 검증 (자기 자신 제외)
		if (!user.getEmail().equals(request.getEmail())
			&& userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
			throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
		}

		// 사용자 정보 수정
		user.updateByAdmin(request.getUsername(), request.getEmail(), request.getUserStatus());

		log.info("사용자 정보 수정: id={}, username={}, email={}, status={}",
			id, request.getUsername(), request.getEmail(), request.getUserStatus());

		// 수정된 정보 응답
		return UserResponseDto.from(user);
	}

	// 계정 잠금 해제
	@Transactional
	public UserResponseDto unlockUser(String id) {
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 잠금 상태가 아닌 경우 예외
		if (!Boolean.TRUE.equals(user.getIsLocked())) {
			throw new BusinessException(UserErrorCode.USER_NOT_LOCKED);
		}

		// 잠금 해제 (실패 횟수 초기화 포함)
		user.unlock();

		log.info("사용자 잠금 해제: id={}, userId={}", id, user.getUserId());

		// 해제 후 정보 응답
		return UserResponseDto.from(user);
	}

	// 사용자 삭제 (소유 그룹 owner 해제 후 삭제)
	// currentUserPk: 현재 로그인한 사용자의 PK (자기 자신 삭제 방지)
	@Transactional
	public void deleteUser(String id, String currentUserPk) {
		// 자기 자신 삭제 방지
		if (id.equals(currentUserPk)) {
			throw new BusinessException(UserErrorCode.SELF_DELETE);
		}

		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// administrator 그룹 소속 사용자 삭제 방지
		groupRepository.findByGroupCode("administrator").ifPresent(adminGroup -> {
			// administrator 그룹에 해당 사용자가 소속되어 있으면 삭제 불가
			if (groupMemberRepository.existsByGroupIdAndUserId(adminGroup.getId(), id)) {
				throw new BusinessException(UserErrorCode.ADMIN_USER_UNDELETABLE);
			}
		});

		// 해당 사용자가 소유한 그룹의 owner를 null로 해제
		List<GroupEntity> ownedGroups = groupRepository.findByOwnerUserId(id);
		for (GroupEntity group : ownedGroups) {
			group.clearOwner();
			log.info("그룹 소유자 해제: groupId={}, groupCode={}", group.getId(), group.getGroupCode());
		}

		// 사용자의 모든 활성 토큰 삭제 (Redis)
		redisTokenService.deleteAllUserTokens(user.getId());

		// 소셜 연동 정보 명시적 삭제 (CASCADE에 의존하지 않음)
		userIdentityRepository.deleteByUserId(id);

		// 사용자 삭제 (CASCADE로 그룹 멤버십 등 자동 정리)
		userRepository.delete(user);

		log.info("사용자 삭제: id={}, userId={}", id, user.getUserId());
	}

	// 관리자에 의한 비밀번호 변경
	@Transactional
	public void changePassword(String id, ChangePasswordRequestDto request) {
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 새 비밀번호 BCrypt 해싱 후 변경
		String encodedPassword = passwordEncoder.encode(request.getNewPassword());
		user.changePassword(encodedPassword);

		log.info("비밀번호 변경: id={}, userId={}", id, user.getUserId());
	}
}
