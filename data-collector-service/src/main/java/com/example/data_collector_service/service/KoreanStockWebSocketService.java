package com.example.data_collector_service.service;

import com.example.data_collector_service.buffer.RealTimeDataBuffer;
import com.example.data_collector_service.entity.KoreanDailyStock;
import com.example.data_collector_service.util.MarketTimeChecker;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * KoreanStockWebSocketService
 * 
 * 국내 주식 실시간체결가 데이터를 WebSocket을 통해 수집합니다.
 * 정규장(09:00 ~ 15:30) 시간 동안에만 동작하며, 응답 데이터를 파싱해 버퍼에 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KoreanStockWebSocketService {

    private final OkHttpClient okHttpClient;
    private final RealTimeDataBuffer dataBuffer;
    private final Gson gson = new Gson();
    private final OAuthApprovalService oauthService;

    @Value("${kis.ws-url-domestic}")
    private String wsUrlDomestic;

    /**
     * 국내 주식 종목 구독 시작 (예: stockCode = "005930")
     */
    public void subscribeKoreanStock(String stockCode) {
        if (!MarketTimeChecker.isKoreanMarketOpen()) {
            log.info("국내 정규장이 아니므로 국내 주식 구독을 시작하지 않습니다.");
            return;
        }
        String approvalKey = oauthService.getApprovalKey();
        Request request = new Request.Builder()
                .url(wsUrlDomestic + "/H0STCNT0")
                .addHeader("approval_key", approvalKey)
                .addHeader("custtype", "P")
                .addHeader("tr_type", "1")
                .addHeader("content-type", "utf-8")
                .build();

        okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("국내 주식 WebSocket 연결 성공, 종목: {}", stockCode);
                Map<String, Object> input = Map.of("tr_id", "H0STCNT0", "tr_key", stockCode);
                Map<String, Object> body = Map.of("input", input);
                String json = gson.toJson(Map.of("body", body));
                webSocket.send(json);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    // PING 메시지인지 확인 (예: JSON 형태이고 header.tr_id == "PINGPONG")
                    if (text.contains("\"tr_id\":\"PINGPONG\"")) {
                        log.debug("PING 메시지 수신, 무시합니다: {}", text);
                        return;
                    }

                    // 응답 예시: "0|H0STCNT0|004|005930^093929^71900^5^-100^-0.14^..."
                    String[] parts = text.split("\\|");
                    if (parts.length < 4) {
                        log.warn("국내 주식 응답 데이터 길이 부족: {}", text);
                        return;
                    }
                    
                    String dataPart = parts[3];
                    String[] tokens = dataPart.split("\\^");
                    if (tokens.length < 6) {
                        log.warn("국내 주식 응답 토큰 수 부족: {}", text);
                        return;
                    }
                    
                    KoreanDailyStock data = new KoreanDailyStock();
                    data.setStockCode(tokens[0]);  // 종목 코드
                    data.setStockName("미매핑");     // 실제 종목명은 SectorMaster 매핑 필요
                    data.setCurrentPrice(new BigDecimal(tokens[2])); // tokens[2]: 현재가
                    data.setChangeRate(new BigDecimal(tokens[5]));   // tokens[5]: 전일 대비율
                    data.setTimestamp(LocalDateTime.now());          // 한국 시간 기준
                    
                    dataBuffer.putKoreanData(data.getStockCode(), data);
                } catch (Exception e) {
                    log.error("국내 주식 데이터 파싱 오류: {}", e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                String errorMsg = (t != null) ? t.getMessage() : "Unknown error";
                log.error("국내 주식 WebSocket 오류: {}", errorMsg);
            }
        });
    }
}
