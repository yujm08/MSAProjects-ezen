package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.DailyForex;
import com.example.data_collector_service.entity.HistoryForex;
import com.example.data_collector_service.repository.DailyForexRepository;
import com.example.data_collector_service.repository.HistoryForexRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

/**
 * **HistoryForexService**
 * - **매일 오전 6시에 2일 전 DailyForex 데이터를 HistoryForex로 이관**
 * - **이관 후 DailyForex에서 해당 날짜의 데이터를 삭제**
 */
@Slf4j
@Service
public class HistoryForexService {

    private final DailyForexRepository dailyForexRepository;
    private final HistoryForexRepository historyForexRepository;

    public HistoryForexService(DailyForexRepository dailyForexRepository,
                               HistoryForexRepository historyForexRepository) {
        this.dailyForexRepository = dailyForexRepository;
        this.historyForexRepository = historyForexRepository;
    }

    /**
     * **매일 오전 6시 실행**
     * - `cron = "0 0 6 * * *"` → 매일 오전 6시 정각 실행
     * - **2일 전 환율 데이터를 HistoryForex 테이블로 이동**
     * - **이동 후 DailyForex 테이블에서 해당 데이터 삭제**
     */
    @Scheduled(cron = "0 0 6 * * *") // 매일 오전 6시 실행
    public void transferDailyToHistory() {
        LocalDate targetDate = LocalDate.now().minusDays(2);
        log.info(" [{}] 2일 전 데이터 이관 시작...", targetDate);

        // 이관할 통화 목록
        String[] currencyList = {"USD/KRW", "JPY/KRW", "EUR/KRW"};
        for (String currency : currencyList) {
            moveDailyToHistory(currency, targetDate);
        }

        log.info(" [{}] 2일 전 데이터 이관 완료.", targetDate);
    }

    /**
     *  **DailyForex 데이터를 HistoryForex로 이동 후 삭제**
     * - **가장 마지막(최신) 시각의 데이터를 "종가"로 저장**
     * - **이관 후 DailyForex에서 해당 날짜 데이터 삭제**
     * @param currencyCode 통화 코드 (예: "USD/KRW", "JPY/KRW", "EUR/KRW")
     * @param targetDate 이관할 날짜 (2일 전 날짜)
     */
    private void moveDailyToHistory(String currencyCode, LocalDate targetDate) {
        LocalDateTime startOfDay = targetDate.atStartOfDay(); // 00:00:00
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX); // 23:59:59

        // 해당 날짜의 모든 환율 기록 조회 (timestamp DESC 정렬)
        List<DailyForex> dailyList = dailyForexRepository.findAllForDateAndCurrency(currencyCode, startOfDay, endOfDay);

        if (!dailyList.isEmpty()) {
            // **종가 (Closing Rate): 가장 마지막 시각의 데이터**
            DailyForex closeRecord = dailyList.get(0); // DESC 정렬이므로 첫 번째 항목이 종가

            // **HistoryForex 객체 생성 후 저장**
            HistoryForex history = HistoryForex.builder()
                    .dailyForexId(closeRecord.getId()) // 원본 DailyForex ID 저장
                    .currencyCode(closeRecord.getCurrencyCode())
                    .currencyName(closeRecord.getCurrencyName())
                    .closingRate(closeRecord.getExchangeRate()) // 종가 저장
                    .date(targetDate) // 2일 전 날짜
                    .build();

            historyForexRepository.save(history);

            // **DailyForex 테이블에서 해당 날짜 데이터 삭제**
            dailyForexRepository.deleteAll(dailyList);

            log.info(" [{}] {} 데이터 {}건 -> HistoryForex 이관 완료 (종가: {})",
                    targetDate, currencyCode, dailyList.size(), closeRecord.getExchangeRate());
        } else {
            log.warn("⚠ [{}] {} 데이터 없음 - 이관 생략", targetDate, currencyCode);
        }
    }
}
