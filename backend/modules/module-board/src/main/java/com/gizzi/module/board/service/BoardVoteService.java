package com.gizzi.module.board.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.module.board.dto.vote.VoteRequestDto;
import com.gizzi.module.board.dto.vote.VoteResponseDto;
import com.gizzi.module.board.entity.BoardCommentEntity;
import com.gizzi.module.board.entity.BoardPostEntity;
import com.gizzi.module.board.entity.BoardSettingsEntity;
import com.gizzi.module.board.entity.BoardVoteEntity;
import com.gizzi.module.board.entity.VoteTargetType;
import com.gizzi.module.board.entity.VoteType;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.repository.BoardCommentRepository;
import com.gizzi.module.board.repository.BoardPostRepository;
import com.gizzi.module.board.repository.BoardSettingsRepository;
import com.gizzi.module.board.repository.BoardVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// 게시판 투표(추천/비추천) 관리 서비스
// 게시글/댓글에 대한 추천(UP)/비추천(DOWN) 투표를 처리한다
// 동일 대상에 같은 타입 투표 → 투표 취소, 다른 타입 투표 → 투표 변경
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardVoteService {

	// 투표 리포지토리
	private final BoardVoteRepository     voteRepository;

	// 게시글 리포지토리 (카운트 갱신용)
	private final BoardPostRepository     postRepository;

	// 댓글 리포지토리 (카운트 갱신용)
	private final BoardCommentRepository  commentRepository;

	// 게시판 설정 리포지토리 (투표 허용 여부 확인용)
	private final BoardSettingsRepository settingsRepository;

	// ─── 게시글 투표 ───

	// 게시글에 대한 추천/비추천 투표
	@Transactional
	public VoteResponseDto votePost(String boardId, String postId, String userId, VoteRequestDto request) {
		// 게시판 설정에서 투표 기능 활성화 여부 확인
		validateVoteEnabled(boardId);

		// 게시글 조회 (삭제 여부 확인)
		BoardPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		// 삭제된 게시글에는 투표 불가
		if (post.getIsDeleted()) {
			throw new BusinessException(BoardErrorCode.BOARD_POST_NOT_FOUND);
		}

		// 본인 게시글에 투표 금지
		if (post.getAuthorId().equals(userId)) {
			throw new BusinessException(BoardErrorCode.BOARD_SELF_VOTE);
		}

		// 요청된 투표 타입 파싱 (UP / DOWN)
		VoteType requestedType = parseVoteType(request.getVoteType());

		// 기존 투표 조회
		Optional<BoardVoteEntity> existingVote = voteRepository.findByTargetTypeAndTargetIdAndUserId(
				VoteTargetType.POST, postId, userId);

		// 현재 사용자의 최종 투표 상태 (응답에 포함)
		String userVoteType;

		if (existingVote.isPresent()) {
			// 기존 투표가 있는 경우
			BoardVoteEntity vote = existingVote.get();

			if (vote.getVoteType() == requestedType) {
				// 같은 타입 투표 → 투표 취소
				voteRepository.delete(vote);

				// 비정규화 카운트 감소
				if (requestedType == VoteType.UP) {
					post.decrementVoteUpCount();
				} else {
					post.decrementVoteDownCount();
				}

				// 투표 취소 상태
				userVoteType = null;

				log.debug("게시글 투표 취소: postId={}, userId={}, type={}", postId, userId, requestedType);
			} else {
				// 다른 타입 투표 → 기존 투표 삭제 후 새 투표 생성
				voteRepository.delete(vote);

				// 기존 타입 카운트 감소
				if (vote.getVoteType() == VoteType.UP) {
					post.decrementVoteUpCount();
				} else {
					post.decrementVoteDownCount();
				}

				// 새 투표 생성
				BoardVoteEntity newVote = BoardVoteEntity.create(
						VoteTargetType.POST, postId, userId, requestedType);
				voteRepository.save(newVote);

				// 새 타입 카운트 증가
				if (requestedType == VoteType.UP) {
					post.incrementVoteUpCount();
				} else {
					post.incrementVoteDownCount();
				}

				// 변경된 투표 타입
				userVoteType = requestedType.name();

				log.debug("게시글 투표 변경: postId={}, userId={}, {} → {}",
						postId, userId, vote.getVoteType(), requestedType);
			}
		} else {
			// 기존 투표가 없는 경우 → 새 투표 생성
			BoardVoteEntity newVote = BoardVoteEntity.create(
					VoteTargetType.POST, postId, userId, requestedType);
			voteRepository.save(newVote);

			// 비정규화 카운트 증가
			if (requestedType == VoteType.UP) {
				post.incrementVoteUpCount();
			} else {
				post.incrementVoteDownCount();
			}

			// 새 투표 타입
			userVoteType = requestedType.name();

			log.debug("게시글 투표: postId={}, userId={}, type={}", postId, userId, requestedType);
		}

		// 게시글 카운트 변경 사항 저장
		postRepository.save(post);

		// 응답 DTO 생성
		return VoteResponseDto.builder()
				.voteUpCount(post.getVoteUpCount())
				.voteDownCount(post.getVoteDownCount())
				.userVoteType(userVoteType)
				.build();
	}

	// ─── 댓글 투표 ───

	// 댓글에 대한 추천/비추천 투표
	@Transactional
	public VoteResponseDto voteComment(String boardId, String commentId, String userId, VoteRequestDto request) {
		// 게시판 설정에서 투표 기능 활성화 여부 확인
		validateVoteEnabled(boardId);

		// 댓글 조회 (삭제 여부 확인)
		BoardCommentEntity comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_COMMENT_NOT_FOUND));

		// 삭제된 댓글에는 투표 불가
		if (comment.getIsDeleted()) {
			throw new BusinessException(BoardErrorCode.BOARD_COMMENT_NOT_FOUND);
		}

		// 본인 댓글에 투표 금지
		if (comment.getAuthorId().equals(userId)) {
			throw new BusinessException(BoardErrorCode.BOARD_SELF_VOTE);
		}

		// 요청된 투표 타입 파싱 (UP / DOWN)
		VoteType requestedType = parseVoteType(request.getVoteType());

		// 기존 투표 조회
		Optional<BoardVoteEntity> existingVote = voteRepository.findByTargetTypeAndTargetIdAndUserId(
				VoteTargetType.COMMENT, commentId, userId);

		// 현재 사용자의 최종 투표 상태 (응답에 포함)
		String userVoteType;

		if (existingVote.isPresent()) {
			// 기존 투표가 있는 경우
			BoardVoteEntity vote = existingVote.get();

			if (vote.getVoteType() == requestedType) {
				// 같은 타입 투표 → 투표 취소
				voteRepository.delete(vote);

				// 비정규화 카운트 감소
				if (requestedType == VoteType.UP) {
					comment.decrementVoteUpCount();
				} else {
					comment.decrementVoteDownCount();
				}

				// 투표 취소 상태
				userVoteType = null;

				log.debug("댓글 투표 취소: commentId={}, userId={}, type={}", commentId, userId, requestedType);
			} else {
				// 다른 타입 투표 → 기존 투표 삭제 후 새 투표 생성
				voteRepository.delete(vote);

				// 기존 타입 카운트 감소
				if (vote.getVoteType() == VoteType.UP) {
					comment.decrementVoteUpCount();
				} else {
					comment.decrementVoteDownCount();
				}

				// 새 투표 생성
				BoardVoteEntity newVote = BoardVoteEntity.create(
						VoteTargetType.COMMENT, commentId, userId, requestedType);
				voteRepository.save(newVote);

				// 새 타입 카운트 증가
				if (requestedType == VoteType.UP) {
					comment.incrementVoteUpCount();
				} else {
					comment.incrementVoteDownCount();
				}

				// 변경된 투표 타입
				userVoteType = requestedType.name();

				log.debug("댓글 투표 변경: commentId={}, userId={}, {} → {}",
						commentId, userId, vote.getVoteType(), requestedType);
			}
		} else {
			// 기존 투표가 없는 경우 → 새 투표 생성
			BoardVoteEntity newVote = BoardVoteEntity.create(
					VoteTargetType.COMMENT, commentId, userId, requestedType);
			voteRepository.save(newVote);

			// 비정규화 카운트 증가
			if (requestedType == VoteType.UP) {
				comment.incrementVoteUpCount();
			} else {
				comment.incrementVoteDownCount();
			}

			// 새 투표 타입
			userVoteType = requestedType.name();

			log.debug("댓글 투표: commentId={}, userId={}, type={}", commentId, userId, requestedType);
		}

		// 댓글 카운트 변경 사항 저장
		commentRepository.save(comment);

		// 응답 DTO 생성
		return VoteResponseDto.builder()
				.voteUpCount(comment.getVoteUpCount())
				.voteDownCount(comment.getVoteDownCount())
				.userVoteType(userVoteType)
				.build();
	}

	// ─── 내부 헬퍼 ───

	// 게시판 투표 기능 활성화 여부 검증
	private void validateVoteEnabled(String boardId) {
		// 게시판 설정 조회
		BoardSettingsEntity settings = settingsRepository.findById(boardId)
				.orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
		// 투표 기능 비활성화 시 예외 발생
		if (!settings.getAllowVote()) {
			throw new BusinessException(BoardErrorCode.BOARD_VOTE_DISABLED);
		}
	}

	// 투표 타입 문자열 파싱 (UP / DOWN)
	private VoteType parseVoteType(String type) {
		try {
			// 대문자로 변환 후 VoteType enum 파싱
			return VoteType.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			// 유효하지 않은 투표 타입
			throw new BusinessException(BoardErrorCode.BOARD_VOTE_DISABLED,
					"유효하지 않은 투표 타입입니다: " + type);
		}
	}
}
