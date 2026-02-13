package com.gizzi.core.domain.user.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.ErrorCode;
import com.gizzi.core.domain.user.dto.SignupRequest;
import com.gizzi.core.domain.user.dto.SignupResponse;
import com.gizzi.core.domain.user.entity.User;
import com.gizzi.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
	public SignupResponse signup(SignupRequest request) {
		// 아이디 중복 검증
		if (userRepository.existsByUserId(request.getUserId())) {
			throw new BusinessException(ErrorCode.DUPLICATE_USER_ID);
		}

		// 이메일 중복 검증
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		// 비밀번호 BCrypt 해싱
		String encodedPassword = passwordEncoder.encode(request.getPassword());

		// 사용자 엔티티 생성
		User user = User.createLocalUser(
			request.getUserId(),
			request.getUsername(),
			request.getEmail(),
			encodedPassword
		);

		// DB 저장
		User savedUser = userRepository.save(user);

		log.info("회원가입 완료: userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());

		// 응답 DTO 변환 후 반환
		return SignupResponse.from(savedUser);
	}
}
