package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.GlobalDailyStock;
import com.example.data_collector_service.entity.GlobalHistoryStock;
import com.example.data_collector_service.entity.KoreanDailyStock;
import com.example.data_collector_service.entity.KoreanHistoryStock;
import com.example.data_collector_service.repository.GlobalDailyStockRepository;
import com.example.data_collector_service.repository.GlobalHistoryStockRepository;
import com.example.data_collector_service.repository.KoreanDailyStockRepository;
import com.example.data_collector_service.repository.KoreanHistoryStockRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
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

    // 해외 주식 종목 목록
    private static final List<ForeignStockInfo> FOREIGN_STOCKS = List.of(
        new ForeignStockInfo("TSLA",  "테슬라",         "NAS"),
        new ForeignStockInfo("AAPL",  "애플",           "NAS"),
        new ForeignStockInfo("NVDA",  "엔비디아",       "NAS"),
        new ForeignStockInfo("MSFT",  "마이크로소프트", "NAS"),
        new ForeignStockInfo("AMZN",  "아마존",         "NAS"),
        new ForeignStockInfo("GOOG",  "구글(알파벳)",   "NAS"),
        new ForeignStockInfo("META",  "메타(페이스북)", "NAS"),
        new ForeignStockInfo("AMD",   "AMD",           "NAS"),
        new ForeignStockInfo("NFLX",  "넷플릭스",       "NAS"),
        new ForeignStockInfo("BRK/B", "버크셔B주",      "NYS"),
        new ForeignStockInfo("TSM",   "TSMC",          "NYS"),
        new ForeignStockInfo("BABA",  "알리바바",       "NYS"),
        new ForeignStockInfo("NIO",   "니오(중국전기차)", "NYS"),
        new ForeignStockInfo("XOM",   "엑슨모빌",       "NYS"),
        new ForeignStockInfo("KO",    "코카콜라",       "NYS"),
        new ForeignStockInfo("JPM",   "JP모건",         "NYS"),
        new ForeignStockInfo("V",     "비자",           "NYS"),
        new ForeignStockInfo("09988","알리바바(홍콩)","HKS"),
        new ForeignStockInfo("09618","징둥닷컴",       "HKS"),
        new ForeignStockInfo("00700","텐센트",         "HKS")
    );

    /**
     * 매일 오전 7시 00분에 실행 (예시)
     * - 2일 전(D-2)의 Daily 데이터 중 마지막(종가)만 History에 저장
     * - 저장 후, 해당 날짜의 Daily 데이터를 삭제 (TRUNCATE 효과)
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void transferDailyToHistoryAndCleanDaily() {
        // 2일 전 날짜 계산 (예: 오늘이 5일이면, 3일 전의 데이터를 이관)
        LocalDate cutoffDate = LocalDate.now().minusDays(2);
        // cutoffDate의 시작과 끝 시간
        LocalDateTime start = cutoffDate.atStartOfDay();
        LocalDateTime end = cutoffDate.plusDays(1).atStartOfDay();

        // --- 국내 주식 처리 ---
        if (isWeekendForKoreanStocks(cutoffDate)) {
            log.info("오늘은 월요일/화요일이며, 2일 전 데이터가 주말(토요일/일요일)일 수 있어 삭제 작업을 건너뜁니다.");
        } else {
            List<String> koreanStockCodes = koreanDailyRepo.findDistinctStockCodesByTimestampBetween(start, end);
            for (String stockCode : koreanStockCodes) {
                boolean exists = koreanHistoryRepo.existsByStockCodeAndTimestamp(stockCode, cutoffDate);
                if (!exists) {
                    KoreanDailyStock lastRecord = koreanDailyRepo.findTopByStockCodeOrderByTimestampDesc(stockCode);
                    if (lastRecord != null && lastRecord.getTimestamp().toLocalDate().equals(cutoffDate)) {
                        KoreanHistoryStock history = KoreanHistoryStock.builder()
                                .stockCode(lastRecord.getStockCode())
                                .stockName(lastRecord.getStockName())
                                .closingPrice(lastRecord.getCurrentPrice())
                                .timestamp(cutoffDate)  // 날짜만 저장
                                .build();

                        koreanHistoryRepo.save(history);
                    }
                }
                koreanDailyRepo.deleteByStockCodeAndTimestampBetween(stockCode, start, end);
            }
        }

        // --- 해외 주식 처리 ---
        List<String> globalStockCodes = globalDailyRepo.findDistinctStockCodesByTimestampBetween(start, end);
        for (String stockCode : globalStockCodes) {
            if (isWeekendForForeignStocks(cutoffDate, stockCode)) {
                log.info("오늘은 주말이며, 2일 전 데이터가 주말(토요일/일요일)일 수 있어 삭제 작업을 건너뜁니다.");
                continue;
            }

            boolean exists = globalHistoryRepo.existsByStockCodeAndTimestamp(stockCode, cutoffDate);
            if (!exists) {
                GlobalDailyStock lastRecord = globalDailyRepo.findTopByStockCodeOrderByTimestampDesc(stockCode);
                if (lastRecord != null && lastRecord.getTimestamp().toLocalDate().equals(cutoffDate)) {
                    GlobalHistoryStock history = GlobalHistoryStock.builder()
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

    /**
     * 한국 주식에 대해 월요일 또는 화요일인 경우, 2일 전 데이터가 주말(토요일/일요일)일 수 있는지 확인
     * @param cutoffDate 2일 전 날짜
     * @return 월요일 또는 화요일이면 true, 아니면 false
     */
    private boolean isWeekendForKoreanStocks(LocalDate cutoffDate) {
        DayOfWeek dayOfWeek = cutoffDate.getDayOfWeek();
        return (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
    }

    /**
     * 해외 주식에 대해, 해당 주식 시장의 현지 시간대에 맞춰 2일 전 데이터가 주말인지 확인
     * @param cutoffDate 2일 전 날짜
     * @param stockCode  종목 코드 (이것으로 시장을 구분)
     * @return 주말이면 true, 아니면 false
     */
    private boolean isWeekendForForeignStocks(LocalDate cutoffDate, String stockCode) {
        // 현지 시간대와 시장에 따라 주말 여부를 체크
        String exchangeCode = getExchangeCodeByStockCode(stockCode);
        ZoneId zoneId = getZoneIdForMarket(exchangeCode);
        LocalDate localCutoffDate = cutoffDate.atStartOfDay(zoneId).toLocalDate();

        // 현지 시간이 주말이면 true 반환
        DayOfWeek dayOfWeek = localCutoffDate.getDayOfWeek();
        return (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
    }

    /**
     * 주어진 종목 코드에 해당하는 거래소를 반환
     * @param stockCode 종목 코드
     * @return 거래소 코드 (HKS, NAS, NYS 등)
     */
    private String getExchangeCodeByStockCode(String stockCode) {
        for (ForeignStockInfo stock : FOREIGN_STOCKS) {
            if (stock.getStockCode().equals(stockCode)) {
                return stock.getExchangeCode();
            }
        }
        return "NYS"; // 기본은 뉴욕
    }

    /**
     * 거래소 코드에 맞는 시간대 (zoneId)를 반환
     * @param exchangeCode 거래소 코드 (HKS, NAS, NYS)
     * @return ZoneId
     */
    private ZoneId getZoneIdForMarket(String exchangeCode) {
        switch (exchangeCode) {
            case "HKS":
                return ZoneId.of("Asia/Hong_Kong");  // 홍콩
            case "NAS":
            case "NYS":
                return ZoneId.of("America/New_York");  // 나스닥/뉴욕
            default:
                return ZoneId.of("Asia/Seoul");  // 기본은 서울
        }
    }

    /**
     * 해외 종목 정보 DTO (하드코딩용)
     */
    @Data
    @AllArgsConstructor
    public static class ForeignStockInfo {
        private String stockCode;     // "TSLA"
        private String stockName;     // "테슬라"
        private String exchangeCode;  // "NAS"
    }
}
