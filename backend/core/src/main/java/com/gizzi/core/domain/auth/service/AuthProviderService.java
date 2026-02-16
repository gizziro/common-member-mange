package com.gizzi.core.domain.auth.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.OAuth2ErrorCode;
import com.gizzi.core.domain.auth.dto.AuthProviderResponseDto;
import com.gizzi.core.domain.auth.dto.UpdateAuthProviderRequestDto;
import com.gizzi.core.domain.auth.entity.AuthProviderEntity;
import com.gizzi.core.domain.auth.repository.AuthProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 인증 제공자 관리 서비스 (관리자 전용)
// Provider 목록 조회, 상세 조회, 설정 수정을 담당한다
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthProviderService {

	// 인증 제공자 리포지토리
	private final AuthProviderRepository authProviderRepository;

	// 전체 Provider 목록 조회
	public List<AuthProviderResponseDto> getAllProviders() {
		// 모든 Provider를 조회하여 DTO 변환
		return authProviderRepository.findAll().stream()
			.map(AuthProviderResponseDto::from)
			.toList();
	}

	// Provider 상세 조회
	public AuthProviderResponseDto getProvider(String id) {
		// PK로 Provider 조회 (없으면 예외)
		AuthProviderEntity provider = authProviderRepository.findById(id)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.PROVIDER_NOT_FOUND));

		return AuthProviderResponseDto.from(provider);
	}

	// Provider 설정 수정
	@Transactional
	public AuthProviderResponseDto updateProvider(String id, UpdateAuthProviderRequestDto request) {
		// PK로 Provider 조회 (없으면 예외)
		AuthProviderEntity provider = authProviderRepository.findById(id)
			.orElseThrow(() -> new BusinessException(OAuth2ErrorCode.PROVIDER_NOT_FOUND));

		// Provider 설정 업데이트
		provider.update(
			request.getClientId(),
			request.getClientSecret(),
			request.getRedirectUri(),
			request.getScope(),
			request.getIconUrl(),
			request.getIsEnabled()
		);

		log.info("인증 제공자 설정 수정: id={}, code={}, enabled={}", id, provider.getCode(), request.getIsEnabled());

		return AuthProviderResponseDto.from(provider);
	}
}
