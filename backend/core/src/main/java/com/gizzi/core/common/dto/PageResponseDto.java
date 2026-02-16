package com.gizzi.core.common.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// 공통 페이지네이션 응답 DTO
// Spring Data Page 객체를 API 응답 형식으로 변환한다
@Getter
@Builder
public class PageResponseDto<T> {

	// 현재 페이지 데이터 목록
	private final List<T>  content;

	// 현재 페이지 번호 (0-based)
	private final int      page;

	// 페이지 크기
	private final int      size;

	// 전체 요소 수
	private final long     totalElements;

	// 전체 페이지 수
	private final int      totalPages;

	// Page<E> 엔티티를 Page<T> DTO로 변환하는 정적 팩토리 메서드
	public static <E, T> PageResponseDto<T> from(Page<E> pageResult, Function<E, T> converter) {
		// 엔티티 목록을 DTO 목록으로 변환
		List<T> content = pageResult.getContent().stream()
			.map(converter)
			.toList();

		return PageResponseDto.<T>builder()
			.content(content)
			.page(pageResult.getNumber())
			.size(pageResult.getSize())
			.totalElements(pageResult.getTotalElements())
			.totalPages(pageResult.getTotalPages())
			.build();
	}
}
