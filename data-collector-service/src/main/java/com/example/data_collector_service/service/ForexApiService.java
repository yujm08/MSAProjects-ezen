package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.DailyForex;
import com.example.data_collector_service.repository.DailyForexRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ForexApiService {

    @Value("${twelvedata.rest-url}")
    private String restUrl;

    @Value("${twelvedata.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate; // HTTP 요청을 위한 RestTemplate
    private final DailyForexRepository dailyForexRepository;

    public ForexApiService(RestTemplateBuilder builder,
                           DailyForexRepository dailyForexRepository) {
        this.restTemplate = builder.build();
        this.dailyForexRepository = dailyForexRepository;
    }

    /**
     * 🌍 **4분 간격으로 환율 데이터 조회 (JPY/KRW, EUR/KRW)**
     * - `cron = "0 0/4 * * * *"` → 매 4분마다 실행 (정각 기준 4분 단위)
     * - Rest API를 호출하여 최신 환율 데이터를 가져오고, DB에 저장
     */
    @Scheduled(cron = "0 0/4 * * * *") // 매 4분마다 실행 (예: 12:00, 12:04, 12:08 ...)
    public void fetchApiData() {
        log.info("📌 [ForexApiService] 4분 간격으로 환율 데이터 조회 시작...");
        fetchAndSave("JPY/KRW", "Japanese Yen / Korean Won"); // 일본 엔 환율 조회
        fetchAndSave("EUR/KRW", "Euro / Korean Won"); // 유로 환율 조회
        log.info("✅ [ForexApiService] 환율 데이터 조회 완료.");
    }

    /**
     * 🌍 **REST API를 호출하여 특정 통화쌍(JPY/KRW, EUR/KRW)의 환율 데이터를 가져와 저장**
     * currencyCode 통화 코드 (예: "JPY/KRW", "EUR/KRW")
     * currencyName 통화명 (예: "Japanese Yen / Korean Won", "Euro / Korean Won")
     */
    private void fetchAndSave(String currencyCode, String currencyName) {
        String url = String.format("%s/price?symbol=%s&apikey=%s", restUrl, currencyCode, apiKey);
        log.info("🔄 [{}] 환율 데이터 요청 중... (URL: {})", currencyCode, url);

        try {
            // API 요청을 보내고 JSON 응답을 받아옴
            String response = restTemplate.getForObject(url, String.class);

            if (response != null) {
                JSONObject json = new JSONObject(response); // JSON 파싱
                if (json.has("price")) {
                    BigDecimal currentPrice = json.getBigDecimal("price"); // 현재 환율
                    log.info("📥 [{}] API 응답 수신 성공. 현재 환율: {}", currencyCode, currentPrice);

                    // DB에서 최근 저장된 해당 통화 데이터 조회
                    DailyForex lastRecord = dailyForexRepository.findTopByCurrencyCodeOrderByTimestampDesc(currencyCode);

                    // 환율이 변동된 경우만 DB에 저장 (중복 저장 방지)
                    if (lastRecord == null || currentPrice.compareTo(lastRecord.getExchangeRate()) != 0) {
                        saveForexData(currencyCode, currencyName, currentPrice, LocalDateTime.now());
                    } else {
                        log.info("⏭ [{}] 환율 변동 없음. 저장 생략.", currencyCode);
                    }
                } else {
                    log.warn("⚠ [{}] API 응답에서 'price' 필드 없음. 응답: {}", currencyCode, response);
                }
            } else {
                log.warn("⚠ [{}] API 응답이 null.", currencyCode);
            }
        } catch (Exception e) {
            log.error("❌ [{}] 환율 데이터 조회 실패: {}", currencyCode, e.getMessage(), e);
        }
    }

    /**
     *  **DB에 환율 데이터 저장 + 변동률 계산**
     * currencyCode 통화 코드 (예: "JPY/KRW", "EUR/KRW")
     * currencyName 통화명 (예: "Japanese Yen / Korean Won", "Euro / Korean Won")
     * price 현재 환율
     * timestamp 데이터 저장 시간
     */
    private void saveForexData(String currencyCode, String currencyName, BigDecimal price, LocalDateTime timestamp) {
        // DB에서 해당 통화의 최근 기록 조회
        DailyForex lastRecord = dailyForexRepository.findTopByCurrencyCodeOrderByTimestampDesc(currencyCode);

        // 변동률(%) 계산
        BigDecimal changeRate = null;
        if (lastRecord != null && lastRecord.getExchangeRate().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal oldRate = lastRecord.getExchangeRate();
            BigDecimal diffPercent = (price.subtract(oldRate))
                    .multiply(BigDecimal.valueOf(100))
                    .divide(oldRate, 2, BigDecimal.ROUND_HALF_UP); // 변동률 소수점 2자리 반올림
            changeRate = diffPercent;
        }

        // 새로운 환율 데이터 객체 생성
        DailyForex dailyForex = DailyForex.builder()
                .currencyCode(currencyCode)
                .currencyName(currencyName)
                .exchangeRate(price)
                .changeRate(changeRate) // 변동률(%)
                .timestamp(timestamp)
                .build();

        // DB에 저장
        dailyForexRepository.save(dailyForex);
        log.info(" [{}] 환율 저장 완료: {} ({}원) - 변동률: {}%", timestamp, currencyCode, price, changeRate);
    }
}
