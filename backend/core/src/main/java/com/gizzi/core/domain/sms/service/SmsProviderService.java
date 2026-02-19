package com.gizzi.core.domain.sms.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SmsErrorCode;
import com.gizzi.core.domain.sms.dto.SmsProviderResponseDto;
import com.gizzi.core.domain.sms.dto.UpdateSmsProviderRequestDto;
import com.gizzi.core.domain.sms.entity.SmsProviderEntity;
import com.gizzi.core.domain.sms.repository.SmsProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// SMS 프로바이더 관리 서비스 (관리자 전용)
// 프로바이더 목록 조회, 설정 수정을 담당한다
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SmsProviderService
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final SmsProviderRepository smsProviderRepository;	// SMS 프로바이더 리포지토리

	//======================================================================================================================
	// [ 핵심 비즈니스 메서드 ]
	//======================================================================================================================

	// 전체 프로바이더 목록 조회 (표시 순서 정렬)
	public List<SmsProviderResponseDto> getAllProviders()
	{
		// 전체 프로바이더 조회 후 DTO 변환
		return smsProviderRepository.findAllByOrderByDisplayOrder().stream()
			.map(SmsProviderResponseDto::from)
			.toList();
	}

	// 프로바이더 설정 수정
	@Transactional
	public SmsProviderResponseDto updateProvider(String id, UpdateSmsProviderRequestDto request)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// PK로 프로바이더 조회 (없으면 예외)
		//----------------------------------------------------------------------------------------------------------------------
		SmsProviderEntity provider = smsProviderRepository.findById(id)
			.orElseThrow(() -> new BusinessException(SmsErrorCode.SMS_PROVIDER_NOT_FOUND));

		//----------------------------------------------------------------------------------------------------------------------
		// 프로바이더 설정 업데이트
		//----------------------------------------------------------------------------------------------------------------------
		provider.update(
			request.getApiKey(),
			request.getApiSecret(),
			request.getSenderNumber(),
			request.getIsEnabled(),
			request.getConfigJson()
		);

		log.info("SMS 프로바이더 설정 수정: id={}, code={}, enabled={}",
			id, provider.getCode(), request.getIsEnabled());

		return SmsProviderResponseDto.from(provider);
	}
}
