package com.gizzi.core.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

//----------------------------------------------------------------------------------------------------------------------
// 소셜 연동 확인 요청 DTO
// 기존 계정이 발견되었을 때 사용자가 로컬 ID/PW로 본인 확인 후 연동을 완료한다
//----------------------------------------------------------------------------------------------------------------------
@Getter
@NoArgsConstructor
public class OAuth2LinkConfirmRequestDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	@NotBlank(message = "연동 식별자는 필수입니다")
	private String pendingId;	// 연동 대기 식별자 (Redis에서 소셜 정보 조회용)

	@NotBlank(message = "아이디는 필수입니다")
	private String userId;		// 로컬 로그인 ID

	@NotBlank(message = "비밀번호는 필수입니다")
	private String password;	// 로컬 비밀번호
}
