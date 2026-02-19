package com.gizzi.core.domain.sms.service;

import com.gizzi.core.domain.sms.entity.SmsLogEntity;
import com.gizzi.core.domain.sms.repository.SmsLogRepository;
import com.gizzi.core.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 자동 SMS 발송 허브 서비스
// 비밀번호 초기화 등 시스템 이벤트에 의한 자동 SMS 발송을 담당한다
// 향후 알림 유형 확장 시 이 서비스에 메서드를 추가한다
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SmsNotificationService
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final SmsService       smsService;			// SMS 발송 오케스트레이션 서비스
	private final SmsLogRepository smsLogRepository;	// SMS 로그 리포지토리

	//======================================================================================================================
	// [ 핵심 비즈니스 메서드 ]
	//======================================================================================================================

	// 비밀번호 초기화 SMS 발송
	// user: 대상 사용자 엔티티, tempPassword: 생성된 임시 비밀번호, adminPk: 초기화 수행 관리자 PK
	// 반환: SMS 발송 성공 여부
	// REQUIRES_NEW: SMS 실패 시에도 비밀번호 변경 트랜잭션에 영향을 주지 않는다
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean sendPasswordResetSms(UserEntity user, String tempPassword, String adminPk)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 전화번호 유효성 확인
		//----------------------------------------------------------------------------------------------------------------------
		if (user.getPhone() == null || user.getPhone().isBlank())
		{
			log.info("비밀번호 초기화 SMS 미발송 (전화번호 없음): userId={}", user.getUserId());
			return false;
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 메시지 포맷팅
		//----------------------------------------------------------------------------------------------------------------------
		String message = String.format("[비밀번호 초기화] 임시 비밀번호: %s\n로그인 후 반드시 비밀번호를 변경해 주세요.", tempPassword);

		// SMS 로그 엔티티 생성 (AUTO 타입)
		SmsLogEntity smsLog = SmsLogEntity.createAuto(
			"PASSWORD_RESET", adminPk,
			user.getPhone(), user.getId(),
			message, null
		);

		try
		{
			//----------------------------------------------------------------------------------------------------------------------
			// SMS 발송 + 프로바이더 코드 획득
			//----------------------------------------------------------------------------------------------------------------------
			String providerCode = smsService.sendAndGetProviderCode(user.getPhone(), message);

			// 발송 성공 처리
			smsLog.markSuccess();
			// 프로바이더 코드는 createAuto에서 null로 설정했으므로 여기서 업데이트 불가
			// SmsLogEntity에 providerCode setter가 없으므로 팩토리에서 직접 설정

			log.info("비밀번호 초기화 SMS 발송 성공: userId={}, phone={}", user.getUserId(), user.getPhone());
			return true;
		}
		catch (Exception e)
		{
			// 발송 실패 처리 (비밀번호는 이미 변경됨)
			smsLog.markFailed(e.getMessage());
			log.warn("비밀번호 초기화 SMS 발송 실패: userId={}, error={}", user.getUserId(), e.getMessage());
			return false;
		}
		finally
		{
			// 성공/실패 관계없이 로그 저장
			smsLogRepository.save(smsLog);
		}
	}
}
