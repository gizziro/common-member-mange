package com.gizzi.admin.controller.setup;

import com.gizzi.core.common.dto.ApiResponseDto;
import com.gizzi.core.domain.setup.dto.SystemSetupRequestDto;
import com.gizzi.core.domain.setup.service.SystemInitService;
import com.gizzi.core.domain.user.dto.SignupResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 시스템 초기 설정 API 컨트롤러
// 최초 배포 시 관리자 계정을 생성하는 셋업 위자드 엔드포인트
@Slf4j
@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

	// 시스템 초기화 서비스
	private final SystemInitService systemInitService;

	// 시스템 초기화 상태 확인 API
	// 프론트엔드에서 셋업 위자드 표시 여부를 결정하는 데 사용
	@GetMapping("/status")
	public ResponseEntity<ApiResponseDto<Map<String, Boolean>>> getSetupStatus() {
		// 초기화 상태 조회
		boolean initialized = systemInitService.isInitialized();

		// 200 OK + initialized 상태 반환
		return ResponseEntity.ok(
			ApiResponseDto.ok(Map.of("initialized", initialized))
		);
	}

	// 최초 관리자 계정 생성 API
	// 시스템이 미초기화 상태일 때만 호출 가능
	@PostMapping("/initialize")
	public ResponseEntity<ApiResponseDto<SignupResponseDto>> initialize(@Valid @RequestBody SystemSetupRequestDto request)
	{
		// 관리자 계정 생성 서비스 호출
		SignupResponseDto response = systemInitService.setupAdmin(request);

		log.info("시스템 초기 설정 API 호출 완료: userId={}", request.getUserId());

		// 201 Created 응답 반환
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponseDto.ok(response));
	}
}
