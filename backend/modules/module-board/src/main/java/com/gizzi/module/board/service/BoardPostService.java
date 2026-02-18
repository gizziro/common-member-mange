package com.gizzi.module.board.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.board.dto.post.CreatePostRequestDto;
import com.gizzi.module.board.dto.post.PostListResponseDto;
import com.gizzi.module.board.dto.post.PostResponseDto;
import com.gizzi.module.board.dto.post.UpdatePostRequestDto;
import com.gizzi.module.board.entity.BoardPostClosureEntity;
import com.gizzi.module.board.entity.BoardPostEntity;
import com.gizzi.module.board.entity.BoardSettingsEntity;
import com.gizzi.module.board.entity.NoticeScope;
import com.gizzi.module.board.entity.PostContentType;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.repository.BoardCategoryRepository;
import com.gizzi.module.board.repository.BoardPostClosureRepository;
import com.gizzi.module.board.repository.BoardPostRepository;
import com.gizzi.module.board.repository.BoardSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// 게시판 게시글 CRUD + Closure Table 계층 관리 서비스
// 게시글 생성/수정/삭제/조회/검색 및 공지 토글 기능을 제공한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPostService {

	// 게시글 리포지토리
	private final BoardPostRepository        postRepository;

	// 게시글 Closure Table 리포지토리 (계층 관계 관리)
	private final BoardPostClosureRepository  closureRepository;

	// 게시판 설정 리포지토리 (답글 깊이 제한 등 조회)
	private final BoardSettingsRepository     settingsRepository;

	// 카테고리 리포지토리 (카테고리명 조회용)
	private final BoardCategoryRepository     categoryRepository;

	// 태그 서비스 (게시글-태그 동기화 위임)
	private final BoardTagService             tagService;

	// 게시판 권한 헬퍼 (수정/삭제/비밀글 열람 권한 체크)
	private final BoardPermissionHelper       permissionHelper;

	// ─── 게시글 생성 ───

	// 게시글 생성 (답글 Closure Table 관리 포함)
	@Transactional
	public PostResponseDto createPost(String boardId, CreatePostRequestDto request,
	                                  String userId, String username) {
		// 1. 콘텐츠 타입 파싱 (기본값: MARKDOWN)
		PostContentType contentType = parseContentType(request.getContentType());

		// 2. 답글인 경우 부모 게시글 검증 + depth 계산
		int depth = 0;
		if (request.getParentId() != null) {
			// 부모 게시글 존재 여부 확인
			BoardPostEntity parent = postRepository.findById(request.getParentId())
					.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND));
			// 부모 깊이 + 1 = 자식 깊이
			depth = parent.getDepth() + 1;

			// 게시판 설정에서 최대 답글 깊이 확인
			BoardSettingsEntity settings = settingsRepository.findById(boardId)
					.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
			// 최대 답글 깊이 초과 시 예외
			if (depth > settings.getMaxReplyDepth()) {
				throw new BusinessException(BoardErrorCode.BOARD_MAX_REPLY_DEPTH);
			}
		}

		// 3. 게시글 엔티티 생성
		BoardPostEntity post = BoardPostEntity.create(
				boardId, request.getCategoryId(), request.getParentId(), depth,
				request.getTitle(), request.getContent(), contentType,
				request.getSlug(),
				Boolean.TRUE.equals(request.getIsSecret()),
				Boolean.TRUE.equals(request.getIsDraft()),
				userId, username
		);
		// DB에 게시글 저장
		postRepository.save(post);

		// 4. Closure Table 관리 — 자기 자신 참조 (depth 0)
		closureRepository.save(BoardPostClosureEntity.create(post.getId(), post.getId(), 0));

		// 부모가 있으면 부모의 모든 조상을 복사하여 새 게시글에 연결
		if (request.getParentId() != null) {
			// 부모의 모든 조상 관계 조회
			List<BoardPostClosureEntity> parentAncestors = closureRepository.findByDescendantId(request.getParentId());
			// 각 조상에 대해 새 게시글까지의 관계 생성 (depth +1)
			for (BoardPostClosureEntity ancestor : parentAncestors) {
				closureRepository.save(BoardPostClosureEntity.create(
						ancestor.getAncestorId(), post.getId(), ancestor.getDepth() + 1));
			}
		}

		// 5. 태그 동기화 (태그가 지정된 경우)
		if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
			tagService.syncPostTags(post.getId(), boardId, request.getTagNames());
		}

		log.info("게시글 생성: {} (boardId: {}, postId: {})", post.getTitle(), boardId, post.getId());
		// 응답 DTO 변환 후 반환
		return toPostResponseDto(post);
	}

	// ─── 게시글 수정 ───

	// 게시글 수정 (제목, 본문, 카테고리, 태그 등 변경)
	@Transactional
	public PostResponseDto updatePost(String boardId, String postId,
	                                  UpdatePostRequestDto request, String userId) {
		// 게시글 조회 (없으면 BOARD_POST_NOT_FOUND 예외)
		BoardPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		// 수정 권한 확인 (본인 글 또는 관리자)
		if (!permissionHelper.canEditPost(userId, boardId, post.getAuthorId())) {
			throw new BusinessException(BoardErrorCode.BOARD_POST_EDIT_DENIED);
		}

		// 콘텐츠 타입 파싱
		PostContentType contentType = parseContentType(request.getContentType());

		// 게시글 내용 수정
		post.updateContent(request.getTitle(), request.getContent(), contentType,
				request.getCategoryId(), request.getSlug(),
				request.getMetaTitle(), request.getMetaDescription());
		// 변경 사항 저장
		postRepository.save(post);

		// 태그 동기화 (null이 아닌 경우에만 — null이면 태그 변경 없음)
		if (request.getTagNames() != null) {
			tagService.syncPostTags(postId, boardId, request.getTagNames());
		}

		// 응답 DTO 변환 후 반환
		return toPostResponseDto(post);
	}

	// ─── 게시글 삭제 ───

	// 게시글 소프트 삭제 (is_deleted 플래그 설정)
	@Transactional
	public void deletePost(String boardId, String postId, String userId) {
		// 게시글 조회 (없으면 BOARD_POST_NOT_FOUND 예외)
		BoardPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		// 삭제 권한 확인 (본인 글 또는 관리자)
		if (!permissionHelper.canDeletePost(userId, boardId, post.getAuthorId())) {
			throw new BusinessException(BoardErrorCode.BOARD_POST_DELETE_DENIED);
		}

		// 소프트 삭제 처리 (삭제 플래그 + 삭제자 + 삭제 시각 기록)
		post.markAsDeleted(userId);
		postRepository.save(post);

		log.info("게시글 삭제: {} (postId: {})", post.getTitle(), postId);
	}

	// ─── 게시글 상세 조회 ───

	// 게시글 상세 조회 (조회수 증가 포함)
	@Transactional
	public PostResponseDto getPost(String boardId, String postId, String userId) {
		// 게시글 조회 (없으면 BOARD_POST_NOT_FOUND 예외)
		BoardPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		// 삭제된 게시글은 조회 불가
		if (post.getIsDeleted()) {
			throw new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND);
		}

		// 비밀글인 경우 열람 권한 확인 (작성자 또는 관리자만)
		if (post.getIsSecret() && !permissionHelper.canReadSecret(userId, boardId, post.getAuthorId())) {
			throw new BusinessException(BoardErrorCode.BOARD_SECRET_ACCESS_DENIED);
		}

		// 조회수 1 증가
		post.incrementViewCount();
		postRepository.save(post);

		// 응답 DTO 변환 후 반환
		return toPostResponseDto(post);
	}

	// ─── 게시글 목록 조회 ───

	// 게시글 목록 조회 (페이징, 카테고리 필터 지원)
	public Page<PostListResponseDto> getPosts(String boardId, String categoryId, Pageable pageable) {
		Page<BoardPostEntity> posts;

		// 카테고리가 지정된 경우 카테고리별 조회
		if (categoryId != null && !categoryId.isBlank()) {
			posts = postRepository.findByBoardInstanceIdAndCategoryIdAndIsDeletedFalseAndIsDraftFalseOrderByIsNoticeDescCreatedAtDesc(
					boardId, categoryId, pageable);
		} else {
			// 전체 게시글 조회 (삭제/임시저장 제외, 공지 우선)
			posts = postRepository.findByBoardInstanceIdAndIsDeletedFalseAndIsDraftFalseOrderByIsNoticeDescCreatedAtDesc(
					boardId, pageable);
		}

		// 목록용 DTO로 변환
		return posts.map(this::toPostListResponseDto);
	}

	// ─── 게시글 검색 ───

	// 게시글 검색 (제목, 내용, 작성자명 키워드 검색)
	public Page<PostListResponseDto> searchPosts(String boardId, String keyword, Pageable pageable) {
		return postRepository.searchPosts(boardId, keyword, pageable)
				.map(this::toPostListResponseDto);
	}

	// ─── 공지글 ───

	// 공지글 목록 조회 (삭제되지 않은 공지글만)
	public List<PostListResponseDto> getNoticePosts(String boardId) {
		return postRepository.findByBoardInstanceIdAndIsNoticeTrueAndIsDeletedFalse(boardId).stream()
				.map(this::toPostListResponseDto)
				.collect(Collectors.toList());
	}

	// 공지글 토글 (관리자용 — 공지 설정/해제 전환)
	@Transactional
	public PostResponseDto toggleNotice(String postId, String scopeStr) {
		// 게시글 조회 (없으면 BOARD_POST_NOT_FOUND 예외)
		BoardPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		// 공지 범위 파싱 (기본값: BOARD)
		NoticeScope scope = scopeStr != null ? NoticeScope.valueOf(scopeStr.toUpperCase()) : NoticeScope.BOARD;

		// 공지 상태 토글
		post.toggleNotice(scope);
		postRepository.save(post);

		// 응답 DTO 변환 후 반환
		return toPostResponseDto(post);
	}

	// ─── 관리자용 게시글 목록 ───

	// 관리자용 게시글 목록 (임시저장 포함, 소프트삭제 제외)
	public Page<PostListResponseDto> getAdminPosts(String boardId, Pageable pageable) {
		return postRepository.findByBoardInstanceIdAndIsDeletedFalseOrderByCreatedAtDesc(boardId, pageable)
				.map(this::toPostListResponseDto);
	}

	// ─── Private 헬퍼 ───

	// 콘텐츠 타입 문자열 → PostContentType 변환 (기본값: MARKDOWN)
	private PostContentType parseContentType(String type) {
		// null이거나 빈 문자열이면 기본값 MARKDOWN
		if (type == null || type.isBlank()) {
			return PostContentType.MARKDOWN;
		}
		try {
			// 대문자로 변환 후 enum 파싱
			return PostContentType.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			// 알 수 없는 타입이면 기본값 반환
			return PostContentType.MARKDOWN;
		}
	}

	// BoardPostEntity → PostResponseDto 변환 (상세 조회용)
	private PostResponseDto toPostResponseDto(BoardPostEntity post) {
		// 카테고리명 조회 (카테고리가 지정된 경우)
		String categoryName = null;
		if (post.getCategoryId() != null) {
			categoryName = categoryRepository.findById(post.getCategoryId())
					.map(cat -> cat.getName())
					.orElse(null);
		}

		// 태그 이름 목록 조회
		List<String> tagNames = tagService.getTagNamesByPostId(post.getId());

		// DTO 빌더로 변환
		return PostResponseDto.builder()
				.id(post.getId())
				.boardInstanceId(post.getBoardInstanceId())
				.categoryId(post.getCategoryId())
				.categoryName(categoryName)
				.parentId(post.getParentId())
				.depth(post.getDepth())
				.title(post.getTitle())
				.content(post.getContent())
				.contentType(post.getContentType() != null ? post.getContentType().name() : null)
				.slug(post.getSlug())
				.isSecret(post.getIsSecret())
				.isNotice(post.getIsNotice())
				.noticeScope(post.getNoticeScope() != null ? post.getNoticeScope().name() : null)
				.isDraft(post.getIsDraft())
				.authorId(post.getAuthorId())
				.authorName(post.getAuthorName())
				.viewCount(post.getViewCount() != null ? post.getViewCount().longValue() : 0L)
				.voteUpCount(post.getVoteUpCount())
				.voteDownCount(post.getVoteDownCount())
				.commentCount(post.getCommentCount())
				.metaTitle(post.getMetaTitle())
				.metaDescription(post.getMetaDescription())
				.tags(tagNames)
				.files(null)
				.createdAt(post.getCreatedAt())
				.updatedAt(post.getUpdatedAt())
				.build();
	}

	// BoardPostEntity → PostListResponseDto 변환 (목록 조회용, 경량)
	private PostListResponseDto toPostListResponseDto(BoardPostEntity post) {
		// 카테고리명 조회 (카테고리가 지정된 경우)
		String categoryName = null;
		if (post.getCategoryId() != null) {
			categoryName = categoryRepository.findById(post.getCategoryId())
					.map(cat -> cat.getName())
					.orElse(null);
		}

		// 목록용 경량 DTO 빌더로 변환
		return PostListResponseDto.builder()
				.id(post.getId())
				.boardInstanceId(post.getBoardInstanceId())
				.categoryId(post.getCategoryId())
				.categoryName(categoryName)
				.title(post.getTitle())
				.contentType(post.getContentType() != null ? post.getContentType().name() : null)
				.slug(post.getSlug())
				.isSecret(post.getIsSecret())
				.isNotice(post.getIsNotice())
				.noticeScope(post.getNoticeScope() != null ? post.getNoticeScope().name() : null)
				.isDraft(post.getIsDraft())
				.authorId(post.getAuthorId())
				.authorName(post.getAuthorName())
				.viewCount(post.getViewCount() != null ? post.getViewCount().longValue() : 0L)
				.voteUpCount(post.getVoteUpCount())
				.voteDownCount(post.getVoteDownCount())
				.commentCount(post.getCommentCount())
				.hasFiles(null)
				.createdAt(post.getCreatedAt())
				.updatedAt(post.getUpdatedAt())
				.build();
	}
}
