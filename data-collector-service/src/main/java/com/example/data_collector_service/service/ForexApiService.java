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

    private final RestTemplate restTemplate;
    private final DailyForexRepository dailyForexRepository;

    public ForexApiService(RestTemplateBuilder builder,
                           DailyForexRepository dailyForexRepository) {
        this.restTemplate = builder.build();
        this.dailyForexRepository = dailyForexRepository;
    }

    @Scheduled(cron = "0 0/4 * * * *")
    public void fetchApiData() {
        log.info("📌 [ForexApiService] 4분 간격으로 환율 데이터 조회 시작...");
        fetchAndSave("USD/KRW", "US Dollar / Korean Won");
        fetchAndSave("JPY/KRW", "Japanese Yen / Korean Won");
        log.info("✅ [ForexApiService] 환율 데이터 조회 완료.");
    }

    private void fetchAndSave(String currencyCode, String currencyName) {
        String url = String.format("%s/price?symbol=%s&apikey=%s", restUrl, currencyCode, apiKey);
        log.info("🔄 [{}] 환율 데이터 요청 중... (URL: {})", currencyCode, url);

        try {
            String response = restTemplate.getForObject(url, String.class);

            if (response != null) {
                JSONObject json = new JSONObject(response);
                if (json.has("price")) {
                    BigDecimal currentPrice = BigDecimal.valueOf(json.getDouble("price"));
                    log.info("📥 [{}] API 응답 수신 성공. 현재 환율: {}", currencyCode, currentPrice);

                    // 주말에는 저장하지 않기
                    if (isWeekend()) {
                        log.info("[ForexApiService] 주말에는 데이터 저장을 생략합니다.");
                        return;
                    }

                    DailyForex lastRecord = dailyForexRepository.findTopByCurrencyCodeOrderByTimestampDesc(currencyCode);

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

    private void saveForexData(String currencyCode, String currencyName, BigDecimal price, LocalDateTime timestamp) {
        DailyForex lastRecord = dailyForexRepository.findTopByCurrencyCodeOrderByTimestampDesc(currencyCode);

        BigDecimal changeRate = null;
        if (lastRecord != null && lastRecord.getExchangeRate().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal oldRate = lastRecord.getExchangeRate();
            BigDecimal diffPercent = (price.subtract(oldRate))
                    .multiply(BigDecimal.valueOf(100))
                    .divide(oldRate, 2, BigDecimal.ROUND_HALF_UP);
            changeRate = diffPercent;
        }

        DailyForex dailyForex = DailyForex.builder()
                .currencyCode(currencyCode)
                .currencyName(currencyName)
                .exchangeRate(price)
                .changeRate(changeRate)
                .timestamp(timestamp)
                .build();

        dailyForexRepository.save(dailyForex);
        log.info(" [{}] 환율 저장 완료: {} ({}원) - 변동률: {}%", timestamp, currencyCode, price, changeRate);
    }

    private boolean isWeekend() {
        LocalDateTime now = LocalDateTime.now();
        return now.getDayOfWeek().getValue() >= 6 && now.getHour() >= 7 && now.getDayOfWeek().getValue() <= 1 && now.getHour() < 6;
    }
}

