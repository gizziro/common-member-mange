// 백엔드 공통 응답 래퍼 (ApiResponse<T> 매핑)
export interface ApiResponse<T> {
	success: boolean;
	data?: T;
	error?: ErrorDetail;
}

// 에러 상세 정보
export interface ErrorDetail {
	code: string;
	message: string;
}

// 회원가입 요청 DTO (백엔드 SignupRequest 매핑)
export interface SignupRequest {
	userId: string;
	password: string;
	username: string;
	email: string;
}

// 회원가입 응답 DTO (백엔드 SignupResponse 매핑)
export interface SignupResponse {
	id: string;
	userId: string;
	username: string;
	email: string;
	userStatus: string;
	createdAt: string;
}

// 현재 사용자 정보 응답 DTO (백엔드 UserMeResponseDto 매핑)
export interface UserMeResponse {
	id: string;
	userId: string;
	username: string;
	email: string;
	userStatus: string;
	provider: string;
	phone: string | null;
	isSmsAgree: boolean | null;
}

// 로그인 요청 DTO (백엔드 LoginRequestDto 매핑)
export interface LoginRequest {
	userId: string;
	password: string;
}

// 로그인 응답 DTO (백엔드 LoginResponseDto 매핑)
export interface LoginResponse {
	accessToken: string;
	refreshToken: string;
	userId: string;
	username: string;
	email: string;
}

// 백엔드 PageResponseDto 매핑 (페이지네이션 응답 공통 타입)
export interface PageResponse<T> {
	content: T[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}
