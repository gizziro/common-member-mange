package com.gizzi.module.board.service;

import com.gizzi.module.board.dto.tag.TagResponseDto;
import com.gizzi.module.board.entity.BoardPostTagEntity;
import com.gizzi.module.board.entity.BoardTagEntity;
import com.gizzi.module.board.repository.BoardPostTagRepository;
import com.gizzi.module.board.repository.BoardTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// 게시판 태그 관리 서비스
// 게시글에 부여되는 태그의 생성/조회/동기화/삭제를 담당한다
// 태그는 게시글 작성/수정 시 자동으로 관리되며, 사용 빈도(postCount)를 추적한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardTagService {

	// 태그 리포지토리
	private final BoardTagRepository     tagRepository;

	// 게시글-태그 연결 리포지토리
	private final BoardPostTagRepository postTagRepository;

	// ─── 태그 목록 조회 (User API용) ───

	// 게시판의 사용 중인 태그 목록 조회 (postCount > 0, 인기순)
	public List<TagResponseDto> getTagList(String boardId) {
		// 게시판의 전체 태그를 인기순으로 조회 후 postCount > 0인 것만 필터
		return tagRepository.findByBoardInstanceIdOrderByPostCountDesc(boardId).stream()
				.filter(tag -> tag.getPostCount() > 0)
				.map(tag -> TagResponseDto.builder()
						.id(tag.getId())
						.name(tag.getName())
						.slug(tag.getSlug())
						.postCount(tag.getPostCount())
						.build())
				.collect(Collectors.toList());
	}

	// ─── 태그 조회/생성 ───

	// 태그 이름 목록으로 태그 엔티티 목록 조회 (없으면 새로 생성)
	// 게시글 작성/수정 시 호출되어 태그를 확보한다
	@Transactional
	public List<BoardTagEntity> getOrCreateTags(String boardId, List<String> tagNames) {
		// 태그 이름 목록이 비어있으면 빈 리스트 반환
		if (tagNames == null || tagNames.isEmpty()) {
			return List.of();
		}

		// 결과를 담을 태그 엔티티 리스트
		List<BoardTagEntity> tags = new ArrayList<>();

		// 각 태그 이름에 대해 조회 또는 생성
		for (String tagName : tagNames) {
			// 태그 이름 정규화 (앞뒤 공백 제거)
			String normalizedName = tagName.trim();
			// 빈 문자열은 무시
			if (normalizedName.isEmpty()) {
				continue;
			}

			// 슬러그 생성 (소문자 변환 + 공백을 하이픈으로 치환)
			String slug = normalizedName.toLowerCase().replaceAll("\\s+", "-");

			// 동일 게시판 내 이름으로 기존 태그 조회
			Optional<BoardTagEntity> existing = tagRepository.findByBoardInstanceIdAndName(boardId, normalizedName);

			if (existing.isPresent()) {
				// 기존 태그가 있으면 그대로 사용
				tags.add(existing.get());
			} else {
				// 기존 태그가 없으면 새로 생성
				BoardTagEntity newTag = BoardTagEntity.create(boardId, normalizedName, slug);
				// DB에 저장
				tagRepository.save(newTag);
				tags.add(newTag);

				log.debug("태그 생성: {} (boardId: {}, slug: {})", normalizedName, boardId, slug);
			}
		}

		return tags;
	}

	// ─── 게시글-태그 동기화 ───

	// 게시글의 태그 연결을 동기화 (기존 연결 삭제 후 새로 연결)
	// 게시글 작성/수정 시 호출된다
	@Transactional
	public void syncPostTags(String postId, String boardId, List<String> tagNames) {
		// 기존 태그 연결 정보를 먼저 조회 (postCount 감소 처리용)
		List<BoardPostTagEntity> existingPostTags = postTagRepository.findByPostId(postId);

		// 기존 연결된 태그들의 postCount 감소
		for (BoardPostTagEntity postTag : existingPostTags) {
			// 태그 엔티티 조회
			tagRepository.findById(postTag.getTagId()).ifPresent(tag -> {
				// 게시글 수 1 감소
				tag.decrementPostCount();
				tagRepository.save(tag);
			});
		}

		// 기존 연결 모두 삭제
		postTagRepository.deleteByPostId(postId);

		// 태그 이름 목록이 비어있으면 연결 삭제만 수행
		if (tagNames == null || tagNames.isEmpty()) {
			return;
		}

		// 새 태그 목록 확보 (없으면 생성)
		List<BoardTagEntity> tags = getOrCreateTags(boardId, tagNames);

		// 새 태그 연결 생성 + postCount 증가
		for (BoardTagEntity tag : tags) {
			// 게시글-태그 연결 엔티티 생성
			BoardPostTagEntity postTag = BoardPostTagEntity.create(postId, tag.getId());
			// DB에 저장
			postTagRepository.save(postTag);

			// 태그 사용 게시글 수 증가
			tag.incrementPostCount();
			tagRepository.save(tag);
		}

		log.debug("게시글 태그 동기화: postId={}, 태그 수={}", postId, tags.size());
	}

	// ─── 게시글 태그 조회 ───

	// 게시글에 연결된 태그명 목록 조회
	public List<String> getTagNamesByPostId(String postId) {
		// 게시글-태그 연결 조회 후 태그 엔티티의 이름 추출
		return postTagRepository.findByPostId(postId).stream()
				.map(postTag -> tagRepository.findById(postTag.getTagId())
						.map(BoardTagEntity::getName)
						.orElse(null))
				.filter(name -> name != null)
				.collect(Collectors.toList());
	}

	// ─── 게시글 태그 제거 ───

	// 게시글 삭제 시 태그 연결 해제 + postCount 감소
	// 게시글 소프트 삭제 시 호출된다
	@Transactional
	public void removePostTags(String postId) {
		// 게시글에 연결된 태그 목록 조회
		List<BoardPostTagEntity> postTags = postTagRepository.findByPostId(postId);

		// 각 태그의 사용 게시글 수 감소
		for (BoardPostTagEntity postTag : postTags) {
			tagRepository.findById(postTag.getTagId()).ifPresent(tag -> {
				// 게시글 수 1 감소
				tag.decrementPostCount();
				tagRepository.save(tag);
			});
		}

		// 게시글-태그 연결 모두 삭제
		postTagRepository.deleteByPostId(postId);

		log.debug("게시글 태그 제거: postId={}, 제거된 태그 수={}", postId, postTags.size());
	}
}
