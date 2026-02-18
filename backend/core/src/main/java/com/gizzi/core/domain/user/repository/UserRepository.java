package com.gizzi.core.domain.user.repository;

import com.gizzi.core.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 사용자 리포지토리 (tb_users 테이블 접근)
public interface UserRepository extends JpaRepository<UserEntity, String> {

	// 로그인 ID로 사용자 조회 (로그인 시 사용)
	Optional<UserEntity> findByUserId(String userId);

	// 로그인 ID로 사용자 존재 여부 확인 (회원가입 시 중복 검증)
	boolean existsByUserId(String userId);

	// 이메일로 사용자 존재 여부 확인 (회원가입 시 중복 검증)
	boolean existsByEmail(String email);

	// 특정 사용자를 제외한 이메일 중복 검증 (수정 시 자기 자신 제외)
	boolean existsByEmailAndIdNot(String email, String id);

	// 이메일로 사용자 조회 (소셜 로그인 시 기존 계정 병합 확인)
	Optional<UserEntity> findByEmail(String email);

	// SMS 수신 가능한 전체 사용자 조회 (phone 존재 + SMS 동의 + ACTIVE 상태)
	@Query("SELECT u FROM UserEntity u " +
		"WHERE u.phone IS NOT NULL " +
		"AND u.isSmsAgree = true " +
		"AND u.userStatus = 'ACTIVE'")
	List<UserEntity> findSmsEligibleUsers();

	// SMS 수신 가능한 전체 사용자 수 조회
	@Query("SELECT COUNT(u) FROM UserEntity u " +
		"WHERE u.phone IS NOT NULL " +
		"AND u.isSmsAgree = true " +
		"AND u.userStatus = 'ACTIVE'")
	long countSmsEligibleUsers();

	// 특정 사용자 PK 목록 중 SMS 수신 가능한 사용자만 조회
	@Query("SELECT u FROM UserEntity u " +
		"WHERE u.id IN :userIds " +
		"AND u.phone IS NOT NULL " +
		"AND u.isSmsAgree = true " +
		"AND u.userStatus = 'ACTIVE'")
	List<UserEntity> findSmsEligibleUsersByIds(@Param("userIds") List<String> userIds);

	// 로그인 ID 목록으로 SMS 수신 가능한 사용자만 조회 (개별 발송용)
	@Query("SELECT u FROM UserEntity u " +
		"WHERE u.userId IN :loginIds " +
		"AND u.phone IS NOT NULL " +
		"AND u.isSmsAgree = true " +
		"AND u.userStatus = 'ACTIVE'")
	List<UserEntity> findSmsEligibleUsersByLoginIds(@Param("loginIds") List<String> loginIds);
}
