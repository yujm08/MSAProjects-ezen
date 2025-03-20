package com.example.data_collector_service.controller;

import com.example.data_collector_service.entity.KoreanDailyStock;
import com.example.data_collector_service.entity.KoreanHistoryStock;
import com.example.data_collector_service.visualizationService.KoreanStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 국내 주식 데이터(실시간/히스토리) 조회 API
 */
@RestController
@RequestMapping("/api/korean-stock")
@RequiredArgsConstructor
public class KoreanStockController {

    private final KoreanStockService koreanStockService;

    /**
     * 오늘 데이터 (00:00 ~ 현재)
     * 
     * @param stockCode 종목 코드
     * @return 오늘의 국내 주식 데이터 (Chart.js 포맷)
     */
    @GetMapping("/today")
    public Map<String, Object> getTodayData(@RequestParam String stockCode) {
        List<KoreanDailyStock> stockList = koreanStockService.getTodayData(stockCode);
        return koreanStockService.buildChartDataForTodayOrYesterday(stockList); // 수정된 부분
    }

    /**
     * 어제 데이터 (00:00 ~ 23:59:59)
     * 
     * @param stockCode 종목 코드
     * @return 어제의 국내 주식 데이터 (Chart.js 포맷)
     */
    @GetMapping("/yesterday")
    public Map<String, Object> getYesterdayData(@RequestParam String stockCode) {
        List<KoreanDailyStock> stockList = koreanStockService.getYesterdayData(stockCode);
        return koreanStockService.buildChartDataForTodayOrYesterday(stockList); // 수정된 부분
    }

    /**
     * 1주일 히스토리
     * 
     * @param stockCode 종목 코드
     * @return 1주일간의 국내 주식 히스토리 데이터 (Chart.js 포맷)
     */
    @GetMapping("/oneweek")
    public Map<String, Object> getOneWeekData(@RequestParam String stockCode) {
        List<KoreanHistoryStock> historyList = koreanStockService.getOneWeekData(stockCode);
        return koreanStockService.buildChartDataForHistory(historyList);
    }

    /**
     * 1개월 히스토리
     * 
     * @param stockCode 종목 코드
     * @return 1개월간의 국내 주식 히스토리 데이터 (Chart.js 포맷)
     */
    @GetMapping("/onemonth")
    public Map<String, Object> getOneMonthData(@RequestParam String stockCode) {
        List<KoreanHistoryStock> historyList = koreanStockService.getOneMonthData(stockCode);
        return koreanStockService.buildChartDataForHistory(historyList);
    }

    /**
     * 3개월 히스토리
     * 
     * @param stockCode 종목 코드
     * @return 3개월간의 국내 주식 히스토리 데이터 (Chart.js 포맷)
     */
    @GetMapping("/threemonth")
    public Map<String, Object> getThreeMonthData(@RequestParam String stockCode) {
        List<KoreanHistoryStock> historyList = koreanStockService.getThreeMonthData(stockCode);
        return koreanStockService.buildChartDataForHistory(historyList);
    }

    /**
     * 종목 코드에 대한 최신 국내 주식 요약 정보
     *
     * @param stockCode 종목 코드
     * @return 최신 국내 주식 요약 정보
     */
    @GetMapping("/summary")
    public Map<String, Object> getStockSummary(@RequestParam String stockCode) {
        return koreanStockService.getKoreanStockSummary(stockCode); // 최신 주식 요약 정보를 반환
    }
}
