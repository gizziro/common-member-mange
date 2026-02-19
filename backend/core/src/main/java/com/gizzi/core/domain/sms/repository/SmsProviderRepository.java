package com.gizzi.core.domain.sms.repository;

import com.gizzi.core.domain.sms.entity.SmsProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// SMS 프로바이더 리포지토리 (tb_sms_providers 테이블 접근)
public interface SmsProviderRepository extends JpaRepository<SmsProviderEntity, String>
{
	//----------------------------------------------------------------------------------------------------------------------
	// 프로바이더 코드로 조회 (예: "solapi", "aws_sns")
	//----------------------------------------------------------------------------------------------------------------------
	Optional<SmsProviderEntity> findByCode(String code);

	//----------------------------------------------------------------------------------------------------------------------
	// 활성화된 프로바이더 목록 조회 (표시 순서 정렬)
	//----------------------------------------------------------------------------------------------------------------------
	List<SmsProviderEntity> findByIsEnabledTrueOrderByDisplayOrder();

	//----------------------------------------------------------------------------------------------------------------------
	// 전체 프로바이더 목록 조회 (표시 순서 정렬)
	//----------------------------------------------------------------------------------------------------------------------
	List<SmsProviderEntity> findAllByOrderByDisplayOrder();
}
