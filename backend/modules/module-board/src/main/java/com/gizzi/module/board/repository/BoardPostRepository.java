package com.gizzi.module.board.repository;

import com.gizzi.module.board.entity.BoardPostEntity;
import com.gizzi.module.board.entity.BoardPostTagEntity;
import com.gizzi.module.board.entity.NoticeScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 게시판 게시글 리포지토리 (tb_board_posts 테이블 접근)
public interface BoardPostRepository extends JpaRepository<BoardPostEntity, String> {

	// 게시판의 게시글 목록 (공지 우선, 최신순 — 고정 정렬)
	Page<BoardPostEntity> findByBoardInstanceIdAndIsDeletedFalseAndIsDraftFalseOrderByIsNoticeDescCreatedAtDesc(
			String boardInstanceId, Pageable pageable);

	// 카테고리별 게시글 목록 (고정 정렬)
	Page<BoardPostEntity> findByBoardInstanceIdAndCategoryIdAndIsDeletedFalseAndIsDraftFalseOrderByIsNoticeDescCreatedAtDesc(
			String boardInstanceId, String categoryId, Pageable pageable);

	// 게시판의 게시글 목록 (동적 정렬 — Pageable의 Sort 사용)
	Page<BoardPostEntity> findByBoardInstanceIdAndIsDeletedFalseAndIsDraftFalse(
			String boardInstanceId, Pageable pageable);

	// 카테고리별 게시글 목록 (동적 정렬 — Pageable의 Sort 사용)
	Page<BoardPostEntity> findByBoardInstanceIdAndCategoryIdAndIsDeletedFalseAndIsDraftFalse(
			String boardInstanceId, String categoryId, Pageable pageable);

	// 태그 필터 조회 (특정 태그가 부여된 게시글만 조회)
	@Query("SELECT DISTINCT p FROM BoardPostEntity p " +
			"JOIN BoardPostTagEntity pt ON pt.postId = p.id " +
			"WHERE p.boardInstanceId = :boardInstanceId " +
			"AND pt.tagId = :tagId " +
			"AND p.isDeleted = false AND p.isDraft = false")
	Page<BoardPostEntity> findByBoardInstanceIdAndTagId(
			@Param("boardInstanceId") String boardInstanceId,
			@Param("tagId") String tagId,
			Pageable pageable);

	// 공지글 목록
	List<BoardPostEntity> findByBoardInstanceIdAndIsNoticeTrueAndIsDeletedFalse(String boardInstanceId);

	// 범위별 공지글
	List<BoardPostEntity> findByBoardInstanceIdAndIsNoticeTrueAndNoticeScopeAndIsDeletedFalse(
			String boardInstanceId, NoticeScope scope);

	// 삭제되지 않은 게시글 수
	long countByBoardInstanceIdAndIsDeletedFalse(String boardInstanceId);

	// 전체 키워드 검색 (제목, 내용, 작성자명)
	@Query("SELECT p FROM BoardPostEntity p WHERE p.boardInstanceId = :boardInstanceId " +
			"AND p.isDeleted = false AND p.isDraft = false " +
			"AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword% OR p.authorName LIKE %:keyword%) " +
			"ORDER BY p.isNotice DESC, p.createdAt DESC")
	Page<BoardPostEntity> searchPosts(@Param("boardInstanceId") String boardInstanceId,
									  @Param("keyword") String keyword,
									  Pageable pageable);

	// 제목만 키워드 검색
	@Query("SELECT p FROM BoardPostEntity p WHERE p.boardInstanceId = :boardInstanceId " +
			"AND p.isDeleted = false AND p.isDraft = false " +
			"AND p.title LIKE %:keyword% " +
			"ORDER BY p.isNotice DESC, p.createdAt DESC")
	Page<BoardPostEntity> searchByTitle(@Param("boardInstanceId") String boardInstanceId,
										@Param("keyword") String keyword,
										Pageable pageable);

	// 작성자명만 키워드 검색
	@Query("SELECT p FROM BoardPostEntity p WHERE p.boardInstanceId = :boardInstanceId " +
			"AND p.isDeleted = false AND p.isDraft = false " +
			"AND p.authorName LIKE %:keyword% " +
			"ORDER BY p.isNotice DESC, p.createdAt DESC")
	Page<BoardPostEntity> searchByAuthor(@Param("boardInstanceId") String boardInstanceId,
										 @Param("keyword") String keyword,
										 Pageable pageable);

	// 답글 목록
	List<BoardPostEntity> findByParentId(String parentId);

	// 관리자용 전체 게시글 목록 (임시저장 포함)
	Page<BoardPostEntity> findByBoardInstanceIdAndIsDeletedFalseOrderByCreatedAtDesc(
			String boardInstanceId, Pageable pageable);
}
