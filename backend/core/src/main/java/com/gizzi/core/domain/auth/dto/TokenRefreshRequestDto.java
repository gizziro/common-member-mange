package com.gizzi.core.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

//----------------------------------------------------------------------------------------------------------------------
// 토큰 갱신 요청 DTO
//----------------------------------------------------------------------------------------------------------------------
@Getter
@NoArgsConstructor
public class TokenRefreshRequestDto
{
	// [ 필드 ]
	//----------------------------------------------------------------------------------------------------------------------
	@NotBlank(message = "리프레시 토큰은 필수입니다")
	private String refreshToken;	// 갱신에 사용할 Refresh Token
}
