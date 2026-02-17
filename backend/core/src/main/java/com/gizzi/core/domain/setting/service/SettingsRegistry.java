package com.gizzi.core.domain.setting.service;

import com.gizzi.core.domain.setting.entity.SettingEntity;
import com.gizzi.core.domain.setting.entity.SettingValueType;
import com.gizzi.core.domain.setting.repository.SettingRepository;
import com.gizzi.core.module.ModuleDefinition;
import com.gizzi.core.module.dto.SettingDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 설정 자동 등록 레지스트리 — 앱 시작 시 시스템/모듈 기본 설정을 DB에 등록
// 실행 순서: @Order(3) — ModuleSchemaInitializer(1) → ModuleRegistry(2) → SettingsRegistry(3)
// DB에 이미 존재하는 설정은 스킵하여 관리자가 수정한 값을 보존한다
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class SettingsRegistry implements ApplicationRunner {

	// 설정 리포지토리
	private final SettingRepository settingRepository;

	// 설정 캐시
	private final SettingCache settingCache;

	// 등록된 모든 모듈 정의 (Spring이 자동 주입, 없으면 빈 리스트)
	private final List<ModuleDefinition> moduleDefinitions;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		log.info("설정 레지스트리 초기화 시작");

		// 1. 시스템 기본 설정 등록
		int systemCount = registerSystemDefaults();
		log.info("시스템 기본 설정 등록: {}개", systemCount);

		// 2. 모듈별 기본 설정 등록
		for (ModuleDefinition definition : moduleDefinitions) {
			List<SettingDefinition> moduleSettings = definition.getDefaultSettings();
			if (!moduleSettings.isEmpty()) {
				int count = registerModuleDefaults(definition.getCode(), moduleSettings);
				log.info("모듈 [{}] 기본 설정 등록: {}개", definition.getCode(), count);
			}
		}

		// 3. 전체 설정 캐시 로딩
		List<SettingEntity> allSettings = settingRepository.findAll();
		settingCache.loadAll(allSettings);

		log.info("설정 레지스트리 초기화 완료: 총 {}개 설정 캐시 로딩", allSettings.size());
	}

	// 시스템 기본 설정 등록 (하드코딩 목록)
	private int registerSystemDefaults() {
		int count = 0;

		// ─── general 그룹: 사이트 기본 정보 ───
		count += registerIfAbsent("system", "general", "site_name",
				"Common CMS", SettingValueType.STRING,
				"사이트 이름", "사이트 제목으로 사용됩니다", false, 0);
		count += registerIfAbsent("system", "general", "site_description",
				"", SettingValueType.STRING,
				"사이트 설명", "사이트 부제목/설명으로 사용됩니다", false, 1);

		// ─── signup 그룹: 회원가입 설정 ───
		count += registerIfAbsent("system", "signup", "enabled",
				"true", SettingValueType.BOOLEAN,
				"회원가입 허용", "비활성화 시 회원가입 API가 차단됩니다", false, 0);
		count += registerIfAbsent("system", "signup", "email_verification",
				"false", SettingValueType.BOOLEAN,
				"이메일 인증 필수", "활성화 시 가입 후 이메일 인증을 완료해야 합니다", false, 1);
		count += registerIfAbsent("system", "signup", "default_status",
				"ACTIVE", SettingValueType.STRING,
				"기본 회원 상태", "가입 시 기본 사용자 상태 (ACTIVE/PENDING)", false, 2);

		// ─── auth 그룹: 인증 보안 설정 ───
		count += registerIfAbsent("system", "auth", "max_login_fail",
				"5", SettingValueType.NUMBER,
				"최대 로그인 실패 횟수", "이 횟수를 초과하면 계정이 잠깁니다", false, 0);
		count += registerIfAbsent("system", "auth", "lock_duration_min",
				"30", SettingValueType.NUMBER,
				"계정 잠금 시간 (분)", "잠금 후 자동 해제까지의 시간 (분 단위)", false, 1);
		count += registerIfAbsent("system", "auth", "otp_required",
				"false", SettingValueType.BOOLEAN,
				"2FA 필수 여부", "활성화 시 모든 사용자에게 2FA를 요구합니다", false, 2);

		// ─── session 그룹: 토큰 만료 설정 ───
		count += registerIfAbsent("system", "session", "access_token_exp",
				"1800000", SettingValueType.NUMBER,
				"Access Token 만료 (ms)", "Access Token 만료 시간 (밀리초, 기본 30분)", false, 0);
		count += registerIfAbsent("system", "session", "refresh_token_exp",
				"604800000", SettingValueType.NUMBER,
				"Refresh Token 만료 (ms)", "Refresh Token 만료 시간 (밀리초, 기본 7일)", false, 1);

		return count;
	}

	// 모듈별 기본 설정 등록
	private int registerModuleDefaults(String moduleCode, List<SettingDefinition> definitions) {
		int count = 0;
		for (SettingDefinition def : definitions) {
			count += registerIfAbsent(
					moduleCode, def.getGroup(), def.getKey(),
					def.getDefaultValue(), def.getValueType(),
					def.getName(), def.getDescription(),
					false, def.getSortOrder()
			);
		}
		return count;
	}

	// DB에 없으면 INSERT, 있으면 스킵 (기존 수정값 보존)
	// 반환값: 실제 INSERT된 경우 1, 이미 존재해서 스킵된 경우 0
	private int registerIfAbsent(String moduleCode, String settingGroup, String settingKey,
								 String defaultValue, SettingValueType valueType,
								 String name, String description,
								 boolean readonly, int sortOrder) {
		// 이미 존재하면 스킵
		if (settingRepository.existsByModuleCodeAndSettingGroupAndSettingKey(
				moduleCode, settingGroup, settingKey)) {
			return 0;
		}

		// 신규 설정 INSERT
		SettingEntity entity = SettingEntity.create(
				moduleCode, settingGroup, settingKey,
				defaultValue, valueType,
				name, description, readonly, sortOrder
		);
		settingRepository.save(entity);
		log.debug("설정 등록: {}/{}/{} = {}", moduleCode, settingGroup, settingKey, defaultValue);
		return 1;
	}
}
