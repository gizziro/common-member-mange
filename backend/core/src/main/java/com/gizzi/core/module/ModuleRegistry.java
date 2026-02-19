package com.gizzi.core.module;

import com.gizzi.core.module.dto.ResourcePermissionDefinition;
import com.gizzi.core.module.entity.ModuleEntity;
import com.gizzi.core.module.entity.ModuleInstanceEntity;
import com.gizzi.core.module.entity.ModulePermissionEntity;
import com.gizzi.core.module.repository.ModuleInstanceRepository;
import com.gizzi.core.module.repository.ModulePermissionRepository;
import com.gizzi.core.module.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 모듈 레지스트리 — ModuleDefinition Bean 자동 발견 → DB 메타데이터 동기화
// 앱 시작 시 모든 ModuleDefinition 구현체를 순회하여:
//   1. tb_modules에 없으면 INSERT, 있으면 메타데이터 갱신
//   2. tb_module_permissions에 없는 (module_code, resource, action) INSERT
//   3. DB에만 있고 코드에 없는 권한은 삭제하지 않음 (경고 로그)
//   4. ConcurrentHashMap에 code → definition 캐시
//
// 실행 순서: @Order(2) — ModuleSchemaInitializer(@Order(1)) 이후 실행
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ModuleRegistry implements ApplicationRunner
{

	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------

	private final ModuleRepository           moduleRepository;       // 모듈 메타데이터 리포지토리
	private final ModulePermissionRepository  permissionRepository;   // 모듈 권한 정의 리포지토리
	private final ModuleInstanceRepository    instanceRepository;     // 모듈 인스턴스 리포지토리 (SINGLE 모듈 시스템 인스턴스 자동 생성용)
	private final List<ModuleDefinition>      moduleDefinitions;      // 등록된 모든 모듈 정의 (Spring이 자동 주입, 없으면 빈 리스트)

	//----------------------------------------------------------------------------------------------------------------------
	// [ 런타임 캐시 ]
	//----------------------------------------------------------------------------------------------------------------------

	// 코드 → ModuleDefinition 런타임 캐시 (모듈 정보 빠른 조회용)
	private final Map<String, ModuleDefinition> definitionCache = new ConcurrentHashMap<>();

	//======================================================================================================================
	// 초기화 실행
	//======================================================================================================================

	@Override
	@Transactional
	public void run(ApplicationArguments args)
	{
		log.info("모듈 레지스트리 초기화 시작: {}개 ModuleDefinition 발견", moduleDefinitions.size());

		// 각 모듈 정의를 순회하며 DB와 동기화
		for (ModuleDefinition definition : moduleDefinitions)
		{
			syncModule(definition);
		}

		log.info("모듈 레지스트리 초기화 완료: {}개 모듈 등록", definitionCache.size());
	}

	//----------------------------------------------------------------------------------------------------------------------
	// 단일 모듈 동기화
	//----------------------------------------------------------------------------------------------------------------------

	// 단일 모듈의 DB 동기화 처리
	private void syncModule(ModuleDefinition definition)
	{
		String moduleCode = definition.getCode();
		log.debug("모듈 [{}] 동기화 시작", moduleCode);

		// 1. tb_modules 동기화 (INSERT 또는 UPDATE)
		syncModuleMetadata(definition);

		// 2. tb_module_permissions 동기화 (새 권한 INSERT, 코드에서 제거된 권한 경고)
		syncModulePermissions(definition);

		// 3. SINGLE 모듈 시스템 인스턴스 자동 생성
		ensureSystemInstance(definition);

		// 4. 런타임 캐시에 등록
		definitionCache.put(moduleCode, definition);

		log.info("모듈 [{}] 동기화 완료 — {} ({})", moduleCode, definition.getName(), definition.getType());
	}

	//----------------------------------------------------------------------------------------------------------------------
	// tb_modules 동기화
	//----------------------------------------------------------------------------------------------------------------------

	// tb_modules 테이블에 모듈 메타데이터 동기화
	private void syncModuleMetadata(ModuleDefinition definition)
	{
		String moduleCode = definition.getCode();

		// DB에서 기존 모듈 조회
		Optional<ModuleEntity> existing = moduleRepository.findByCode(moduleCode);

		if (existing.isPresent())
		{
			// 기존 모듈이 있으면 메타데이터 갱신
			ModuleEntity entity = existing.get();
			entity.updateMetadata(
					definition.getName(),
					definition.getSlug(),
					definition.getDescription(),
					definition.getType()
			);
			moduleRepository.save(entity);
			log.debug("모듈 [{}]: 메타데이터 갱신 완료", moduleCode);
		}
		else
		{
			// 신규 모듈 INSERT
			ModuleEntity entity = ModuleEntity.create(
					definition.getCode(),
					definition.getName(),
					definition.getSlug(),
					definition.getDescription(),
					definition.getType()
			);
			moduleRepository.save(entity);
			log.info("모듈 [{}]: 신규 등록 완료", moduleCode);
		}
	}

	//----------------------------------------------------------------------------------------------------------------------
	// tb_module_permissions 동기화
	//----------------------------------------------------------------------------------------------------------------------

	// tb_module_permissions 테이블에 권한 정의 동기화
	private void syncModulePermissions(ModuleDefinition definition)
	{
		String moduleCode = definition.getCode();
		List<ResourcePermissionDefinition> codePermissions = definition.getPermissions();

		// 코드에 정의된 권한 키 수집 (중복 체크용)
		Set<String> codePermissionKeys = new HashSet<>();

		//----------------------------------------------------------------------------------------------------------------------
		// 코드 → DB 동기화 (새 권한 INSERT)
		//----------------------------------------------------------------------------------------------------------------------

		// 코드에 정의된 각 권한에 대해 DB 존재 여부 확인 → 없으면 INSERT
		for (ResourcePermissionDefinition perm : codePermissions)
		{
			String key = moduleCode + ":" + perm.getResource() + ":" + perm.getAction();
			codePermissionKeys.add(key);

			// DB에 이미 존재하면 스킵
			boolean exists = permissionRepository.existsByModuleCodeAndResourceAndAction(
					moduleCode, perm.getResource(), perm.getAction()
			);

			if (!exists)
			{
				// 신규 권한 INSERT
				ModulePermissionEntity entity = ModulePermissionEntity.create(
						moduleCode, perm.getResource(), perm.getAction(), perm.getName()
				);
				permissionRepository.save(entity);
				log.info("모듈 [{}]: 권한 추가 — {}_{}_{}", moduleCode,
						moduleCode.toUpperCase(), perm.getResource().toUpperCase(), perm.getAction().toUpperCase());
			}
		}

		//----------------------------------------------------------------------------------------------------------------------
		// DB에만 있는 권한 경고 (삭제하지 않음)
		//----------------------------------------------------------------------------------------------------------------------

		// DB에만 있고 코드에 없는 권한 경고 (삭제하지 않음 — 기존 권한 부여 데이터 보호)
		List<ModulePermissionEntity> dbPermissions = permissionRepository.findByModuleCode(moduleCode);
		for (ModulePermissionEntity dbPerm : dbPermissions)
		{
			String key = moduleCode + ":" + dbPerm.getResource() + ":" + dbPerm.getAction();
			if (!codePermissionKeys.contains(key))
			{
				log.warn("모듈 [{}]: DB에만 존재하는 권한 — {}_{}_{}  (코드에서 제거됨, DB에서는 유지)",
						moduleCode, moduleCode.toUpperCase(),
						dbPerm.getResource().toUpperCase(), dbPerm.getAction().toUpperCase());
			}
		}
	}

	//======================================================================================================================
	// 런타임 캐시 조회
	//======================================================================================================================

	// 모듈 코드로 ModuleDefinition 조회 (런타임 캐시)
	public ModuleDefinition getDefinition(String moduleCode)
	{
		return definitionCache.get(moduleCode);
	}

	// 등록된 모든 ModuleDefinition 조회
	public Map<String, ModuleDefinition> getAllDefinitions()
	{
		return Map.copyOf(definitionCache);
	}

	// 모듈 코드의 등록 여부 확인
	public boolean isRegistered(String moduleCode)
	{
		return definitionCache.containsKey(moduleCode);
	}

	//----------------------------------------------------------------------------------------------------------------------
	// SINGLE 모듈 시스템 인스턴스
	//----------------------------------------------------------------------------------------------------------------------

	// SINGLE 모듈의 시스템 인스턴스 자동 생성
	// PermissionChecker는 instanceId를 필요로 하므로, SINGLE 모듈도 시스템 인스턴스가 필요하다
	// 이미 존재하면 스킵 (멱등)
	private void ensureSystemInstance(ModuleDefinition definition)
	{
		// SINGLE 모듈이 아니면 스킵
		if (definition.getType() != ModuleType.SINGLE)
		{
			return;
		}

		String moduleCode = definition.getCode();

		// 이미 시스템 인스턴스가 존재하면 스킵
		boolean exists = instanceRepository.existsByModuleCodeAndInstanceType(moduleCode, "SYSTEM");
		if (exists)
		{
			log.debug("모듈 [{}]: SINGLE 모듈 시스템 인스턴스 이미 존재", moduleCode);
			return;
		}

		// 시스템 인스턴스 생성 (소유자/생성자 = "SYSTEM")
		ModuleInstanceEntity instance = ModuleInstanceEntity.create(
				moduleCode,
				definition.getName(),
				definition.getSlug(),
				definition.getDescription(),
				"SYSTEM",
				"SYSTEM",
				"SYSTEM"
		);
		instanceRepository.save(instance);
		log.info("모듈 [{}]: SINGLE 모듈 시스템 인스턴스 자동 생성", moduleCode);
	}
}
