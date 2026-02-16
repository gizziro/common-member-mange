package com.gizzi.core.domain.user.entity;

import com.gizzi.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 사용자 엔티티 (tb_users 테이블 매핑)
// 로컬/소셜 회원가입, 인증, 계정 잠금 등 사용자 핵심 정보를 관리한다
@Entity
@Table(name = "tb_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

	// 사용자 PK (UUID)
	@Id
	@Column(name = "id", length = 36)
	private String id;

	// 로그인 ID
	@Column(name = "user_id", nullable = false, length = 50)
	private String userId;

	// 사용자 이름
	@Column(name = "username", nullable = false, length = 100)
	private String username;

	// 가입/로그인 제공자 (LOCAL, GOOGLE, KAKAO 등)
	@Column(name = "provider", nullable = false, length = 20)
	private String provider;

	// 소셜 제공자 사용자 ID (로컬 가입 시 userId와 동일)
	@Column(name = "provider_id", nullable = false, length = 255)
	private String providerId;

	// 비밀번호 해시 (BCrypt)
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	// 비밀번호 변경 일시
	@Column(name = "password_change_date", nullable = false)
	private LocalDateTime passwordChangeDate;

	// 이메일 주소
	@Column(name = "email", nullable = false, length = 320)
	private String email;

	// 이메일 인증 토큰
	@Column(name = "email_token", length = 255)
	private String emailToken;

	// 이메일 인증 여부
	@Column(name = "email_verified", nullable = false)
	private Boolean emailVerified;

	// 전화번호
	@Column(name = "phone", length = 20)
	private String phone;

	// 전화번호 인증 여부
	@Column(name = "phone_verified", nullable = false)
	private Boolean phoneVerified;

	// 소셜 가입 인증 여부
	@Column(name = "social_join_verified", nullable = false)
	private Boolean socialJoinVerified;

	// 소셜 가입 인증 토큰
	@Column(name = "social_join_token", length = 255)
	private String socialJoinToken;

	// OTP 사용 여부
	@Column(name = "is_otp_use", nullable = false)
	private Boolean isOtpUse;

	// OTP 비밀키 (암호화 저장)
	@Column(name = "otp_secret", length = 255)
	private String otpSecret;

	// 로그인 실패 횟수
	@Column(name = "login_fail_count", nullable = false)
	private Integer loginFailCount;

	// 계정 잠금 여부
	@Column(name = "is_locked", nullable = false)
	private Boolean isLocked;

	// 계정 잠금 일시
	@Column(name = "locked_at")
	private LocalDateTime lockedAt;

	// 사용자 상태 (PENDING, ACTIVE, SUSPENDED)
	@Column(name = "user_status", nullable = false, length = 20)
	private String userStatus;

	// 생성자 (최초 가입 처리자)
	@Column(name = "created_by", nullable = false, length = 100)
	private String createdBy;

	// 수정자
	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	// 엔티티 저장 전 UUID PK 자동 생성
	@PrePersist
	private void generateId() {
		// ID가 없을 때만 새로 생성 (수동 설정된 경우 유지)
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}

	// 최대 로그인 실패 허용 횟수 (초과 시 계정 잠금)
	private static final int MAX_LOGIN_FAIL_COUNT = 5;

	// 로컬 회원가입용 정적 팩토리 메서드
	public static UserEntity createLocalUser(String userId, String username,
	                                         String email, String encodedPassword) {
		// 새 사용자 엔티티 생성
		UserEntity user          = new UserEntity();
		user.userId              = userId;
		user.username            = username;
		user.email               = email;
		user.passwordHash        = encodedPassword;
		user.provider            = "LOCAL";
		user.providerId          = userId;
		user.passwordChangeDate  = LocalDateTime.now();
		user.emailVerified       = false;
		user.phoneVerified       = false;
		user.socialJoinVerified  = false;
		user.isOtpUse            = false;
		user.loginFailCount      = 0;
		user.isLocked            = false;
		user.userStatus          = "PENDING";
		user.createdBy           = userId;
		return user;
	}

	// 소셜 회원가입용 정적 팩토리 메서드
	// providerCode: 소셜 제공자 코드 (GOOGLE, KAKAO, NAVER)
	// providerSubject: 소셜 제공자 사용자 고유 키
	public static UserEntity createSocialUser(String providerCode, String providerSubject,
	                                          String email, String username, String encodedRandomPassword) {
		// 새 소셜 사용자 엔티티 생성
		UserEntity user          = new UserEntity();
		// 로그인 ID: {provider}_{subject} (소셜 사용자 고유 ID)
		user.userId              = providerCode.toLowerCase() + "_" + providerSubject;
		user.username            = username;
		user.email               = email;
		// 소셜 로그인은 비밀번호를 직접 사용하지 않으므로 랜덤 해시 설정
		user.passwordHash        = encodedRandomPassword;
		user.provider            = providerCode.toUpperCase();
		user.providerId          = providerSubject;
		user.passwordChangeDate  = LocalDateTime.now();
		// 소셜 로그인 사용자는 이메일 인증 처리 완료로 간주
		user.emailVerified       = true;
		user.phoneVerified       = false;
		user.socialJoinVerified  = true;
		user.isOtpUse            = false;
		user.loginFailCount      = 0;
		user.isLocked            = false;
		// 소셜 로그인 사용자는 바로 ACTIVE 상태
		user.userStatus          = "ACTIVE";
		user.createdBy           = providerCode.toLowerCase();
		return user;
	}

	// 로그인 실패 횟수 증가 + 최대 횟수 초과 시 자동 잠금
	public void incrementLoginFailCount() {
		// 실패 횟수 1 증가
		this.loginFailCount++;
		// 최대 실패 횟수 이상이면 계정 잠금
		if (this.loginFailCount >= MAX_LOGIN_FAIL_COUNT) {
			this.isLocked = true;
			this.lockedAt = LocalDateTime.now();
		}
	}

	// 로그인 성공 시 실패 횟수 초기화
	public void resetLoginFailCount() {
		// 실패 횟수를 0으로 리셋
		this.loginFailCount = 0;
	}

	// 계정 잠금 해제
	public void unlock() {
		// 잠금 상태 및 잠금 일시 초기화
		this.isLocked = false;
		this.lockedAt = null;
		// 실패 횟수도 함께 초기화
		this.loginFailCount = 0;
	}

	// 사용자 상태를 ACTIVE로 변경
	public void activate() {
		// 사용자 상태를 활성으로 전환
		this.userStatus = "ACTIVE";
	}

	// 관리자에 의한 사용자 정보 수정
	public void updateByAdmin(String username, String email, String userStatus) {
		// 사용자 이름 변경
		this.username   = username;
		// 이메일 변경
		this.email      = email;
		// 사용자 상태 변경
		this.userStatus = userStatus;
	}

	// 비밀번호 변경 (관리자 또는 본인)
	public void changePassword(String encodedPassword) {
		// 새 비밀번호 해시로 교체
		this.passwordHash       = encodedPassword;
		// 비밀번호 변경 일시 갱신
		this.passwordChangeDate = LocalDateTime.now();
	}

	// 소셜 전용 사용자에게 로컬 자격증명 설정 (로컬 ID + 비밀번호)
	// 설정 후 provider가 LOCAL로 변경되어 소셜 연동 추가/해제가 가능해진다
	public void setLocalCredentials(String newUserId, String encodedPassword) {
		// 로컬 로그인 ID 설정
		this.userId             = newUserId;
		// 비밀번호 해시 설정
		this.passwordHash       = encodedPassword;
		// 제공자를 LOCAL로 전환
		this.provider           = "LOCAL";
		// providerId도 로컬 ID와 동일하게 설정
		this.providerId         = newUserId;
		// 비밀번호 변경 일시 갱신
		this.passwordChangeDate = LocalDateTime.now();
	}
}
