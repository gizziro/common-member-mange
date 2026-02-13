package com.gizzi.core.domain.user.repository;

import com.gizzi.core.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// 사용자 리포지토리 (tb_users 테이블 접근)
public interface UserRepository extends JpaRepository<User, String> {

	// 로그인 ID로 사용자 존재 여부 확인 (회원가입 시 중복 검증)
	boolean existsByUserId(String userId);

	// 이메일로 사용자 존재 여부 확인 (회원가입 시 중복 검증)
	boolean existsByEmail(String email);
}
