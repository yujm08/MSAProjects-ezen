package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.DailyForex;
import com.example.data_collector_service.repository.DailyForexRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ForexWebSocketService
 * ---------------------
 * - "subscribe" 액션(JSON) 방식을 사용하여 EUR/USD 심볼만 실시간 구독
 * - 4분마다 DB에 최신 환율을 저장 (변동이 있을 때만 저장)
 */
@Slf4j
@Service
public class ForexWebSocketService {

    private final OkHttpClient okHttpClient;
    private final DailyForexRepository dailyForexRepository;
    
    @Value("${twelvedata.websocket-url}")
    private String websocketUrl;  // 예) wss://ws.twelvedata.com/v1/price

    @Value("${twelvedata.api-key}")
    private String apiKey;

    private WebSocket webSocket;
    private BigDecimal latestPrice;
    private LocalDateTime latestTimestamp;

    public ForexWebSocketService(OkHttpClient okHttpClient,
                                 DailyForexRepository dailyForexRepository) {
        this.okHttpClient = okHttpClient;
        this.dailyForexRepository = dailyForexRepository;
    }

    @PostConstruct
    public void connect() {
        String fullUrl = websocketUrl + "?apikey=" + apiKey;
        Request request = new Request.Builder().url(fullUrl).build();

        this.webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("[WebSocket] 연결 성공: {}", fullUrl);
                JSONObject subscribeMsg = new JSONObject();
                subscribeMsg.put("action", "subscribe");
                JSONObject paramsObj = new JSONObject();
                paramsObj.put("symbols", "EUR/USD");  // 단일 심볼 구독
                subscribeMsg.put("params", paramsObj);
                webSocket.send(subscribeMsg.toString());
                log.info("[WebSocket] subscribe 메시지 전송: {}", subscribeMsg);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    String eventType = json.optString("event", "");
                    switch (eventType) {
                        case "subscribe-status":
                            String status = json.optString("status", "");
                            log.info("[WebSocket] subscribe-status 수신: status={}, raw={}", status, text);
                            break;
                        case "price":
                            String symbol = json.optString("symbol", "");
                            if (!"EUR/USD".equals(symbol)) return;
                            BigDecimal price = json.has("price")
                                    ? BigDecimal.valueOf(json.getDouble("price"))
                                    : null;
                            if (price == null) {
                                log.warn("[WebSocket] price 이벤트이지만 price 필드 누락: {}", text);
                                return;
                            }

                            // 주말에는 데이터 수집하지 않기
                            if (isWeekend()) {
                                log.info("[WebSocket] 주말에는 데이터 수집을 생략합니다.");
                                return;
                            }

                            latestPrice = price;
                            latestTimestamp = LocalDateTime.now();
                            log.debug("[WebSocket] 실시간 환율 수신 - {}: {}", symbol, price);
                            break;
                        default:
                            log.debug("[WebSocket] 기타 메시지 수신: {}", text);
                    }
                } catch (Exception e) {
                    log.error("[WebSocket] 메시지 파싱 오류: {}", text, e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("[WebSocket] 연결 실패: {}", t.getMessage(), t);
            }
        });

        log.info("[WebSocket] ForexWebSocketService 연결 시도 중...");
    }

    public void saveLatestPriceIfChanged() {
        if (latestPrice == null || latestTimestamp == null) {
            log.warn("[WebSocket] 저장할 EUR/USD 환율 데이터 없음 (아직 수신되지 않음)");
            return;
        }

        // 주말에는 저장하지 않기
        if (isWeekend()) {
            log.info("[WebSocket] 주말에는 데이터 저장을 생략합니다.");
            return;
        }

        DailyForex lastRecord = dailyForexRepository.findTopByCurrencyCodeOrderByTimestampDesc("EUR/USD");

        if (shouldSave(lastRecord, latestPrice)) {
            BigDecimal changeRate = calculateChangeRate(lastRecord, latestPrice);
            DailyForex entity = DailyForex.builder()
                    .currencyCode("EUR/USD")
                    .currencyName("Euro / US Dollar")
                    .exchangeRate(latestPrice)
                    .changeRate(changeRate)
                    .timestamp(latestTimestamp)
                    .build();

            dailyForexRepository.save(entity);
            log.info("[WebSocket] 4분 저장 - EUR/USD: {}원, 변동률: {}%", latestPrice, changeRate);
        } else {
            log.debug("[WebSocket] EUR/USD 환율 변동 없음 - 저장 생략");
        }
    }

    private boolean shouldSave(DailyForex lastRecord, BigDecimal currentPrice) {
        if (lastRecord == null) {
            return true; // 첫 저장
        }
        return currentPrice.compareTo(lastRecord.getExchangeRate()) != 0;
    }

    // 주말 확인
    private boolean isWeekend() {
        LocalDateTime now = LocalDateTime.now();
        return now.getDayOfWeek().getValue() >= 6 && now.getHour() >= 7 && now.getDayOfWeek().getValue() <= 1 && now.getHour() < 6;
    }

    private BigDecimal calculateChangeRate(DailyForex lastRecord, BigDecimal latestPrice) {
        if (lastRecord == null || lastRecord.getExchangeRate().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal oldRate = lastRecord.getExchangeRate();
        return latestPrice.subtract(oldRate)
                .multiply(BigDecimal.valueOf(100))
                .divide(oldRate, 2, BigDecimal.ROUND_HALF_UP);
    }
}
