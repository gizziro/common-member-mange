package com.gizzi.core.module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

// 그룹별 권한 일괄 설정 요청 DTO
// 기존 권한을 모두 삭제 후 새로운 권한으로 재설정한다
@Getter
public class SetPermissionsRequestDto {

	// 권한을 설정할 그룹 ID
	@NotBlank(message = "그룹 ID는 필수입니다")
	private String groupId;

	// 부여할 권한 ID 목록 (빈 리스트 = 모든 권한 회수)
	@NotNull(message = "권한 ID 목록은 필수입니다")
	private List<String> permissionIds;
}
