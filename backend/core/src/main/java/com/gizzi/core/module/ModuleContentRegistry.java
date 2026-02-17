package com.gizzi.core.module;

import com.gizzi.core.module.dto.ContentSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 모듈 간 콘텐츠 참조를 위한 중앙 레지스트리
// ModuleContentProvider 구현체를 수집하여 모듈 코드 기반으로 콘텐츠를 조회한다
//
// 사용 예:
//   가계부 모듈에서 게시판의 게시글을 참조할 때
//   moduleContentRegistry.getContent("board", "post", "post-uuid-123");
//   → ContentSummaryDto { title: "공지사항", url: "/board/notice/posts/123" }
//
// 순환 의존성 해결:
//   module-board는 core의 ModuleContentProvider를 구현하고
//   module-accounting은 core의 ModuleContentRegistry를 통해 board 콘텐츠를 조회한다
//   두 모듈이 서로를 직접 의존하지 않으므로 순환 참조가 발생하지 않는다
@Slf4j
@Component
public class ModuleContentRegistry {

	// 모듈 코드 → ContentProvider 매핑
	private final Map<String, ModuleContentProvider> providers = new HashMap<>();

	// 생성자에서 모든 ContentProvider Bean을 자동 수집
	// ContentProvider가 없으면 빈 리스트가 주입된다
	public ModuleContentRegistry(List<ModuleContentProvider> contentProviders) {
		for (ModuleContentProvider provider : contentProviders) {
			providers.put(provider.getModuleCode(), provider);
			log.debug("콘텐츠 제공자 등록: 모듈 [{}]", provider.getModuleCode());
		}
		log.info("모듈 콘텐츠 레지스트리 초기화 완료: {}개 제공자 등록", providers.size());
	}

	// 특정 모듈의 리소스 콘텐츠 조회
	// moduleCode: 조회 대상 모듈 코드 (예: "board")
	// resourceType: 리소스 유형 (예: "post")
	// resourceId: 리소스 PK (예: "post-uuid-123")
	public Optional<ContentSummaryDto> getContent(String moduleCode, String resourceType,
	                                               String resourceId) {
		// 해당 모듈의 ContentProvider 조회
		ModuleContentProvider provider = providers.get(moduleCode);
		if (provider == null) {
			log.warn("콘텐츠 제공자 없음: 모듈 [{}]", moduleCode);
			return Optional.empty();
		}

		// 리소스 유형 지원 여부 확인
		if (!provider.supports(resourceType)) {
			log.warn("지원하지 않는 리소스 유형: 모듈 [{}], 리소스 [{}]", moduleCode, resourceType);
			return Optional.empty();
		}

		return provider.getContent(resourceType, resourceId);
	}

	// 특정 모듈의 리소스 검색
	// moduleCode: 검색 대상 모듈 코드 (예: "board")
	// resourceType: 리소스 유형 (예: "post")
	// query: 검색 키워드
	// limit: 최대 결과 수
	public List<ContentSummaryDto> search(String moduleCode, String resourceType,
	                                       String query, int limit) {
		// 해당 모듈의 ContentProvider 조회
		ModuleContentProvider provider = providers.get(moduleCode);
		if (provider == null) {
			log.warn("콘텐츠 제공자 없음: 모듈 [{}]", moduleCode);
			return List.of();
		}

		// 리소스 유형 지원 여부 확인
		if (!provider.supports(resourceType)) {
			log.warn("지원하지 않는 리소스 유형: 모듈 [{}], 리소스 [{}]", moduleCode, resourceType);
			return List.of();
		}

		return provider.search(resourceType, query, limit);
	}

	// 특정 모듈의 ContentProvider 존재 여부 확인
	public boolean hasProvider(String moduleCode) {
		return providers.containsKey(moduleCode);
	}
}
