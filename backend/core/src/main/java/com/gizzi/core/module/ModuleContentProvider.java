package com.gizzi.core.module;

import com.gizzi.core.module.dto.ContentSummaryDto;

import java.util.List;
import java.util.Optional;

// 모듈 간 콘텐츠 참조를 위한 SPI 인터페이스
// 기능 모듈이 이 인터페이스를 구현하면 다른 모듈에서 직접 의존 없이
// ModuleContentRegistry를 통해 해당 모듈의 콘텐츠를 조회할 수 있다
//
// 순환 의존성 해결 패턴:
//   module-board ──→ core ←── module-accounting
//   (BoardContentProvider)     (core의 ModuleContentRegistry를 통해 board 콘텐츠 조회)
public interface ModuleContentProvider
{

	//----------------------------------------------------------------------------------------------------------------------
	// 모듈 식별
	//----------------------------------------------------------------------------------------------------------------------

	// 이 제공자가 담당하는 모듈 코드
	// 예: "board", "blog"
	String getModuleCode();

	// 특정 리소스 유형을 지원하는지 확인
	// 예: supports("post") → true
	boolean supports(String resourceType);

	//----------------------------------------------------------------------------------------------------------------------
	// 콘텐츠 조회
	//----------------------------------------------------------------------------------------------------------------------

	// 특정 리소스의 요약 정보 조회
	// 예: getContent("post", "post-uuid-123") → 게시글 제목, URL 등
	Optional<ContentSummaryDto> getContent(String resourceType, String resourceId);

	// 리소스 검색 (키워드 기반, 결과 수 제한)
	// 예: search("post", "공지사항", 10)
	List<ContentSummaryDto> search(String resourceType, String query, int limit);
}
