package com.example.data_collector_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/*/oauth2/Approval API를 호출하여 WebSocket 접속키(approval_key)를 발급받습니다.
  모의계좌 환경에 맞게 모의 도메인을 사용할 수 있습니다. */

@Slf4j
@Service
public class OAuthApprovalService {

    @Value("${kis.rest-url}")
    private String restUrl;

    @Value("${kis.app-key}")
    private String appKey;    

    @Value("${kis.app-secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getApprovalKey() {  //승인 키(approval_key)를 발급받음. 발급받은 키는 WebSocket 연결 시 사용됨
        String url = restUrl + "/oauth2/Approval";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "secretkey", appSecret
        );
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers); //HttpEntity<>(body, headers) → HTTP 요청 객체 생성
        //POST 요청을 보내고, 응답을 Map<String, Object> 형태로 받음
        log.info("-------------------승인 키 발급 요청");
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Object approvalKey = response.getBody().get("approval_key");
            if (approvalKey != null) {
                log.info("---------------Approval Key 발급 성공");
                return approvalKey.toString();
            }
        }
        log.error("Approval Key 발급 실패");
        throw new RuntimeException("---------------Approval Key 발급 실패");
    }
}
