package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.GlobalDailyStock;
import com.example.data_collector_service.entity.GlobalHistoryStock;
import com.example.data_collector_service.entity.KoreanDailyStock;
import com.example.data_collector_service.entity.KoreanHistoryStock;
import com.example.data_collector_service.repository.GlobalDailyStockRepository;
import com.example.data_collector_service.repository.GlobalHistoryStockRepository;
import com.example.data_collector_service.repository.KoreanDailyStockRepository;
import com.example.data_collector_service.repository.KoreanHistoryStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HistoryStockService
 * 
 * 매일 지정된 시각(여기서는 한국시간 오전 7시)에 Daily 테이블의 데이터 중,
 * **2일 전**(예: 오늘이 5일이면 3일 전)의 데이터를 History 테이블로 이관하고,
 * 해당 데이터를 Daily 테이블에서 삭제(truncate)합니다.
 * 또한, 매일 오전 7시 10분에 3달 이상된 History 데이터를 삭제하여 DB 용량을 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryStockService {

    // 국내 주식 Daily/History 레포지토리
    private final KoreanDailyStockRepository koreanDailyRepo;
    private final KoreanHistoryStockRepository koreanHistoryRepo;

    // 해외 주식 Daily/History 레포지토리
    private final GlobalDailyStockRepository globalDailyRepo;
    private final GlobalHistoryStockRepository globalHistoryRepo;

    /**
     * 매일 오전 7시 00분에 실행 (예시)
     * - 2일 전(D-2)의 Daily 데이터 중 마지막(종가)만 History에 저장
     * - 저장 후, 해당 날짜의 Daily 데이터를 삭제 (TRUNCATE 효과)
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void transferDailyToHistoryAndCleanDaily() {
        // 2일 전 날짜 계산 (예: 오늘이 5일이면, 3일째 데이터를 이관)
        LocalDate cutoffDate = LocalDate.now().minusDays(2);
        // cutoffDate의 시작과 끝 시간
        LocalDateTime start = cutoffDate.atStartOfDay();
        LocalDateTime end = cutoffDate.plusDays(1).atStartOfDay();

        // --- 국내 주식 처리 ---
        // 1) cutoffDate에 해당하는 국내 종목 코드 목록 (중복 없이)
        List<String> koreanStockCodes = koreanDailyRepo.findDistinctStockCodesByTimestampBetween(start, end);
        for (String stockCode : koreanStockCodes) {
            // 2) 이미 History에 저장된지 체크 (하루 1건만 저장)
            boolean exists = koreanHistoryRepo.existsByStockCodeAndTimestamp(stockCode, cutoffDate);
            if (!exists) {
                // 3) cutoffDate에 해당 종목의 마지막 Daily 데이터 (종가로 간주)
                KoreanDailyStock lastRecord = koreanDailyRepo.findTopByStockCodeOrderByTimestampDesc(stockCode);
                if (lastRecord != null && lastRecord.getTimestamp().toLocalDate().equals(cutoffDate)) {
                    // 4) History 엔티티 생성
                    KoreanHistoryStock history = KoreanHistoryStock.builder()
                            .koreanDailyStockId(lastRecord.getId())
                            .stockCode(lastRecord.getStockCode())
                            .stockName(lastRecord.getStockName())
                            .exchangeCode(lastRecord.getExchangeCode())
                            .closingPrice(lastRecord.getCurrentPrice())
                            .timestamp(cutoffDate)  // 날짜만 저장
                            .build();

                    koreanHistoryRepo.save(history);
                }
            }
            // 5) cutoffDate에 해당하는 Daily 데이터 삭제
            koreanDailyRepo.deleteByStockCodeAndTimestampBetween(stockCode, start, end);
        }

        // --- 해외 주식 처리 ---
        List<String> globalStockCodes = globalDailyRepo.findDistinctStockCodesByTimestampBetween(start, end);
        for (String stockCode : globalStockCodes) {
            boolean exists = globalHistoryRepo.existsByStockCodeAndTimestamp(stockCode, cutoffDate);
            if (!exists) {
                GlobalDailyStock lastRecord = globalDailyRepo.findTopByStockCodeOrderByTimestampDesc(stockCode);
                if (lastRecord != null && lastRecord.getTimestamp().toLocalDate().equals(cutoffDate)) {
                    GlobalHistoryStock history = GlobalHistoryStock.builder()
                            .globalDailyStockId(lastRecord.getId())
                            .stockCode(lastRecord.getStockCode())
                            .exchangeCode(lastRecord.getExchangeCode())
                            .closingPrice(lastRecord.getCurrentPrice())
                            .timestamp(cutoffDate)
                            .build();

                    globalHistoryRepo.save(history);
                }
            }
            globalDailyRepo.deleteByStockCodeAndTimestampBetween(stockCode, start, end);
        }

        log.info("{}의 Daily 데이터가 History로 이관되고, Daily 테이블이 정리되었습니다.", cutoffDate);
    }

    /**
     * 매일 오전 7시 10분에 실행 (예시)
     * - 3달 이상된 History 데이터를 삭제하여 DB 용량을 관리합니다.
     */
    @Scheduled(cron = "0 10 7 * * *", zone = "Asia/Seoul")
    public void cleanupOldHistoryData() {
        LocalDate cutoffDate = LocalDate.now().minusMonths(3);

        // 국내 주식 History 데이터 삭제
        koreanHistoryRepo.deleteByTimestampBefore(cutoffDate);

        // 해외 주식 History 데이터 삭제
        globalHistoryRepo.deleteByTimestampBefore(cutoffDate);

        log.info("3달 이상된 History 데이터가 삭제되었습니다. (cutoff: {})", cutoffDate);
    }
}
