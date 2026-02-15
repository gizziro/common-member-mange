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
