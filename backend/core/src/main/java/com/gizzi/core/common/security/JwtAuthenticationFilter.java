package com.gizzi.core.common.security;

import com.gizzi.core.domain.auth.service.RedisTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// JWT 인증 필터: 모든 요청에서 Authorization 헤더의 JWT 토큰을 검증한다
// 유효한 토큰이면 SecurityContext에 인증 정보를 설정하고, 아니면 필터 체인을 계속 진행한다
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter
{
	//----------------------------------------------------------------------------------------------------------------------
	// [ 의존성 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final JwtTokenProvider  jwtTokenProvider;		// JWT 토큰 생성/파싱 컴포넌트
	private final RedisTokenService redisTokenService;		// Redis 토큰 세션 관리 서비스

	//----------------------------------------------------------------------------------------------------------------------
	// [ 상수 ]
	//----------------------------------------------------------------------------------------------------------------------
	private static final String AUTHORIZATION_HEADER	= "Authorization";		// Authorization 헤더 이름
	private static final String BEARER_PREFIX			= "Bearer ";			// Bearer 토큰 접두사

	//======================================================================================================================
	// 요청마다 JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정
	//======================================================================================================================
	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 1. Authorization 헤더에서 Bearer 토큰 추출
		//----------------------------------------------------------------------------------------------------------------------
		String token = extractToken(request);

		//----------------------------------------------------------------------------------------------------------------------
		// 2. 토큰이 없으면 인증 없이 필터 체인 계속
		//----------------------------------------------------------------------------------------------------------------------
		if (token == null)
		{
			filterChain.doFilter(request, response);
			return;
		}

		try
		{
			//----------------------------------------------------------------------------------------------------------------------
			// 3. JWT 서명 검증 + 클레임 파싱 (엄격 모드: 만료 시 예외)
			//----------------------------------------------------------------------------------------------------------------------
			Claims claims    = jwtTokenProvider.parseClaims(token);
			String userPk    = jwtTokenProvider.getUserPk(claims);
			String sessionId = jwtTokenProvider.getSessionId(claims);

			//----------------------------------------------------------------------------------------------------------------------
			// 4. Redis에서 Access Token 활성 상태 확인 (로그아웃 체크)
			//----------------------------------------------------------------------------------------------------------------------
			if (!redisTokenService.isAccessTokenActive(userPk, sessionId))
			{
				log.debug("비활성 Access Token: userPk={}, sessionId={}", userPk, sessionId);
				filterChain.doFilter(request, response);
				return;
			}

			//----------------------------------------------------------------------------------------------------------------------
			// 5. SecurityContext에 인증 정보 설정
			//----------------------------------------------------------------------------------------------------------------------
			UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(
					userPk,                                              // principal: 사용자 PK
					null,                                                // credentials: 불필요
					List.of(new SimpleGrantedAuthority("ROLE_USER"))     // 기본 권한
				);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (JwtException e)
		{
			// JWT 파싱 실패 (만료, 서명 불일치 등) → 인증 없이 계속
			log.debug("JWT 검증 실패: {}", e.getMessage());
		}

		//----------------------------------------------------------------------------------------------------------------------
		// 6. 다음 필터로 진행
		//----------------------------------------------------------------------------------------------------------------------
		filterChain.doFilter(request, response);
	}

	//----------------------------------------------------------------------------------------------------------------------
	// Authorization 헤더에서 Bearer 토큰 추출
	//----------------------------------------------------------------------------------------------------------------------
	private String extractToken(HttpServletRequest request)
	{
		// Authorization 헤더 값 조회
		String header = request.getHeader(AUTHORIZATION_HEADER);

		// "Bearer " 접두사가 있으면 토큰 문자열 반환
		if (header != null && header.startsWith(BEARER_PREFIX))
		{
			return header.substring(BEARER_PREFIX.length());
		}

		// 헤더가 없거나 Bearer 형식이 아니면 null 반환
		return null;
	}
}
