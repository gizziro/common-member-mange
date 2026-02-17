package com.gizzi.admin.controller.setting;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.setting.dto.SettingGroupResponseDto;
import com.gizzi.core.domain.setting.dto.SettingResponseDto;
import com.gizzi.core.domain.setting.dto.UpdateSettingRequestDto;
import com.gizzi.core.domain.setting.dto.UpdateSettingsRequestDto;
import com.gizzi.core.domain.setting.entity.SettingEntity;
import com.gizzi.core.domain.setting.service.SettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 관리자 설정 관리 API 컨트롤러
// 시스템 설정 + 모듈 설정 CRUD
@Slf4j
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingController {

	// 설정 서비스
	private final SettingService settingService;

	// ─── 시스템 설정 ───

	// 시스템 설정 전체 조회 (그룹별 묶음)
	@GetMapping("/system")
	public ResponseEntity<ApiResponseDto<List<SettingGroupResponseDto>>> getSystemSettings() {
		// 시스템 모듈의 전체 설정을 그룹별로 조회
		Map<String, List<SettingEntity>> grouped = settingService.getSettingsByModule("system");

		// 그룹별 DTO 변환
		List<SettingGroupResponseDto> response = toGroupResponseList(grouped);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 시스템 특정 그룹 설정 조회
	@GetMapping("/system/{group}")
	public ResponseEntity<ApiResponseDto<SettingGroupResponseDto>> getSystemSettingGroup(
			@PathVariable String group) {
		// 시스템 모듈의 특정 그룹 설정 조회
		List<SettingEntity> settings = settingService.getSettingsByModuleAndGroup("system", group);

		// DTO 변환
		SettingGroupResponseDto response = SettingGroupResponseDto.builder()
				.group(group)
				.settings(settings.stream().map(SettingResponseDto::from).toList())
				.build();
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 시스템 특정 그룹 설정 일괄 수정
	@PutMapping("/system/{group}")
	public ResponseEntity<ApiResponseDto<Void>> updateSystemSettingGroup(
			@PathVariable String group,
			@Valid @RequestBody UpdateSettingsRequestDto request,
			Authentication authentication) {
		// 인증된 사용자 ID를 수정자로 기록
		String updatedBy = authentication.getName();

		// 요청을 key → value Map으로 변환
		Map<String, String> keyValueMap = request.getSettings().stream()
				.collect(Collectors.toMap(
						UpdateSettingRequestDto::getSettingKey,
						UpdateSettingRequestDto::getSettingValue
				));

		// 그룹 단위 일괄 수정
		settingService.updateGroupSettings("system", group, keyValueMap, updatedBy);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 모듈 설정 ───

	// 설정이 있는 모듈 코드 목록 조회
	@GetMapping("/modules")
	public ResponseEntity<ApiResponseDto<List<String>>> getModulesWithSettings() {
		List<String> moduleCodes = settingService.getAllModuleCodes();
		return ResponseEntity.ok(ApiResponseDto.ok(moduleCodes));
	}

	// 특정 모듈 설정 전체 조회 (그룹별 묶음)
	@GetMapping("/modules/{code}")
	public ResponseEntity<ApiResponseDto<List<SettingGroupResponseDto>>> getModuleSettings(
			@PathVariable String code) {
		// 모듈 코드의 전체 설정을 그룹별로 조회
		Map<String, List<SettingEntity>> grouped = settingService.getSettingsByModule(code);

		// 그룹별 DTO 변환
		List<SettingGroupResponseDto> response = toGroupResponseList(grouped);
		return ResponseEntity.ok(ApiResponseDto.ok(response));
	}

	// 특정 모듈 + 그룹 설정 일괄 수정
	@PutMapping("/modules/{code}/{group}")
	public ResponseEntity<ApiResponseDto<Void>> updateModuleSettingGroup(
			@PathVariable String code,
			@PathVariable String group,
			@Valid @RequestBody UpdateSettingsRequestDto request,
			Authentication authentication) {
		// 인증된 사용자 ID를 수정자로 기록
		String updatedBy = authentication.getName();

		// 요청을 key → value Map으로 변환
		Map<String, String> keyValueMap = request.getSettings().stream()
				.collect(Collectors.toMap(
						UpdateSettingRequestDto::getSettingKey,
						UpdateSettingRequestDto::getSettingValue
				));

		// 그룹 단위 일괄 수정
		settingService.updateGroupSettings(code, group, keyValueMap, updatedBy);
		return ResponseEntity.ok(ApiResponseDto.ok());
	}

	// ─── 헬퍼 ───

	// Map<String, List<SettingEntity>> → List<SettingGroupResponseDto> 변환
	private List<SettingGroupResponseDto> toGroupResponseList(Map<String, List<SettingEntity>> grouped) {
		List<SettingGroupResponseDto> result = new ArrayList<>();
		for (Map.Entry<String, List<SettingEntity>> entry : grouped.entrySet()) {
			result.add(SettingGroupResponseDto.builder()
					.group(entry.getKey())
					.settings(entry.getValue().stream().map(SettingResponseDto::from).toList())
					.build());
		}
		return result;
	}
}
