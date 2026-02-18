package com.gizzi.core.domain.sms.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SmsErrorCode;
import com.gizzi.core.domain.audit.AuditAction;
import com.gizzi.core.domain.audit.AuditTarget;
import com.gizzi.core.domain.audit.service.AuditLogService;
import com.gizzi.core.domain.group.entity.GroupEntity;
import com.gizzi.core.domain.group.repository.GroupMemberRepository;
import com.gizzi.core.domain.group.repository.GroupRepository;
import com.gizzi.core.domain.sms.dto.GroupRecipientsDto;
import com.gizzi.core.domain.sms.dto.ManualSmsRequestDto;
import com.gizzi.core.domain.sms.dto.SmsSendResultDto;
import com.gizzi.core.domain.sms.dto.SmsRecipientCountDto;
import com.gizzi.core.domain.sms.entity.SmsLogEntity;
import com.gizzi.core.domain.sms.repository.SmsLogRepository;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 관리자 수동 SMS 대량 발송 서비스
// 수신 대상 해석 → 개별 발송 → 이력 저장을 담당한다
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ManualSmsService {

	// SMS 발송 오케스트레이션 서비스
	private final SmsService             smsService;

	// SMS 로그 리포지토리
	private final SmsLogRepository       smsLogRepository;

	// 사용자 리포지토리
	private final UserRepository         userRepository;

	// 그룹 리포지토리
	private final GroupRepository         groupRepository;

	// 그룹 멤버 리포지토리
	private final GroupMemberRepository  groupMemberRepository;

	// 감사 로그 서비스
	private final AuditLogService        auditLogService;

	// 수동 SMS 대량 발송
	// adminPk: 발송 요청 관리자 PK
	public SmsSendResultDto sendManualSms(String adminPk, ManualSmsRequestDto request) {
		// 수신 대상 유형에 따라 수신자 목록 해석
		List<UserEntity> recipients = resolveRecipients(request);

		// 수신자가 0명이면 에러
		if (recipients.isEmpty()) {
			throw new BusinessException(SmsErrorCode.SMS_NO_RECIPIENTS);
		}

		// 배치 ID 생성 (대량 발송 묶음 추적용)
		String batchId = UUID.randomUUID().toString();

		// 발송 카운터
		int successCount = 0;
		int failCount    = 0;

		// 개별 발송 루프
		for (UserEntity recipient : recipients) {
			// SMS 로그 엔티티 생성
			SmsLogEntity smsLog = SmsLogEntity.createManual(
				adminPk, recipient.getPhone(),
				recipient.getId(), request.getMessage(),
				null, batchId
			);

			try {
				// SMS 발송
				smsService.sendAndGetProviderCode(recipient.getPhone(), request.getMessage());

				// 발송 성공 처리
				smsLog.markSuccess();
				successCount++;
			} catch (Exception e) {
				// 발송 실패 처리 (다음 수신자 계속 발송)
				smsLog.markFailed(e.getMessage());
				failCount++;
				log.warn("수동 SMS 발송 실패: phone={}, error={}", recipient.getPhone(), e.getMessage());
			}

			// 로그 저장
			smsLogRepository.save(smsLog);
		}

		log.info("수동 SMS 대량 발송 완료: batchId={}, total={}, success={}, fail={}",
			batchId, recipients.size(), successCount, failCount);

		// 감사 로그: SMS 대량 발송 요약
		auditLogService.logSuccess(adminPk, AuditAction.SMS_BULK_SEND, AuditTarget.SMS, null,
			"수동 SMS 대량 발송", Map.of(
				"batchId", batchId,
				"recipientType", request.getRecipientType(),
				"totalCount", String.valueOf(recipients.size()),
				"successCount", String.valueOf(successCount),
				"failCount", String.valueOf(failCount)
			));

		// 결과 반환
		return SmsSendResultDto.builder()
			.batchId(batchId)
			.totalCount(recipients.size())
			.successCount(successCount)
			.failCount(failCount)
			.build();
	}

	// 수신 대상 유형에 따라 수신자 목록을 해석한다
	// 중복 제거: 여러 그룹에 동시 소속된 사용자는 한 번만 발송
	private List<UserEntity> resolveRecipients(ManualSmsRequestDto request) {
		String recipientType = request.getRecipientType();

		switch (recipientType) {
			case "ALL":
				// 전체 SMS 수신 가능 사용자
				return userRepository.findSmsEligibleUsers();

			case "GROUP":
				// 지정 그룹의 멤버 중 SMS 수신 가능 사용자
				if (request.getGroupIds() == null || request.getGroupIds().isEmpty()) {
					throw new BusinessException(SmsErrorCode.SMS_NO_RECIPIENTS);
				}
				// 그룹 멤버 사용자 ID 조회 (중복 제거)
				List<String> groupUserIds = groupMemberRepository
					.findDistinctUserIdsByGroupIds(request.getGroupIds());
				if (groupUserIds.isEmpty()) {
					return List.of();
				}
				// SMS 수신 가능 필터 적용
				return userRepository.findSmsEligibleUsersByIds(groupUserIds);

			case "INDIVIDUAL":
				// 개별 지정 사용자 중 SMS 수신 가능 사용자 (로그인 ID로 조회)
				if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
					throw new BusinessException(SmsErrorCode.SMS_NO_RECIPIENTS);
				}
				return userRepository.findSmsEligibleUsersByLoginIds(request.getUserIds());

			default:
				throw new BusinessException(SmsErrorCode.SMS_INVALID_RECIPIENT_TYPE);
		}
	}

	// 전체 SMS 수신 가능 회원 수 조회
	public SmsRecipientCountDto getRecipientCount() {
		long count = userRepository.countSmsEligibleUsers();
		return SmsRecipientCountDto.builder()
			.totalCount(count)
			.build();
	}

	// 그룹별 SMS 수신 가능 회원 수 조회
	public List<GroupRecipientsDto> getGroupRecipients() {
		// 전체 그룹 조회
		List<GroupEntity> groups = groupRepository.findAll();

		List<GroupRecipientsDto> result = new ArrayList<>();
		for (GroupEntity group : groups) {
			// 그룹 멤버 사용자 ID 목록 조회
			List<String> memberUserIds = groupMemberRepository
				.findDistinctUserIdsByGroupIds(List.of(group.getId()));

			// SMS 수신 가능 필터 적용 후 카운트
			long recipientCount = 0;
			if (!memberUserIds.isEmpty()) {
				recipientCount = userRepository.findSmsEligibleUsersByIds(memberUserIds).size();
			}

			result.add(GroupRecipientsDto.builder()
				.groupId(group.getId())
				.groupName(group.getName())
				.groupCode(group.getGroupCode())
				.recipientCount(recipientCount)
				.build());
		}

		return result;
	}
}
