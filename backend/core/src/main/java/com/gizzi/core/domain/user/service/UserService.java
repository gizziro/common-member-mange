package com.gizzi.core.domain.user.service;

import com.gizzi.core.common.exception.AuthErrorCode;
import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SmsErrorCode;
import com.gizzi.core.common.exception.UserErrorCode;
import com.gizzi.core.domain.audit.AuditAction;
import com.gizzi.core.domain.audit.AuditTarget;
import com.gizzi.core.domain.audit.service.AuditLogService;
import com.gizzi.core.domain.auth.repository.UserIdentityRepository;
import com.gizzi.core.domain.auth.service.RedisTokenService;
import com.gizzi.core.domain.sms.service.OtpService;
import com.gizzi.core.domain.group.entity.GroupEntity;
import com.gizzi.core.domain.group.repository.GroupMemberRepository;
import com.gizzi.core.domain.group.repository.GroupRepository;
import com.gizzi.core.domain.group.service.GroupService;
import com.gizzi.core.domain.sms.dto.ResetPasswordResponseDto;
import com.gizzi.core.domain.sms.service.SmsNotificationService;
import com.gizzi.core.domain.user.dto.AvailabilityCheckResponseDto;
import com.gizzi.core.domain.user.dto.ChangePasswordRequestDto;
import com.gizzi.core.domain.user.dto.SignupRequestDto;
import com.gizzi.core.domain.user.dto.SignupResponseDto;
import com.gizzi.core.domain.user.dto.UpdateUserRequestDto;
import com.gizzi.core.domain.user.dto.UserListResponseDto;
import com.gizzi.core.domain.user.dto.UserResponseDto;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import com.gizzi.core.domain.setting.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

// 사용자 관련 비즈니스 로직을 처리하는 서비스
// 회원가입, 중복 확인, 관리자 CRUD, 비밀번호 변경/초기화 등을 담당한다
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final UserRepository          userRepository;           // 사용자 리포지토리
	private final PasswordEncoder         passwordEncoder;          // 비밀번호 인코더 (BCrypt)
	private final GroupService            groupService;             // 그룹 서비스 (기본 그룹 배정용)
	private final GroupRepository         groupRepository;          // 그룹 리포지토리 (사용자 삭제 시 소유 그룹 owner 해제용)
	private final GroupMemberRepository   groupMemberRepository;    // 그룹 멤버 리포지토리 (administrator 그룹 소속 확인용)
	private final RedisTokenService       redisTokenService;        // Redis 토큰 서비스 (사용자 삭제 시 활성 토큰 정리용)
	private final UserIdentityRepository  userIdentityRepository;   // 소셜 연동 리포지토리 (사용자 삭제 시 연동 정보 정리용)
	private final SettingService          settingService;           // 시스템 설정 서비스 (회원가입 활성화, 기본 상태 등)
	private final AuditLogService         auditLogService;          // 감사 로그 서비스
	private final OtpService              otpService;               // OTP 서비스 (SMS 전화번호 인증)
	private final SmsNotificationService  smsNotificationService;   // SMS 알림 서비스 (비밀번호 초기화 SMS 발송용)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 상수 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 임시 비밀번호 생성 문자셋 (영대소문자 + 숫자 + 특수문자)
	private static final String TEMP_PASSWORD_CHARS =
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";

	//======================================================================================================================
	// 회원가입
	//======================================================================================================================

	// 로컬 회원가입 처리
	@Transactional
	public SignupResponseDto signup(SignupRequestDto request)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 시스템 설정 확인
		//----------------------------------------------------------------------------------------------------------------------

		// 시스템 설정: 회원가입 활성화 여부 확인
		boolean signupEnabled = settingService.getSystemBoolean("signup", "enabled");
		if (!signupEnabled)
		{
			throw new BusinessException(UserErrorCode.SIGNUP_DISABLED);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 중복 검증
		//----------------------------------------------------------------------------------------------------------------------

		// 아이디 중복 검증
		if (userRepository.existsByUserId(request.getUserId()))
		{
			throw new BusinessException(UserErrorCode.DUPLICATE_USER_ID);
		}

		// 이메일 중복 검증
		if (userRepository.existsByEmail(request.getEmail()))
		{
			throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 비밀번호 해싱 + 초기 상태 결정
		//----------------------------------------------------------------------------------------------------------------------

		// 비밀번호 BCrypt 해싱
		String encodedPassword = passwordEncoder.encode(request.getPassword());

		// 시스템 설정: 신규 사용자 초기 상태 (ACTIVE, PENDING 등)
		String defaultStatus = settingService.getSystemSetting("signup", "default_status");

		//----------------------------------------------------------------------------------------------------------------------
		// SMS 전화번호 인증 검증
		//----------------------------------------------------------------------------------------------------------------------

		// SMS 인증 활성화 시 전화번호 인증 검증
		boolean smsEnabled = settingService.getSystemBoolean("sms", "enabled");
		if (smsEnabled && request.getPhone() != null && !request.getPhone().isBlank())
		{
			// 전화번호가 제공된 경우 인증 토큰 검증
			if (request.getPhoneVerificationToken() == null
				|| !otpService.isPhoneVerified(request.getPhone(), request.getPhoneVerificationToken()))
			{
				throw new BusinessException(SmsErrorCode.SMS_VERIFICATION_INVALID);
			}
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 사용자 엔티티 생성 + 전화번호 설정
		//----------------------------------------------------------------------------------------------------------------------

		// 사용자 엔티티 생성 (초기 상태는 시스템 설정에서 결정)
		UserEntity user = UserEntity.createLocalUser(
			request.getUserId(),
			request.getUsername(),
			request.getEmail(),
			encodedPassword,
			defaultStatus
		);

		// 전화번호가 제공된 경우 엔티티에 설정
		if (request.getPhone() != null && !request.getPhone().isBlank())
		{
			user.updatePhone(request.getPhone());
			// SMS 인증이 활성화되고 인증 토큰이 유효하면 인증 완료 처리
			if (smsEnabled && request.getPhoneVerificationToken() != null)
			{
				user.verifyPhone();
				// 인증 완료 토큰 소비 (1회용)
				otpService.consumeVerification(request.getPhone());
			}
		}

		//----------------------------------------------------------------------------------------------------------------------
		// DB 저장 + 기본 그룹 배정
		//----------------------------------------------------------------------------------------------------------------------

		// DB 저장
		UserEntity savedUser = userRepository.save(user);

		// 기본 그룹(user) 자동 배정
		groupService.assignToDefaultGroup(savedUser.getId());

		log.info("회원가입 완료: userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());

		// 회원가입 감사 로그
		auditLogService.logSuccess(savedUser.getId(), AuditAction.USER_SIGNUP, AuditTarget.USER, savedUser.getId(),
			"회원가입: " + savedUser.getUserId(), Map.of("email", savedUser.getEmail()));

		// 응답 DTO 변환 후 반환
		return SignupResponseDto.from(savedUser);
	}

	//======================================================================================================================
	// 로그인 관련
	//======================================================================================================================

	// 로그인 실패 횟수 증가 (독립 트랜잭션)
	// 로그인 실패 시 BusinessException으로 외부 트랜잭션이 롤백되므로
	// REQUIRES_NEW로 별도 트랜잭션에서 실행하여 실패 횟수가 반드시 커밋되도록 한다
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void incrementLoginFailCount(String userPk)
	{
		userRepository.findById(userPk).ifPresent(user -> {
			// 시스템 설정: 최대 로그인 실패 허용 횟수
			int maxFailCount = (int) settingService.getSystemNumber("auth", "max_login_fail");
			// 실패 횟수 증가 (설정된 최대 횟수 초과 시 자동 잠금)
			user.incrementLoginFailCount(maxFailCount);
			log.warn("로그인 실패 카운트 증가: userId={}, failCount={}, maxFail={}, locked={}",
				user.getUserId(), user.getLoginFailCount(), maxFailCount, user.getIsLocked());
		});
	}

	//======================================================================================================================
	// 중복 확인
	//======================================================================================================================

	// 아이디 사용 가능 여부 확인
	public AvailabilityCheckResponseDto checkUserIdAvailable(String userId)
	{
		// 아이디 존재 여부를 조회하여 사용 가능 여부 반환
		boolean exists = userRepository.existsByUserId(userId);
		return exists ? AvailabilityCheckResponseDto.unavailable() : AvailabilityCheckResponseDto.available();
	}

	// 이메일 사용 가능 여부 확인
	public AvailabilityCheckResponseDto checkEmailAvailable(String email)
	{
		// 이메일 존재 여부를 조회하여 사용 가능 여부 반환
		boolean exists = userRepository.existsByEmail(email);
		return exists ? AvailabilityCheckResponseDto.unavailable() : AvailabilityCheckResponseDto.available();
	}

	// 로그인 ID로 사용자 엔티티 조회 (admin 로그인 전 관리자 그룹 확인용)
	// 없으면 AUTH_INVALID_CREDENTIALS 예외 발생
	public UserEntity findByLoginId(String loginId)
	{
		return userRepository.findByUserId(loginId)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));
	}

	//======================================================================================================================
	// 관리자 전용 — 사용자 조회
	//======================================================================================================================

	// 사용자 목록 페이지네이션 조회
	public Page<UserListResponseDto> getUsers(Pageable pageable)
	{
		// 전체 사용자를 페이지네이션하여 조회 후 DTO 변환
		return userRepository.findAll(pageable)
			.map(UserListResponseDto::from);
	}

	// 사용자 단건 상세 조회
	public UserResponseDto getUser(String id)
	{
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 상세 응답 DTO 반환
		return UserResponseDto.from(user);
	}

	//======================================================================================================================
	// 관리자 전용 — 사용자 수정
	//======================================================================================================================

	// 관리자에 의한 사용자 정보 수정
	@Transactional
	public UserResponseDto updateUser(String id, UpdateUserRequestDto request)
	{
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 이메일 변경 시 중복 검증 (자기 자신 제외)
		if (!user.getEmail().equals(request.getEmail())
			&& userRepository.existsByEmailAndIdNot(request.getEmail(), id))
		{
			throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
		}

		// 사용자 정보 수정
		user.updateByAdmin(request.getUsername(), request.getEmail(), request.getUserStatus());

		// SMS 수신 동의 여부 수정 (요청에 포함된 경우만)
		if (request.getIsSmsAgree() != null)
		{
			user.updateSmsConsent(request.getIsSmsAgree());
		}

		log.info("사용자 정보 수정: id={}, username={}, email={}, status={}, smsAgree={}",
			id, request.getUsername(), request.getEmail(), request.getUserStatus(), request.getIsSmsAgree());

		// 사용자 수정 감사 로그
		auditLogService.logSuccess(null, AuditAction.USER_UPDATE, AuditTarget.USER, id,
			"사용자 정보 수정: " + user.getUserId(), Map.of("username", request.getUsername(), "status", request.getUserStatus()));

		// 수정된 정보 응답
		return UserResponseDto.from(user);
	}

	//======================================================================================================================
	// 관리자 전용 — 잠금 해제
	//======================================================================================================================

	// 계정 잠금 해제
	@Transactional
	public UserResponseDto unlockUser(String id)
	{
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 잠금 상태가 아닌 경우 예외
		if (!Boolean.TRUE.equals(user.getIsLocked()))
		{
			throw new BusinessException(UserErrorCode.USER_NOT_LOCKED);
		}

		// 잠금 해제 (실패 횟수 초기화 포함)
		user.unlock();

		log.info("사용자 잠금 해제: id={}, userId={}", id, user.getUserId());

		// 잠금 해제 감사 로그
		auditLogService.logSuccess(null, AuditAction.USER_UNLOCK, AuditTarget.USER, id,
			"계정 잠금 해제: " + user.getUserId(), null);

		// 해제 후 정보 응답
		return UserResponseDto.from(user);
	}

	//======================================================================================================================
	// 관리자 전용 — 사용자 삭제
	//======================================================================================================================

	// 사용자 삭제 (소유 그룹 owner 해제 후 삭제)
	// currentUserPk: 현재 로그인한 사용자의 PK (자기 자신 삭제 방지)
	@Transactional
	public void deleteUser(String id, String currentUserPk)
	{
		// 자기 자신 삭제 방지
		if (id.equals(currentUserPk))
		{
			throw new BusinessException(UserErrorCode.SELF_DELETE);
		}

		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		//----------------------------------------------------------------------------------------------------------------------
		// administrator 그룹 소속 사용자 삭제 방지
		//----------------------------------------------------------------------------------------------------------------------

		groupRepository.findByGroupCode("administrator").ifPresent(adminGroup -> {
			// administrator 그룹에 해당 사용자가 소속되어 있으면 삭제 불가
			if (groupMemberRepository.existsByGroupIdAndUserId(adminGroup.getId(), id))
			{
				throw new BusinessException(UserErrorCode.ADMIN_USER_UNDELETABLE);
			}
		});

		//----------------------------------------------------------------------------------------------------------------------
		// 소유 그룹 owner 해제 + 활성 토큰 삭제
		//----------------------------------------------------------------------------------------------------------------------

		// 해당 사용자가 소유한 그룹의 owner를 null로 해제
		List<GroupEntity> ownedGroups = groupRepository.findByOwnerUserId(id);
		for (GroupEntity group : ownedGroups)
		{
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

		// 사용자 삭제 감사 로그 (관리자에 의한 삭제)
		auditLogService.logSuccess(currentUserPk, AuditAction.USER_DELETE, AuditTarget.USER, id,
			"사용자 삭제: " + user.getUserId(), null);
	}

	//======================================================================================================================
	// 사용자 본인 탈퇴
	//======================================================================================================================

	// 사용자 본인 탈퇴 (마이페이지에서 호출)
	@Transactional
	public void withdrawUser(String userPk)
	{
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(userPk)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		//----------------------------------------------------------------------------------------------------------------------
		// administrator 그룹 소속 사용자 탈퇴 방지
		//----------------------------------------------------------------------------------------------------------------------

		groupRepository.findByGroupCode("administrator").ifPresent(adminGroup -> {
			if (groupMemberRepository.existsByGroupIdAndUserId(adminGroup.getId(), userPk))
			{
				throw new BusinessException(UserErrorCode.ADMIN_USER_UNDELETABLE);
			}
		});

		//----------------------------------------------------------------------------------------------------------------------
		// 소유 그룹 owner 해제 + 활성 토큰 삭제
		//----------------------------------------------------------------------------------------------------------------------

		// 해당 사용자가 소유한 그룹의 owner를 null로 해제
		List<GroupEntity> ownedGroups = groupRepository.findByOwnerUserId(userPk);
		for (GroupEntity group : ownedGroups)
		{
			group.clearOwner();
			log.info("그룹 소유자 해제: groupId={}, groupCode={}", group.getId(), group.getGroupCode());
		}

		// 사용자의 모든 활성 토큰 삭제 (Redis)
		redisTokenService.deleteAllUserTokens(user.getId());

		// 소셜 연동 정보 명시적 삭제 (CASCADE에 의존하지 않음)
		userIdentityRepository.deleteByUserId(userPk);

		// 사용자 삭제 (CASCADE로 그룹 멤버십 등 자동 정리)
		userRepository.delete(user);

		log.info("사용자 본인 탈퇴: id={}, userId={}", userPk, user.getUserId());

		// 본인 탈퇴 감사 로그
		auditLogService.logSuccess(userPk, AuditAction.USER_DELETE, AuditTarget.USER, userPk,
			"본인 탈퇴: " + user.getUserId(), null);
	}

	//======================================================================================================================
	// 비밀번호 관리
	//======================================================================================================================

	// 관리자에 의한 비밀번호 변경
	@Transactional
	public void changePassword(String id, ChangePasswordRequestDto request)
	{
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// 새 비밀번호 BCrypt 해싱 후 변경
		String encodedPassword = passwordEncoder.encode(request.getNewPassword());
		user.changePassword(encodedPassword);

		log.info("비밀번호 변경: id={}, userId={}", id, user.getUserId());

		// 비밀번호 변경 감사 로그
		auditLogService.logSuccess(null, AuditAction.PASSWORD_CHANGE, AuditTarget.USER, id,
			"비밀번호 변경: " + user.getUserId(), null);
	}

	// 관리자에 의한 비밀번호 초기화 (임시 비밀번호 생성 + SMS 자동 발송)
	@Transactional
	public ResetPasswordResponseDto resetPassword(String id, String adminPk)
	{
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		//----------------------------------------------------------------------------------------------------------------------
		// 임시 비밀번호 생성
		//----------------------------------------------------------------------------------------------------------------------

		// SecureRandom으로 임시 비밀번호 생성 (10자)
		SecureRandom  random        = new SecureRandom();
		StringBuilder tempPwBuilder = new StringBuilder(10);
		for (int i = 0; i < 10; i++)
		{
			int idx = random.nextInt(TEMP_PASSWORD_CHARS.length());
			tempPwBuilder.append(TEMP_PASSWORD_CHARS.charAt(idx));
		}
		String tempPassword = tempPwBuilder.toString();

		// BCrypt 해싱 후 비밀번호 변경
		String encodedPassword = passwordEncoder.encode(tempPassword);
		user.changePassword(encodedPassword);

		log.info("비밀번호 초기화: id={}, userId={}", id, user.getUserId());

		// 비밀번호 초기화 감사 로그
		auditLogService.logSuccess(adminPk, AuditAction.PASSWORD_RESET, AuditTarget.USER, id,
			"비밀번호 초기화: " + user.getUserId(), null);

		//----------------------------------------------------------------------------------------------------------------------
		// SMS 발송 시도
		//----------------------------------------------------------------------------------------------------------------------

		// 전화번호가 있으면 SMS 발송 시도 (실패해도 비밀번호는 이미 변경됨)
		boolean smsSent = smsNotificationService.sendPasswordResetSms(user, tempPassword, adminPk);

		// 임시 비밀번호 + SMS 발송 여부 반환
		return ResetPasswordResponseDto.builder()
			.temporaryPassword(tempPassword)
			.smsSent(smsSent)
			.build();
	}

	//======================================================================================================================
	// SMS 수신 동의
	//======================================================================================================================

	// 사용자 본인 SMS 수신 동의 변경
	@Transactional
	public void updateSmsConsent(String userPk, boolean agree)
	{
		// PK로 사용자 조회 (없으면 예외)
		UserEntity user = userRepository.findById(userPk)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		// SMS 수신 동의 상태 변경
		user.updateSmsConsent(agree);

		log.info("SMS 수신 동의 변경: userId={}, agree={}", user.getUserId(), agree);
	}
}
