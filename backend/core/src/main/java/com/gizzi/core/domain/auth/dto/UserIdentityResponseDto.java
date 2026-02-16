package com.gizzi.core.domain.auth.dto;

import com.gizzi.core.domain.auth.entity.UserIdentityEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 소셜 연동 정보 응답 DTO
// 관리자 회원 상세 페이지 및 마이페이지에서 연동된 소셜 계정 표시용
@Getter
@Builder
public class UserIdentityResponseDto {

	// 연동 PK
	private final String        id;

	// 제공자 코드 (google, kakao, naver)
	private final String        providerCode;

	// 제공자 표시명
	private final String        providerName;

	// 제공자 측 사용자 고유 키
	private final String        providerSubject;

	// 연동 일시
	private final LocalDateTime linkedAt;

	// UserIdentityEntity → DTO 변환 팩토리
	public static UserIdentityResponseDto from(UserIdentityEntity entity) {
		return UserIdentityResponseDto.builder()
			.id(entity.getId())
			.providerCode(entity.getProvider().getCode())
			.providerName(entity.getProvider().getName())
			.providerSubject(entity.getProviderSubject())
			.linkedAt(entity.getCreatedAt())
			.build();
	}
}
