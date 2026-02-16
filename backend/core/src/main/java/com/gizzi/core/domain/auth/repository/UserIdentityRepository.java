package com.gizzi.core.domain.auth.repository;

import com.gizzi.core.domain.auth.entity.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 소셜 로그인 연동 리포지토리 (tb_user_identities 테이블 접근)
public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, String> {

	// 제공자 코드 + 제공자 사용자 키로 연동 조회 (소셜 로그인 시 기존 연동 확인)
	@Query("SELECT ui FROM UserIdentityEntity ui " +
		"JOIN ui.provider p " +
		"WHERE p.code = :providerCode AND ui.providerSubject = :providerSubject")
	Optional<UserIdentityEntity> findByProviderCodeAndProviderSubject(
		@Param("providerCode") String providerCode,
		@Param("providerSubject") String providerSubject);

	// 사용자 PK로 소셜 연동 목록 조회
	@Query("SELECT ui FROM UserIdentityEntity ui WHERE ui.user.id = :userId")
	List<UserIdentityEntity> findByUserId(@Param("userId") String userId);

	// 사용자 PK로 소셜 연동 전체 삭제 (회원 삭제 시 명시적 정리)
	@Modifying
	@Query("DELETE FROM UserIdentityEntity ui WHERE ui.user.id = :userId")
	void deleteByUserId(@Param("userId") String userId);
}
