package com.example.data_collector_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class OAuthApprovalKeyService {

    @Value("${kis.rest-url}")
    private String restUrl;

    @Value("${kis.app-key}")
    private String appKey;    

    @Value("${kis.app-secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // 캐싱된 Approval Key와 발급 시각
    private volatile String cachedApprovalKey;
    private volatile LocalDateTime approvalKeyAcquiredTime;

    // Approval Key의 기본 유효기간: 24시간 (86400초)
    private final long approvalKeyExpiresIn = 86400;
    // 만료 전 여유 시간 (예: 4시간), 즉 20시간 이상 경과 시 재발급
    private final long renewalMarginSeconds = 14400;

    // 동시 접근 제어용 Lock
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * WebSocket 접속용 Approval Key를 반환합니다.
     * 토큰이 없거나 만료 임박 시 새로 발급받고, 그렇지 않으면 캐시된 값을 반환합니다.
     *
     * @return Approval Key (문자열)
     */
    public String getApprovalKey() {
        if (cachedApprovalKey == null || isApprovalKeyExpired()) {
            lock.lock();
            try {
                // Double-check
                if (cachedApprovalKey == null || isApprovalKeyExpired()) {
                    cachedApprovalKey = requestNewApprovalKey();
                    approvalKeyAcquiredTime = LocalDateTime.now();
                }
            } finally {
                lock.unlock();
            }
        }
        return cachedApprovalKey;
    }

    /**
     * Approval Key가 만료되었거나 갱신해야 하는지 판단합니다.
     * 기본적으로 24시간 유효하지만, 4시간 여유를 두어 20시간 경과 시 재발급합니다.
     *
     * @return 만료되었으면 true, 아니면 false
     */
    private boolean isApprovalKeyExpired() {
        if (approvalKeyAcquiredTime == null) {
            return true;
        }
        Duration elapsed = Duration.between(approvalKeyAcquiredTime, LocalDateTime.now());
        return elapsed.getSeconds() >= (approvalKeyExpiresIn - renewalMarginSeconds);
    }

    /**
     * REST API /oauth2/Approval 엔드포인트를 호출하여 새 Approval Key를 발급받습니다.
     *
     * @return 새로 발급된 Approval Key
     */
    private String requestNewApprovalKey() {
        String url = restUrl + "/oauth2/Approval";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // body에 grant_type, appkey, secretkey(=appsecret) 전달
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "secretkey", appSecret
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        log.info("[OAuthApprovalKeyService] Approval Key 발급 요청: URL={}", url);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Object approvalKey = response.getBody().get("approval_key");
            if (approvalKey != null) {
                log.info("[OAuthApprovalKeyService] Approval Key 발급 성공");
                return approvalKey.toString();
            }
        }
        log.error("[OAuthApprovalKeyService] Approval Key 발급 실패");
        throw new RuntimeException("Approval Key 발급 실패");
    }
}
