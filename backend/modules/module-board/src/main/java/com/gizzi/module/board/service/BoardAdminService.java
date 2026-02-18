package com.gizzi.module.board.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.CommonErrorCode;
import com.gizzi.core.domain.user.entity.UserEntity;
import com.gizzi.core.domain.user.repository.UserRepository;
import com.gizzi.module.board.dto.admin.AddBoardAdminRequestDto;
import com.gizzi.module.board.dto.admin.BoardAdminResponseDto;
import com.gizzi.module.board.entity.BoardAdminEntity;
import com.gizzi.module.board.exception.BoardErrorCode;
import com.gizzi.module.board.repository.BoardAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// 게시판 부관리자 관리 서비스
// 게시판 인스턴스별 부관리자 추가/삭제/목록 조회를 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardAdminService {

	// 게시판 부관리자 리포지토리
	private final BoardAdminRepository adminRepository;

	// 사용자 리포지토리 (부관리자 이름 조회용)
	private final UserRepository       userRepository;

	// ─── 부관리자 목록 조회 ───

	// 게시판 인스턴스의 부관리자 목록 조회
	public List<BoardAdminResponseDto> getAdmins(String boardId) {
		// 게시판 인스턴스의 부관리자 엔티티 목록 조회
		List<BoardAdminEntity> admins = adminRepository.findByBoardInstanceId(boardId);

		// 각 부관리자에 대해 사용자명을 조회하여 DTO 변환
		return admins.stream()
				.map(admin -> {
					// 사용자 엔티티에서 이름 조회 (탈퇴 등으로 없을 수 있음)
					String username = userRepository.findById(admin.getUserId())
							.map(UserEntity::getUsername)
							.orElse(null);
					return toAdminResponseDto(admin, username);
				})
				.collect(Collectors.toList());
	}

	// ─── 부관리자 추가 ───

	// 게시판 부관리자 추가 (중복 확인 + 사용자 존재 확인)
	@Transactional
	public BoardAdminResponseDto addAdmin(String boardId, AddBoardAdminRequestDto request) {
		// 사용자 존재 여부 확인 (없으면 COM_RESOURCE_NOT_FOUND 예외)
		UserEntity user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
						"사용자를 찾을 수 없습니다: " + request.getUserId()));

		// 이미 부관리자로 등록된 사용자인지 중복 확인
		if (adminRepository.existsByBoardInstanceIdAndUserId(boardId, request.getUserId())) {
			throw new BusinessException(BoardErrorCode.BOARD_ADMIN_ALREADY_EXISTS);
		}

		// 부관리자 엔티티 생성 + DB 저장
		BoardAdminEntity admin = BoardAdminEntity.create(boardId, request.getUserId());
		adminRepository.save(admin);

		log.info("게시판 부관리자 추가: boardId={}, userId={}, username={}",
				boardId, request.getUserId(), user.getUsername());

		// 응답 DTO 변환 후 반환
		return toAdminResponseDto(admin, user.getUsername());
	}

	// ─── 부관리자 삭제 ───

	// 게시판 부관리자 삭제
	@Transactional
	public void removeAdmin(String boardId, String userId) {
		// 부관리자 존재 여부 확인 후 삭제
		if (!adminRepository.existsByBoardInstanceIdAndUserId(boardId, userId)) {
			throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
					"해당 부관리자를 찾을 수 없습니다");
		}

		// DB에서 부관리자 삭제
		adminRepository.deleteByBoardInstanceIdAndUserId(boardId, userId);

		log.info("게시판 부관리자 삭제: boardId={}, userId={}", boardId, userId);
	}

	// ─── Private 헬퍼 ───

	// BoardAdminEntity → BoardAdminResponseDto 변환
	private BoardAdminResponseDto toAdminResponseDto(BoardAdminEntity entity, String username) {
		return BoardAdminResponseDto.builder()
				.boardInstanceId(entity.getBoardInstanceId())
				.userId(entity.getUserId())
				.username(username)
				.createdAt(entity.getCreatedAt())
				.build();
	}
}
