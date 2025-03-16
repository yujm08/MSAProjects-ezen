package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.DailyForex;
import com.example.data_collector_service.entity.HistoryForex;
import com.example.data_collector_service.repository.DailyForexRepository;
import com.example.data_collector_service.repository.HistoryForexRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryForexService {

    private final DailyForexRepository dailyForexRepository;
    private final HistoryForexRepository historyForexRepository;
    private final RestTemplate restTemplate;
    private final OAuthTokenService oAuthTokenService; // 엑세스토큰 발급용 서비스

    // KIS API 관련 설정 (application.yml에서 주입)
    @Value("${kis.rest-url}")
    private String kisRestUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    // accessToken은 동적으로 발급받으므로 @Value 없이 선언
    private String accessToken;

    @PostConstruct
    public void init() {
        // OAuthApprovalService를 통해 동적으로 accessToken 발급
        this.accessToken = oAuthTokenService.getAccessToken();
        log.info("HistoryForexService: accessToken 초기화 완료, accessToken={}", accessToken);
    }

    /**
     * 매일 오전 6시에 실행되는 스케줄러
     * [전체 처리 단계]
     * 1. (오늘 - 3개월)부터 (어제)까지의 날짜에 대해, 각 통화별 HistoryForex 기록이 누락되었으면 처리:
     *    - 해당 날짜의 DailyForex 데이터를 모두 삭제
     *    - KIS API를 호출하여 해당 날짜의 종가 데이터를 조회 후 HistoryForex에 저장(각 통화별 하루에 한 건)
     * 2. 3개월보다 오래된 HistoryForex 데이터는 삭제한다.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void processHistoryData() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(3);
        LocalDate endDate = today.minusDays(1); // 어제까지

        log.info("===== {}: History 데이터 채움 시작 ({} ~ {}) =====", LocalDateTime.now(), startDate, endDate);

        String[] currencyList = {"EUR/USD", "USD/KRW", "JPY/KRW"};
        for (String currency : currencyList) {
            LocalDate cursor = startDate;
            while (!cursor.isAfter(endDate)) {
                // 이틀 전 데이터 삭제: 한국 시간 기준으로 일요일이면 삭제 건너뛰기
                if (shouldSkipDeletion(cursor)) {
                    log.info("[{}] {}: 일요일 데이터이므로 삭제하지 않음", cursor, currency);
                    cursor = cursor.plusDays(1);
                    continue;
                }

                // History 기록이 이미 존재하면 넘어감
                if (!historyExists(currency, cursor)) {
                    // 해당 날짜의 DailyForex 데이터를 모두 삭제
                    deleteDailyForexForDate(currency, cursor);

                    // KIS API로 해당 날짜의 종가 데이터 조회
                    BigDecimal dailyRate = callKisApiForOneDay(currency, cursor);
                    if (dailyRate != null) {
                        // HistoryForex 레코드 생성 및 저장 (DailyForex와 연관 없이 독립적으로 저장)
                        saveHistoryData(currency, getCurrencyName(currency), dailyRate, cursor);
                        log.info("[{}] {}: HistoryForex 기록 생성 (종가: {})", cursor, currency, dailyRate);
                    } else {
                        log.warn("[{}] {}: KIS API 호출 실패", cursor, currency);
                    }
                } else {
                    log.info("[{}] {}: 이미 History 데이터 존재", cursor, currency);
                }
                cursor = cursor.plusDays(1);
            }
        }

        // 3. 3개월보다 오래된 HistoryForex 데이터 삭제
        cleanupOldHistoryData(today.minusMonths(3));

        log.info("===== {}: History 데이터 채움 완료 =====", LocalDateTime.now());
    }

    /**
     * 한국 시간 기준으로 2일 전이 일요일이면 삭제를 건너뛰는 메소드
     */
    private boolean shouldSkipDeletion(LocalDate date) {
        // 2일 전이 한국 시간 기준으로 일요일인지 확인
        LocalDateTime dateTime = date.atStartOfDay();
        ZonedDateTime koreaTime = dateTime.atZone(ZoneId.of("Asia/Seoul"));
        DayOfWeek dayOfWeek = koreaTime.getDayOfWeek();
        return (dayOfWeek == DayOfWeek.SUNDAY);  // 일요일일 경우 삭제 건너뛰기
    }

    /**
     * HistoryForex에 해당 통화, 해당 날짜의 기록이 존재하는지 확인한다.
     */
    private boolean historyExists(String currencyCode, LocalDate date) {
        List<HistoryForex> list = historyForexRepository.findByCurrencyCodeAndDate(currencyCode, date);
        return !list.isEmpty();
    }

    /**
     * 해당 통화와 날짜에 대해 DailyForex 데이터를 모두 삭제한다.
     */
    private void deleteDailyForexForDate(String currencyCode, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<DailyForex> dailyList = dailyForexRepository.findAllForDateAndCurrency(currencyCode, startOfDay, endOfDay);
        if (!dailyList.isEmpty()) {
            dailyForexRepository.deleteAll(dailyList);
            log.info("[{}] {}: 기존 DailyForex {}건 삭제", date, currencyCode, dailyList.size());
        }
    }

    /**
     * KIS API를 호출하여 하루치 환율 데이터를 조회하고, 종가(ovrs_nmix_prpr)를 반환한다.
     * API 문서에 따른 헤더 및 쿼리 파라미터를 설정한다.
     */
    private BigDecimal callKisApiForOneDay(String currencyCode, LocalDate date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateStr = date.format(formatter);

            String url = kisRestUrl + "/uapi/overseas-price/v1/quotations/inquire-daily-chartprice"
                    + "?FID_COND_MRKT_DIV_CODE=X"
                    + "&FID_INPUT_ISCD=" + currencyCode
                    + "&FID_INPUT_DATE_1=" + dateStr
                    + "&FID_INPUT_DATE_2=" + dateStr
                    + "&FID_PERIOD_DIV_CODE=D";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json; charset=utf-8");
            headers.set("authorization", "Bearer " + accessToken);
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "FHKST03030100");

            HttpEntity<String> entity = new HttpEntity<>("", headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = new JSONObject(response.getBody());
                if (json.has("output2")) {
                    var outputArray = json.getJSONArray("output2");
                    if (outputArray.length() > 0) {
                        JSONObject dayRecord = outputArray.getJSONObject(0);
                        String closingRateStr = dayRecord.getString("ovrs_nmix_prpr");
                        return new BigDecimal(closingRateStr);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("KIS API 호출 실패: {} - {} | Exception: ", currencyCode, date, e);
            return null;
        }
    }

    /**
     * API로 조회한 종가 데이터를 HistoryForex 테이블에 저장한다.
     */
    private void saveHistoryData(String currencyCode, String currencyName, BigDecimal price, LocalDate date) {
        HistoryForex history = HistoryForex.builder()
                .currencyCode(currencyCode)
                .currencyName(currencyName)
                .closingRate(price)
                .date(date)
                .build();
        historyForexRepository.save(history);
    }

    /**
     * 3개월보다 오래된 HistoryForex 데이터(등록일자가 cutoffDate 이전)를 삭제한다.
     */
    private void cleanupOldHistoryData(LocalDate cutoffDate) {
        int deletedCount = historyForexRepository.deleteByDateBefore(cutoffDate);
        log.info("Cleanup: {} HistoryForex 레코드 삭제 (등록일자 {} 이전)", deletedCount, cutoffDate);
    }

    /**
     * 통화 코드에 따른 통화 이름 반환
     */
    private String getCurrencyName(String currencyCode) {
        switch (currencyCode) {
            case "USD/KRW":
                return "US Dollar / Korean Won";
            case "JPY/KRW":
                return "Japanese Yen / Korean Won";
            case "EUR/USD":
                return "Euro / US Dollar";
            default:
                return "Unknown Currency";
        }
    }
}

