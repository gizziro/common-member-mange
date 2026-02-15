package com.gizzi.core.common.exception;

import com.gizzi.core.common.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 모든 API 모듈에 적용되는 전역 예외 처리기
// core 모듈에 위치하며, scanBasePackages를 통해 admin-api/user-api 양쪽에서 자동 적용
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// BusinessException 처리 (비즈니스 로직 예외)
	@ExceptionHandler(BusinessException.class)
	protected ResponseEntity<ApiResponseDto<Void>> handleBusinessException(BusinessException e) {
		// 비즈니스 예외 로그 기록 (description 포함)
		ErrorCode errorCode = e.getErrorCode();
		log.warn("BusinessException: code={}, message={}, description={}",
			errorCode.getCode(), e.getMessage(), errorCode.getDescription());

		// ErrorCode에 정의된 HTTP 상태와 에러 응답 반환
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponseDto.error(errorCode, e.getMessage()));
	}

	// Bean Validation 실패 처리 (@Valid 검증 오류)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ApiResponseDto<Void>> handleValidationException(MethodArgumentNotValidException e) {
		// 첫 번째 필드 에러의 메시지를 추출
		String message = e.getBindingResult()
			.getFieldErrors()
			.stream()
			.findFirst()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.orElse("유효하지 않은 입력값");

		// 검증 실패 로그 기록
		log.warn("Validation failed: {}", message);

		// 400 Bad Request + 검증 오류 메시지 반환
		return ResponseEntity
			.status(CommonErrorCode.INVALID_INPUT.getHttpStatus())
			.body(ApiResponseDto.error(CommonErrorCode.INVALID_INPUT, message));
	}

	// 존재하지 않는 리소스 접근 처리 (404)
	@ExceptionHandler(NoResourceFoundException.class)
	protected ResponseEntity<ApiResponseDto<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
		// 404 로그 기록
		log.warn("Resource not found: {}", e.getMessage());

		// 404 Not Found 응답 반환
		return ResponseEntity
			.status(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
			.body(ApiResponseDto.error(CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	// 허용되지 않은 HTTP 메서드 처리 (405)
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	protected ResponseEntity<ApiResponseDto<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
		// 405 로그 기록
		log.warn("Method not allowed: {}", e.getMessage());

		// 405 Method Not Allowed 응답 반환
		return ResponseEntity
			.status(CommonErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
			.body(ApiResponseDto.error(CommonErrorCode.METHOD_NOT_ALLOWED));
	}

	// 기타 예상치 못한 예외 처리 (500)
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ApiResponseDto<Void>> handleException(Exception e) {
		// 예상치 못한 예외는 ERROR 레벨로 로그 기록
		log.error("Unhandled exception: ", e);

		// 500 Internal Server Error 응답 반환
		return ResponseEntity
			.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
			.body(ApiResponseDto.error(CommonErrorCode.INTERNAL_SERVER_ERROR));
	}
}
