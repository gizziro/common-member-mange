package com.gizzi.core.domain.audit.service;

import com.gizzi.core.domain.audit.ActorType;
import com.gizzi.core.domain.audit.ResultStatus;
import com.gizzi.core.domain.audit.dto.AuditLogResponseDto;
import com.gizzi.core.domain.audit.entity.AuditLogEntity;
import com.gizzi.core.domain.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

// 감사 로그 서비스 — 감사 로그 기록 및 조회
// log(): 현재 트랜잭션에 참여 (성공 이벤트용)
// logIndependent(): REQUIRES_NEW 독립 트랜잭션 (실패 이벤트용 — 외부 롤백 시에도 커밋)
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

	// 감사 로그 리포지토리
	private final AuditLogRepository auditLogRepository;

	// 현재 트랜잭션에 참여하여 감사 로그 기록 (성공 이벤트용)
	@Transactional
	public void log(String actorUserId, String actorType,
	                String actionType, String targetType, String targetId,
	                String resultStatus, String message, Map<String, Object> metadata) {
		// 요청 컨텍스트에서 IP/UserAgent 자동 추출
		String[] requestInfo = extractRequestInfo();

		// 메타데이터 Map → JSON 문자열 변환
		String metadataJson = mapToJson(metadata);

		// 감사 로그 엔티티 생성 + 저장
		AuditLogEntity entity = AuditLogEntity.create(
			actorUserId, actorType, actionType, targetType, targetId,
			resultStatus, message, metadataJson,
			requestInfo[0], requestInfo[1]
		);
		auditLogRepository.save(entity);

		log.debug("감사 로그 기록: action={}, actor={}, target={}/{}",
			actionType, actorUserId, targetType, targetId);
	}

	// 독립 트랜잭션으로 감사 로그 기록 (실패 이벤트용 — 외부 롤백과 무관하게 커밋)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logIndependent(String actorUserId, String actorType,
	                           String actionType, String targetType, String targetId,
	                           String resultStatus, String message, Map<String, Object> metadata) {
		// 요청 컨텍스트에서 IP/UserAgent 자동 추출
		String[] requestInfo = extractRequestInfo();

		// 메타데이터 Map → JSON 문자열 변환
		String metadataJson = mapToJson(metadata);

		// 감사 로그 엔티티 생성 + 저장
		AuditLogEntity entity = AuditLogEntity.create(
			actorUserId, actorType, actionType, targetType, targetId,
			resultStatus, message, metadataJson,
			requestInfo[0], requestInfo[1]
		);
		auditLogRepository.save(entity);

		log.debug("감사 로그 기록(독립 tx): action={}, actor={}, target={}/{}",
			actionType, actorUserId, targetType, targetId);
	}

	// 성공 이벤트 간편 메서드 (USER 행위자)
	@Transactional
	public void logSuccess(String actorUserId, String actionType,
	                       String targetType, String targetId,
	                       String message, Map<String, Object> metadata) {
		log(actorUserId, ActorType.USER.name(), actionType, targetType, targetId,
			ResultStatus.SUCCESS.name(), message, metadata);
	}

	// 실패 이벤트 간편 메서드 (USER 행위자, 독립 트랜잭션)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logFailure(String actorUserId, String actionType,
	                       String targetType, String targetId,
	                       String message, Map<String, Object> metadata) {
		logIndependent(actorUserId, ActorType.USER.name(), actionType, targetType, targetId,
			ResultStatus.FAILURE.name(), message, metadata);
	}

	// 감사 로그 목록 조회 (복합 필터 + 페이지네이션)
	@Transactional(readOnly = true)
	public Page<AuditLogResponseDto> getAuditLogs(String actorUserId, String actionType,
	                                              LocalDateTime startTime, LocalDateTime endTime,
	                                              Pageable pageable) {
		// 복합 필터로 조회 → DTO 변환
		return auditLogRepository.findByFilters(actorUserId, actionType, startTime, endTime, pageable)
			.map(AuditLogResponseDto::from);
	}

	// 현재 HTTP 요청에서 IP 주소와 User-Agent 추출
	// 웹 컨텍스트가 없으면 [null, null] 반환
	private String[] extractRequestInfo() {
		try {
			// RequestContextHolder에서 현재 요청 가져오기
			ServletRequestAttributes attrs =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs != null) {
				HttpServletRequest request = attrs.getRequest();
				// X-Forwarded-For 헤더 우선, 없으면 remoteAddr
				String ip = request.getHeader("X-Forwarded-For");
				if (ip == null || ip.isEmpty()) {
					ip = request.getRemoteAddr();
				}
				String userAgent = request.getHeader("User-Agent");
				// User-Agent 255자 제한 (DB 컬럼 크기)
				if (userAgent != null && userAgent.length() > 255) {
					userAgent = userAgent.substring(0, 255);
				}
				return new String[]{ip, userAgent};
			}
		} catch (Exception e) {
			log.debug("요청 컨텍스트에서 정보 추출 실패 (비웹 환경): {}", e.getMessage());
		}
		return new String[]{null, null};
	}

	// Map<String, Object> → JSON 문자열 변환 (수동 빌더, ObjectMapper 미사용)
	// core 모듈은 ObjectMapper 빈에 직접 접근할 수 없으므로 수동으로 JSON을 생성한다
	private String mapToJson(Map<String, Object> metadata) {
		// null 또는 빈 맵이면 null 반환
		if (metadata == null || metadata.isEmpty()) {
			return null;
		}

		// JSON 객체 빌더
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<String, Object> entry : metadata.entrySet()) {
			if (!first) {
				sb.append(",");
			}
			first = false;

			// 키는 항상 문자열
			sb.append("\"").append(escapeJson(entry.getKey())).append("\":");

			// 값의 타입에 따라 JSON 직렬화
			Object value = entry.getValue();
			if (value == null) {
				sb.append("null");
			} else if (value instanceof Number || value instanceof Boolean) {
				sb.append(value);
			} else {
				sb.append("\"").append(escapeJson(value.toString())).append("\"");
			}
		}
		sb.append("}");
		return sb.toString();
	}

	// JSON 문자열 이스케이프 (XSS 방지 + 올바른 JSON 생성)
	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}
}
