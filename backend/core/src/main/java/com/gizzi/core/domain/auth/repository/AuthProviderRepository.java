package com.gizzi.core.domain.auth.repository;

import com.gizzi.core.domain.auth.entity.AuthProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

//----------------------------------------------------------------------------------------------------------------------
// 인증 제공자 리포지토리 (tb_auth_providers 테이블 접근)
//----------------------------------------------------------------------------------------------------------------------
public interface AuthProviderRepository extends JpaRepository<AuthProviderEntity, String>
{
	// 제공자 코드로 조회 (예: "google", "kakao")
	Optional<AuthProviderEntity> findByCode(String code);

	// 활성화된 제공자 목록 조회 (표시 순서 정렬)
	List<AuthProviderEntity> findByIsEnabledTrueOrderByDisplayOrder();
}
