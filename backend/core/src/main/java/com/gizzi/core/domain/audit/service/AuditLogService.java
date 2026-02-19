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
public class AuditLogService
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 감사 로그 리포지토리
	private final AuditLogRepository auditLogRepository;

	//======================================================================================================================
	// [ 핵심 로그 기록 메서드 ]
	//======================================================================================================================

	// 현재 트랜잭션에 참여하여 감사 로그 기록 (성공 이벤트용)
	@Transactional
	public void log(String actorUserId, String actorType,
	                String actionType, String targetType, String targetId,
	                String resultStatus, String message, Map<String, Object> metadata)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 요청 컨텍스트에서 IP/UserAgent 자동 추출
		//----------------------------------------------------------------------------------------------------------------------
		String[] requestInfo = extractRequestInfo();

		//----------------------------------------------------------------------------------------------------------------------
		// 메타데이터 Map → JSON 문자열 변환
		//----------------------------------------------------------------------------------------------------------------------
		String metadataJson = mapToJson(metadata);

		//----------------------------------------------------------------------------------------------------------------------
		// 감사 로그 엔티티 생성 + 저장
		//----------------------------------------------------------------------------------------------------------------------
		AuditLogEntity entity = AuditLogEntity.create(
			actorUserId, actorType, actionType, targetType, targetId,
			resultStatus, message, metadataJson,
			requestInfo[0], requestInfo[1]
		);
		auditLogRepository.save(entity);

		// 디버그 로그 출력
		log.debug("감사 로그 기록: action={}, actor={}, target={}/{}",
			actionType, actorUserId, targetType, targetId);
	}

	// 독립 트랜잭션으로 감사 로그 기록 (실패 이벤트용 — 외부 롤백과 무관하게 커밋)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logIndependent(String actorUserId, String actorType,
	                           String actionType, String targetType, String targetId,
	                           String resultStatus, String message, Map<String, Object> metadata)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 요청 컨텍스트에서 IP/UserAgent 자동 추출
		//----------------------------------------------------------------------------------------------------------------------
		String[] requestInfo = extractRequestInfo();

		//----------------------------------------------------------------------------------------------------------------------
		// 메타데이터 Map → JSON 문자열 변환
		//----------------------------------------------------------------------------------------------------------------------
		String metadataJson = mapToJson(metadata);

		//----------------------------------------------------------------------------------------------------------------------
		// 감사 로그 엔티티 생성 + 저장
		//----------------------------------------------------------------------------------------------------------------------
		AuditLogEntity entity = AuditLogEntity.create(
			actorUserId, actorType, actionType, targetType, targetId,
			resultStatus, message, metadataJson,
			requestInfo[0], requestInfo[1]
		);
		auditLogRepository.save(entity);

		// 디버그 로그 출력
		log.debug("감사 로그 기록(독립 tx): action={}, actor={}, target={}/{}",
			actionType, actorUserId, targetType, targetId);
	}

	//======================================================================================================================
	// [ 간편 로그 기록 메서드 ]
	//======================================================================================================================

	// 성공 이벤트 간편 메서드 (USER 행위자)
	@Transactional
	public void logSuccess(String actorUserId, String actionType,
	                       String targetType, String targetId,
	                       String message, Map<String, Object> metadata)
	{
		// USER 행위자 + SUCCESS 상태로 로그 기록 위임
		log(actorUserId, ActorType.USER.name(), actionType, targetType, targetId,
			ResultStatus.SUCCESS.name(), message, metadata);
	}

	// 실패 이벤트 간편 메서드 (USER 행위자, 독립 트랜잭션)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logFailure(String actorUserId, String actionType,
	                       String targetType, String targetId,
	                       String message, Map<String, Object> metadata)
	{
		// USER 행위자 + FAILURE 상태로 독립 트랜잭션 로그 기록 위임
		logIndependent(actorUserId, ActorType.USER.name(), actionType, targetType, targetId,
			ResultStatus.FAILURE.name(), message, metadata);
	}

	//======================================================================================================================
	// [ 조회 메서드 ]
	//======================================================================================================================

	// 감사 로그 목록 조회 (복합 필터 + 페이지네이션)
	@Transactional(readOnly = true)
	public Page<AuditLogResponseDto> getAuditLogs(String actorUserId, String actionType,
	                                              LocalDateTime startTime, LocalDateTime endTime,
	                                              Pageable pageable)
	{
		// 복합 필터로 조회 → DTO 변환
		return auditLogRepository.findByFilters(actorUserId, actionType, startTime, endTime, pageable)
			.map(AuditLogResponseDto::from);
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ 요청 컨텍스트 추출 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 현재 HTTP 요청에서 IP 주소와 User-Agent 추출
	// 웹 컨텍스트가 없으면 [null, null] 반환
	private String[] extractRequestInfo()
	{
		try
		{
			// RequestContextHolder에서 현재 요청 가져오기
			ServletRequestAttributes attrs =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

			// 요청 속성이 존재하면 IP/UserAgent 추출
			if (attrs != null)
			{
				HttpServletRequest request = attrs.getRequest();

				// X-Forwarded-For 헤더 우선, 없으면 remoteAddr 사용
				String ip = request.getHeader("X-Forwarded-For");
				if (ip == null || ip.isEmpty())
				{
					ip = request.getRemoteAddr();
				}

				// User-Agent 헤더 추출
				String userAgent = request.getHeader("User-Agent");

				// User-Agent 255자 제한 (DB 컬럼 크기)
				if (userAgent != null && userAgent.length() > 255)
				{
					userAgent = userAgent.substring(0, 255);
				}

				return new String[]{ip, userAgent};
			}
		}
		catch (Exception e)
		{
			// 비웹 환경 (스케줄러, 테스트 등) 에서는 무시
			log.debug("요청 컨텍스트에서 정보 추출 실패 (비웹 환경): {}", e.getMessage());
		}

		// 웹 컨텍스트 없으면 null 배열 반환
		return new String[]{null, null};
	}

	//----------------------------------------------------------------------------------------------------------------------
	// [ JSON 변환 유틸리티 ]
	//----------------------------------------------------------------------------------------------------------------------

	// Map<String, Object> → JSON 문자열 변환 (수동 빌더, ObjectMapper 미사용)
	// core 모듈은 ObjectMapper 빈에 직접 접근할 수 없으므로 수동으로 JSON을 생성한다
	private String mapToJson(Map<String, Object> metadata)
	{
		// null 또는 빈 맵이면 null 반환
		if (metadata == null || metadata.isEmpty())
		{
			return null;
		}

		// JSON 객체 빌더 초기화
		StringBuilder sb	= new StringBuilder("{");
		boolean first		= true;

		// 모든 엔트리를 순회하며 JSON 키-값 쌍 생성
		for (Map.Entry<String, Object> entry : metadata.entrySet())
		{
			// 첫 번째 항목이 아니면 구분자 추가
			if (!first)
			{
				sb.append(",");
			}
			first = false;

			// 키는 항상 문자열로 이스케이프 처리
			sb.append("\"").append(escapeJson(entry.getKey())).append("\":");

			// 값의 타입에 따라 JSON 직렬화 분기
			Object value = entry.getValue();
			if (value == null)
			{
				// null 값
				sb.append("null");
			}
			else if (value instanceof Number || value instanceof Boolean)
			{
				// 숫자/불리언은 따옴표 없이 출력
				sb.append(value);
			}
			else
			{
				// 문자열은 따옴표로 감싸고 이스케이프 처리
				sb.append("\"").append(escapeJson(value.toString())).append("\"");
			}
		}

		// JSON 객체 닫기
		sb.append("}");
		return sb.toString();
	}

	// JSON 문자열 이스케이프 (XSS 방지 + 올바른 JSON 생성)
	private String escapeJson(String value)
	{
		// null이면 빈 문자열 반환
		if (value == null)
		{
			return "";
		}

		// 특수 문자를 JSON 이스케이프 시퀀스로 치환
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}
}
