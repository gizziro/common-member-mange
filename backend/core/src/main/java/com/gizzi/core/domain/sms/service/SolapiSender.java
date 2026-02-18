package com.gizzi.core.domain.sms.service;

import com.gizzi.core.common.exception.BusinessException;
import com.gizzi.core.common.exception.SmsErrorCode;
import com.gizzi.core.domain.sms.entity.SmsProviderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

// SOLAPI SMS 발송 구현체
// SOLAPI REST API를 직접 호출한다 (SDK 미사용, RestClient 사용)
// 인증: HMAC-SHA256 서명 기반 Authorization 헤더
@Slf4j
@Component
public class SolapiSender implements SmsProviderSender {

	// SOLAPI 메시지 발송 API 엔드포인트
	private static final String SOLAPI_API_URL = "https://api.solapi.com/messages/v4/send";

	// HMAC 알고리즘
	private static final String HMAC_SHA256    = "HmacSHA256";

	// RestClient 인스턴스 (Spring Boot 4.0 권장)
	private final RestClient restClient = RestClient.create();

	@Override
	public String getProviderCode() {
		return "solapi";
	}

	// SOLAPI REST API를 호출하여 SMS 발송
	@Override
	public void send(SmsProviderEntity provider, String to, String message) {
		// API Key, Secret 미설정 시 에러
		if (provider.getApiKey() == null || provider.getApiSecret() == null) {
			throw new BusinessException(SmsErrorCode.SMS_SEND_FAILED);
		}

		try {
			// HMAC-SHA256 서명 생성 (date는 ISO 8601 형식 필수)
			String date      = Instant.now().toString();
			String salt      = UUID.randomUUID().toString();
			String signature = generateSignature(provider.getApiSecret(), date, salt);

			// Authorization 헤더 값 조합
			String authorization = "HMAC-SHA256 apiKey=" + provider.getApiKey()
				+ ", date=" + date
				+ ", salt=" + salt
				+ ", signature=" + signature;

			// 요청 바디 JSON (수동 빌더 — ObjectMapper 미사용)
			// JSON 특수문자 이스케이프 (줄바꿈, 탭, 따옴표, 백슬래시 등)
			String requestBody = String.format(
				"{\"message\":{\"to\":\"%s\",\"from\":\"%s\",\"text\":\"%s\"}}",
				escapeJson(to), escapeJson(provider.getSenderNumber()), escapeJson(message)
			);

			// SOLAPI API 호출
			String response = restClient.post()
				.uri(SOLAPI_API_URL)
				.header("Authorization", authorization)
				.header("Content-Type", "application/json")
				.body(requestBody)
				.retrieve()
				.body(String.class);

			log.info("SOLAPI SMS 발송 성공: to={}, response={}", to, response);

		} catch (BusinessException e) {
			throw e;
		} catch (HttpStatusCodeException e) {
			// SOLAPI API가 HTTP 에러 응답을 반환한 경우 — 상세 에러 메시지 추출
			String responseBody = e.getResponseBodyAsString();
			String detailMessage = extractSolapiErrorMessage(responseBody);
			log.error("SOLAPI SMS 발송 실패: to={}, status={}, body={}", to, e.getStatusCode(), responseBody);
			throw new BusinessException(SmsErrorCode.SMS_SEND_FAILED, detailMessage);
		} catch (Exception e) {
			log.error("SOLAPI SMS 발송 실패: to={}, error={}", to, e.getMessage(), e);
			throw new BusinessException(SmsErrorCode.SMS_SEND_FAILED);
		}
	}

	// SOLAPI 응답 바디에서 에러 메시지 추출 (ObjectMapper 미사용, 수동 파싱)
	// 응답 예시: {"errorCode":"FailedToAddMessage","errorMessage":"발신번호 미등록"}
	private String extractSolapiErrorMessage(String responseBody) {
		try {
			// "errorMessage":"..." 패턴에서 메시지 추출
			String marker = "\"errorMessage\":\"";
			int start = responseBody.indexOf(marker);
			if (start >= 0) {
				start += marker.length();
				int end = responseBody.indexOf("\"", start);
				if (end > start) {
					return "SOLAPI: " + responseBody.substring(start, end);
				}
			}
		} catch (Exception ignored) {
			// 파싱 실패 시 기본 메시지 사용
		}
		// 파싱 실패 시 원본 응답 바디 포함
		return "SMS 발송에 실패했습니다 (" + responseBody + ")";
	}

	// JSON 문자열 값에 포함될 특수문자를 이스케이프한다
	// 줄바꿈, 탭, 따옴표, 백슬래시 등을 JSON 호환 형태로 변환
	private String escapeJson(String value) {
		if (value == null) return "";
		return value
			.replace("\\", "\\\\")   // 백슬래시 (가장 먼저)
			.replace("\"", "\\\"")   // 큰따옴표
			.replace("\n", "\\n")    // 줄바꿈
			.replace("\r", "\\r")    // 캐리지 리턴
			.replace("\t", "\\t");   // 탭
	}

	// HMAC-SHA256 서명 생성 (date + salt를 Secret Key로 서명)
	private String generateSignature(String apiSecret, String date, String salt) {
		try {
			// 서명 대상 문자열: date + salt
			String data = date + salt;
			// HMAC-SHA256 Mac 인스턴스 생성
			Mac mac = Mac.getInstance(HMAC_SHA256);
			// Secret Key 설정
			SecretKeySpec secretKeySpec = new SecretKeySpec(
				apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256
			);
			mac.init(secretKeySpec);
			// 서명 생성 후 16진수 문자열로 변환
			byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			log.error("HMAC 서명 생성 실패: {}", e.getMessage(), e);
			throw new BusinessException(SmsErrorCode.SMS_SEND_FAILED);
		}
	}
}
