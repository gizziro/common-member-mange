package com.gizzi.core.domain.user.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.UserErrorCode;
import com.gizzi.core.domain.user.dto.AvailabilityCheckResponseDto;
import com.gizzi.core.domain.user.dto.SignupRequestDto;
import com.gizzi.core.domain.user.dto.SignupResponseDto;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 사용자 관련 비즈니스 로직을 처리하는 서비스
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

	// 사용자 리포지토리
	private final UserRepository userRepository;

	// 비밀번호 인코더 (BCrypt)
	private final PasswordEncoder passwordEncoder;

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
}
