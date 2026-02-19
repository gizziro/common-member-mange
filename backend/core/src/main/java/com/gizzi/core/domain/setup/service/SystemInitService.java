package com.gizzi.core.domain.setup.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SetupErrorCode;
import com.gizzi.core.domain.sms.service.OtpService;
import com.gizzi.core.domain.group.entity.GroupEntity;
import com.gizzi.core.domain.group.repository.GroupMemberRepository;
import com.gizzi.core.domain.group.repository.GroupRepository;
import com.gizzi.core.domain.group.service.GroupService;
import com.gizzi.core.domain.setup.dto.SystemSetupRequestDto;
import com.gizzi.core.domain.user.dto.SignupRequestDto;
import com.gizzi.core.domain.user.dto.SignupResponseDto;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import com.gizzi.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

// 시스템 초기 설정 서비스
// 최초 배포 시 관리자 계정 생성을 담당한다
// administrator 그룹 멤버 유무로 초기화 상태를 판단한다
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemInitService
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 상수 ]
	//----------------------------------------------------------------------------------------------------------------------
	private static final String ADMIN_GROUP_CODE = "administrator";	// 관리자 그룹 코드 (시드 데이터)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final UserService           userService;			// 사용자 서비스 (회원가입 처리)
	private final GroupService          groupService;			// 그룹 서비스 (멤버 추가)
	private final UserRepository        userRepository;			// 사용자 리포지토리 (activate 처리)
	private final GroupRepository       groupRepository;		// 그룹 리포지토리 (administrator 그룹 조회)
	private final GroupMemberRepository groupMemberRepository;	// 그룹 멤버 리포지토리 (멤버 수 확인)
	private final OtpService            otpService;				// OTP 서비스 (SMS 전화번호 인증)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 초기화 상태 캐시 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final AtomicBoolean initializedCache = new AtomicBoolean(false);	// true면 매 요청마다 DB 조회 생략

	//----------------------------------------------------------------------------------------------------------------------
	// 초기화 캐시 무효화 (DB 초기화 후 재확인이 필요할 때 호출)
	// 캐시를 false로 리셋하여 다음 isInitialized() 호출 시 DB를 다시 확인하게 한다
	//----------------------------------------------------------------------------------------------------------------------
	public void invalidateCache()
	{
		initializedCache.set(false);
		log.info("시스템 초기화 캐시 무효화됨 — 다음 조회 시 DB 재확인");
	}

	//======================================================================================================================
	// 시스템 초기화 여부 확인
	// administrator 그룹에 멤버가 1명 이상이면 초기화 완료로 판단
	//----------------------------------------------------------------------------------------------------------------------
	public boolean isInitialized()
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 캐시가 true이면 DB 조회 없이 즉시 반환 (한 번 초기화되면 되돌릴 수 없으므로)
		//----------------------------------------------------------------------------------------------------------------------
		if (initializedCache.get())
		{
			return true;
		}

		//----------------------------------------------------------------------------------------------------------------------
		// administrator 그룹 조회
		//----------------------------------------------------------------------------------------------------------------------
		GroupEntity adminGroup = groupRepository.findByGroupCode(ADMIN_GROUP_CODE)
			.orElse(null);

		// 그룹 자체가 없으면 미초기화 상태 (시드 데이터 누락)
		if (adminGroup == null)
		{
			return false;
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 멤버 수 확인 — 1명 이상이면 초기화 완료
		//----------------------------------------------------------------------------------------------------------------------
		boolean initialized = groupMemberRepository.countByGroupId(adminGroup.getId()) > 0;

		// 초기화 상태를 캐시에 저장
		if (initialized)
		{
			initializedCache.set(true);
		}

		return initialized;
	}

	//======================================================================================================================
	// 최초 관리자 계정 생성 (셋업 위자드)
	//======================================================================================================================
	@Transactional
	public SignupResponseDto setupAdmin(SystemSetupRequestDto request)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 이미 초기화된 시스템이면 거부
		//----------------------------------------------------------------------------------------------------------------------
		if (isInitialized())
		{
			throw new BusinessException(SetupErrorCode.SYSTEM_ALREADY_INITIALIZED);
		}

		//----------------------------------------------------------------------------------------------------------------------
		// administrator 그룹 존재 확인 (시드 데이터 필수)
		//----------------------------------------------------------------------------------------------------------------------
		GroupEntity adminGroup = groupRepository.findByGroupCode(ADMIN_GROUP_CODE)
			.orElseThrow(() -> new BusinessException(SetupErrorCode.ADMIN_GROUP_NOT_FOUND));

		//----------------------------------------------------------------------------------------------------------------------
		// 1. UserService.signup()으로 사용자 생성 + "user" 그룹 자동 배정
		//----------------------------------------------------------------------------------------------------------------------
		SignupRequestDto signupRequest = SignupRequestDto.builder()
			.userId(request.getUserId())
			.password(request.getPassword())
			.username(request.getUsername())
			.email(request.getEmail())
			.build();

		SignupResponseDto signupResponse = userService.signup(signupRequest);

		//----------------------------------------------------------------------------------------------------------------------
		// 2. 생성된 사용자를 ACTIVE 상태로 전환 (관리자는 이메일 인증 없이 즉시 활성)
		//----------------------------------------------------------------------------------------------------------------------
		UserEntity user = userRepository.findById(signupResponse.getId())
			.orElseThrow(() -> new BusinessException(SetupErrorCode.ADMIN_GROUP_NOT_FOUND));
		user.activate();

		//----------------------------------------------------------------------------------------------------------------------
		// 3. 전화번호가 제공된 경우 설정
		//----------------------------------------------------------------------------------------------------------------------
		if (request.getPhone() != null && !request.getPhone().isBlank())
		{
			user.updatePhone(request.getPhone());
			// 인증 토큰이 유효하면 인증 완료 처리
			if (request.getPhoneVerificationToken() != null
				&& otpService.isPhoneVerified(request.getPhone(), request.getPhoneVerificationToken()))
			{
				user.verifyPhone();
				otpService.consumeVerification(request.getPhone());
			}
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 4. administrator 그룹에 멤버 추가 (로그인 ID 기반)
		//----------------------------------------------------------------------------------------------------------------------
		groupService.addMember(adminGroup.getId(), request.getUserId());

		// 캐시 갱신 (이후 SetupGuardFilter에서 DB 조회 없이 통과)
		initializedCache.set(true);

		log.info("시스템 초기 설정 완료: 관리자 계정 생성 userId={}, email={}",
			request.getUserId(), request.getEmail());

		//----------------------------------------------------------------------------------------------------------------------
		// 응답 DTO 반환 (activate 이후 상태를 다시 조회하여 반환)
		//----------------------------------------------------------------------------------------------------------------------
		UserEntity activatedUser = userRepository.findById(signupResponse.getId())
			.orElseThrow(() -> new BusinessException(SetupErrorCode.ADMIN_GROUP_NOT_FOUND));
		return SignupResponseDto.from(activatedUser);
	}
}
