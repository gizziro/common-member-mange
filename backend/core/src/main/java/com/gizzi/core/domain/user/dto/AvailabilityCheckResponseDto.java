package com.gizzi.core.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 아이디/이메일 중복 확인 응답 DTO
@Getter
@AllArgsConstructor
public class AvailabilityCheckResponseDto {

	// 사용 가능 여부 (true: 사용 가능, false: 이미 사용 중)
	private final boolean available;

	// 사용 가능 응답 생성
	public static AvailabilityCheckResponseDto available() {
		return new AvailabilityCheckResponseDto(true);
	}

	// 사용 불가 응답 생성
	public static AvailabilityCheckResponseDto unavailable() {
		return new AvailabilityCheckResponseDto(false);
	}
}
