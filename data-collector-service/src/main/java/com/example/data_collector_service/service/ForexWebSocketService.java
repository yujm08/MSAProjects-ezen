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
import java.util.concurrent.atomic.AtomicReference;

/**
 * **ForexWebSocketService**
 * - **WebSocket을 통해 실시간으로 USD/KRW 환율을 수신**
 * - **최신 환율 데이터를 메모리에 저장하고, 4분마다 DB에 저장**
 * - **환율이 변동된 경우에만 저장하여 불필요한 데이터 저장 방지**
 */
@Slf4j
@Service
public class ForexWebSocketService {

    private final OkHttpClient okHttpClient;
    private final DailyForexRepository dailyForexRepository;

    @Value("${twelvedata.websocket-url}")
    private String websocketUrl;

    @Value("${twelvedata.api-key}")
    private String apiKey;

    // WebSocket 객체 (연결 유지)
    private WebSocket webSocket;

    // **최신 환율을 저장하는 AtomicReference (Thread-Safe)**
    // AtomicReference<T>**는 멀티스레드 환경에서도 안전하게 객체를 읽고 쓰기 위한 클래스
    private final AtomicReference<BigDecimal> latestPrice = new AtomicReference<>(null);
    // **최신 환율 데이터가 수신된 시각**
    private final AtomicReference<LocalDateTime> latestTimestamp = new AtomicReference<>(null);

    public ForexWebSocketService(OkHttpClient okHttpClient,
                                 DailyForexRepository dailyForexRepository) {
        this.okHttpClient = okHttpClient;
        this.dailyForexRepository = dailyForexRepository;
    }

    /**
     * **서비스가 시작되면 WebSocket을 자동 연결**
     * - USD/KRW 환율 정보를 실시간으로 받아오기 시작
     */
    @PostConstruct
    public void connect() {
        // WebSocket 연결을 위한 URL 생성
        String fullUrl = websocketUrl + "?symbol=USD/KRW&apikey=" + apiKey;

        Request request = new Request.Builder()
                .url(fullUrl)
                .build();

        this.webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info(" [WebSocket] 연결 성공: {}", fullUrl);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    if (json.has("price")) {
                        BigDecimal price = json.getBigDecimal("price");
                        latestPrice.set(price);
                        latestTimestamp.set(LocalDateTime.now());
                        log.debug(" [WebSocket] 실시간 환율 수신 - USD/KRW: {}", price);
                    }
                } catch (Exception e) {
                    log.error(" [WebSocket] 메시지 파싱 오류: {}", text, e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error(" [WebSocket] 연결 실패: {}", t.getMessage(), t);
            }
        });

        log.info(" [WebSocket] USD/KRW 실시간 환율 수신을 시작합니다...");
    }

    /**
     * **4분마다 최신 환율을 DB에 저장 (ForexScheduledSaver에서 실행)**
     * - 최신 환율이 존재하면 DB에 저장
     * - 변동이 있을 때만 저장하여 불필요한 DB 저장 방지
     */
    public void saveLatestPriceIfChanged() {
        BigDecimal currentPrice = latestPrice.get();
        LocalDateTime currentTime = latestTimestamp.get();

        // **아직 데이터가 없으면 저장하지 않음**
        if (currentPrice == null || currentTime == null) {
            log.warn("⚠ [WebSocket] 저장할 USD/KRW 환율 데이터 없음 (아직 수신되지 않음)");
            return;
        }

        // **DB에서 가장 최근 저장된 환율 조회**
        DailyForex lastRecord = dailyForexRepository.findTopByCurrencyCodeOrderByTimestampDesc("USD/KRW");

        // **이전 값과 다를 경우에만 저장**
        if (shouldSave(lastRecord, currentPrice)) {
            // 변동률 계산
            BigDecimal changeRate = null;
            if (lastRecord != null && lastRecord.getExchangeRate().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal oldRate = lastRecord.getExchangeRate();
                changeRate = (currentPrice.subtract(oldRate))
                        .multiply(BigDecimal.valueOf(100))
                        .divide(oldRate, 2, BigDecimal.ROUND_HALF_UP);
            }

            // **DB에 저장할 새 환율 데이터 생성**
            DailyForex entity = DailyForex.builder()
                    .currencyCode("USD/KRW")
                    .currencyName("US Dollar / Korean Won")
                    .exchangeRate(currentPrice)
                    .changeRate(changeRate)
                    .timestamp(currentTime)
                    .build();

            dailyForexRepository.save(entity);
            log.info(" [WebSocket] 4분 저장 - USD/KRW: {}원, 변동률: {}%", currentPrice, changeRate);
        } else {
            log.debug(" [WebSocket] USD/KRW 환율 변동 없음 - 저장 생략");
        }
    }

    /**
     * **이전 데이터와 비교하여 저장이 필요한지 여부를 판단**
     * - 이전 데이터가 없거나, 값이 달라졌다면 저장 필요
     * param lastRecord 이전 저장된 환율 데이터
     * param currentPrice 현재 WebSocket에서 수신한 환율
     * return 저장 여부 (true = 저장 필요)
     */
    private boolean shouldSave(DailyForex lastRecord, BigDecimal currentPrice) {
        if (lastRecord == null) {
            return true; // DB에 기록이 없으면 저장
        }
        return currentPrice.compareTo(lastRecord.getExchangeRate()) != 0; // 값이 달라지면 저장
    }
}
