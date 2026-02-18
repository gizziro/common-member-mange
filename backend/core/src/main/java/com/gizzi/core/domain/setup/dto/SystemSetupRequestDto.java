package com.gizzi.core.domain.setup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 시스템 초기 설정 — 최초 관리자 계정 생성 요청 DTO
// SignupRequestDto와 동일한 필드/검증이지만, 향후 셋업 전용 필드(사이트명 등) 확장을 위해 별도 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetupRequestDto {

	// 관리자 로그인 ID (4~50자, 영문과 숫자만 허용)
	@NotBlank(message = "아이디는 필수입니다")
	@Size(min = 4, max = 50, message = "아이디는 4~50자여야 합니다")
	@Pattern(regexp = "^[a-zA-Z0-9]{4,50}$", message = "아이디는 영문과 숫자만 사용 가능합니다")
	private String userId;

	// 관리자 비밀번호 (8~100자, 영문+숫자+특수문자 각 1자 이상 필수)
	@NotBlank(message = "비밀번호는 필수입니다")
	@Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
		message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다"
	)
	private String password;

	// 관리자 이름 (2~100자)
	@NotBlank(message = "이름은 필수입니다")
	@Size(min = 2, max = 100, message = "이름은 2~100자여야 합니다")
	private String username;

	// 관리자 이메일 주소
	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "올바른 이메일 형식이 아닙니다")
	private String email;

	// 관리자 전화번호 (선택)
	private String phone;

	// 전화번호 인증 검증 토큰 (OTP 인증 성공 시 발급받은 토큰)
	private String phoneVerificationToken;
}
