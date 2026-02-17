package com.gizzi.core.module;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

// 모듈별 DB 스키마 자동 초기화 컴포넌트
// 앱 시작 시 classpath에서 모듈 스키마 SQL 파일을 탐색하여
// 해당 테이블이 존재하지 않으면 스키마를 자동 실행한다
//
// 실행 순서: @Order(1) — ModuleRegistry(@Order(2))보다 먼저 실행
//
// 동작 흐름:
//   1. 등록된 ModuleDefinition Bean의 getRequiredTables()로 대표 테이블 확인
//   2. 대표 테이블이 이미 존재하면 스킵 (멱등)
//   3. 테이블이 없으면 classpath:db/{module-code}-schema.sql 로드 → SQL 실행
//
// 모듈 스키마 파일 규칙:
//   위치: modules/module-{code}/src/main/resources/db/{code}-schema.sql
//   classpath: db/{code}-schema.sql (빌드 시 자동 포함)
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ModuleSchemaInitializer implements ApplicationRunner {

	// JDBC 직접 사용 (JPA보다 먼저 스키마를 준비해야 하므로)
	private final JdbcTemplate jdbcTemplate;

	// 등록된 모든 모듈 정의 (Spring이 자동 주입, 없으면 빈 리스트)
	private final List<ModuleDefinition> moduleDefinitions;

	@Override
	public void run(ApplicationArguments args) {
		log.info("모듈 스키마 초기화 시작: {}개 모듈 정의 발견", moduleDefinitions.size());

		// 각 모듈 정의를 순회하며 스키마 존재 여부 확인 및 적용
		for (ModuleDefinition definition : moduleDefinitions) {
			initializeModuleSchema(definition);
		}

		log.info("모듈 스키마 초기화 완료");
	}

	// 단일 모듈의 스키마 초기화 처리
	private void initializeModuleSchema(ModuleDefinition definition) {
		String moduleCode = definition.getCode();

		// 대표 테이블이 정의되지 않은 모듈은 스키마 초기화 불필요
		List<String> requiredTables = definition.getRequiredTables();
		if (requiredTables.isEmpty()) {
			log.debug("모듈 [{}]: 필수 테이블 정의 없음 — 스키마 초기화 스킵", moduleCode);
			return;
		}

		// 대표 테이블 존재 여부 확인 (첫 번째 테이블만 검사)
		String checkTable = requiredTables.get(0);
		if (isTableExists(checkTable)) {
			log.debug("모듈 [{}]: 테이블 '{}' 이미 존재 — 스키마 초기화 스킵", moduleCode, checkTable);
			return;
		}

		// classpath에서 스키마 SQL 파일 로드
		String resourcePath = "classpath:db/" + moduleCode + "-schema.sql";
		try {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			Resource resource = resolver.getResource(resourcePath);

			// 리소스가 존재하지 않으면 경고 로그
			if (!resource.exists()) {
				log.warn("모듈 [{}]: 스키마 파일 '{}' 을 찾을 수 없습니다", moduleCode, resourcePath);
				return;
			}

			// SQL 파일 내용 읽기
			String sql = readResourceContent(resource);

			// SQL 실행
			log.info("모듈 [{}]: 스키마 SQL 실행 — {}", moduleCode, resourcePath);
			jdbcTemplate.execute(sql);
			log.info("모듈 [{}]: 스키마 초기화 성공", moduleCode);

		} catch (Exception e) {
			log.error("모듈 [{}]: 스키마 초기화 실패 — {}", moduleCode, e.getMessage(), e);
		}
	}

	// information_schema를 조회하여 테이블 존재 여부 확인
	private boolean isTableExists(String tableName) {
		String sql = "SELECT COUNT(*) FROM information_schema.TABLES " +
		             "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
		return count != null && count > 0;
	}

	// Resource에서 텍스트 내용 읽기
	private String readResourceContent(Resource resource) throws Exception {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}
}
