package com.gizzi.core.common.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// JPA 설정: Auditing 활성화 + 엔티티/리포지토리 스캔 패키지 등록
// core 모듈의 도메인 패키지를 스캔 대상으로 등록하여
// admin-api, user-api 양쪽에서 엔티티와 리포지토리를 자동 인식하도록 한다
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.gizzi.core.domain")
@EntityScan(basePackages = "com.gizzi.core.domain")
public class JpaConfig {
}
