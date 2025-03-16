package com.example.data_collector_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class OAuthTokenService {

    // API 요청을 위한 기본 URL, 앱키, 앱시크릿 설정
    @Value("${kis.rest-url}")
    private String restUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    // 토큰 요청에 사용할 RestTemplate
    private final RestTemplate restTemplate = new RestTemplate();

    // 캐싱된 토큰과 토큰 발급 시각
    private volatile String cachedAccessToken;
    private volatile LocalDateTime tokenAcquiredTime;

    // 토큰 재발급 주기: 5시간 (5시간 = 5 * 60 * 60 초)
    private static final long REFRESH_INTERVAL_SECONDS = 5 * 60 * 60;

    // 동시 접근 방지를 위한 Lock
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 최신 접근 토큰을 반환합니다.
     * 만약 캐시된 토큰이 없거나, 토큰 발급 후 5시간이 경과했다면 새 토큰을 발급받습니다.
     *
     * @return 최신 접근 토큰 (문자열)
     */
    public String getAccessToken() {
        if (cachedAccessToken == null || isTokenRefreshNeeded()) {
            lock.lock();
            try {
                // double-check: 락을 걸고 다시 한번 확인
                if (cachedAccessToken == null || isTokenRefreshNeeded()) {
                    cachedAccessToken = requestNewAccessToken();
                    tokenAcquiredTime = LocalDateTime.now(); // 발급 시각 업데이트
                }
            } finally {
                lock.unlock();
            }
        }
        return cachedAccessToken;
    }

    /**
     * 토큰이 새로 발급되어야 하는지 확인합니다.
     * 현재 토큰이 없거나, 마지막 발급 시각으로부터 5시간 이상 경과했다면 true를 반환합니다.
     *
     * @return 새 토큰 발급 필요 여부 (true: 발급 필요, false: 캐시 사용)
     */
    private boolean isTokenRefreshNeeded() {
        if (tokenAcquiredTime == null) {
            return true;
        }
        // 현재 시각과 마지막 발급 시각의 차이를 초 단위로 계산
        long secondsSinceIssued = java.time.Duration.between(tokenAcquiredTime, LocalDateTime.now()).getSeconds();
        return secondsSinceIssued >= REFRESH_INTERVAL_SECONDS;
    }

    /**
     * /oauth2/tokenP 엔드포인트를 호출하여 새 접근 토큰을 발급받습니다.
     * 만약 403 Forbidden 에러(요청 제한)가 발생하면 60초 대기 후 재시도합니다.
     *
     * @return 새로 발급받은 접근 토큰 (문자열)
     */
    private String requestNewAccessToken() {
        String url = restUrl + "/oauth2/tokenP";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 본문 구성: grant_type, appkey, appsecret
        Map<String, String> body = Map.of(
            "grant_type", "client_credentials",
            "appkey", appKey,
            "appsecret", appSecret
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        log.info("[OAuthTokenService] Access Token 발급 요청: URL={}", url);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object tokenObj = response.getBody().get("access_token");
                if (tokenObj != null) {
                    log.info("[OAuthTokenService] Access Token 발급 성공");
                    return tokenObj.toString();
                }
            }
        } catch (HttpClientErrorException.Forbidden e) {
            // API 요청 제한 메시지 포함 시 60초 후 재시도
            if (e.getResponseBodyAsString().contains("접근토큰 발급 잠시 후 다시 시도하세요")) {
                log.warn("[OAuthTokenService] 토큰 발급 제한에 걸림: {}. 60초 후 재시도합니다.", e.getMessage());
                try {
                    Thread.sleep(60000); // 60초 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return requestNewAccessToken();
            }
            throw e;
        } catch (Exception e) {
            log.error("[OAuthTokenService] Access Token 발급 실패: {}", e.getMessage());
            throw new RuntimeException("Access Token 발급 실패", e);
        }
        log.error("[OAuthTokenService] Access Token 발급 실패");
        throw new RuntimeException("Access Token 발급 실패");
    }
}
