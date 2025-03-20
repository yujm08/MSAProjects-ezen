package com.example.data_collector_service.controller;

import com.example.data_collector_service.entity.DailyForex;
import com.example.data_collector_service.entity.HistoryForex;
import com.example.data_collector_service.visualizationService.ForexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 환율 데이터(실시간/히스토리)에 대한 조회 API
 * Chart.js가 요구하는 { labels, datasets } 구조로 JSON을 반환
 */
@RestController
@RequestMapping("/api/forex")
@RequiredArgsConstructor
public class ForexController {

    private final ForexService forexService;

    /**
     * 오늘 00:00 ~ 현재까지 실시간 환율 데이터
     * 
     * @param currencyCode 통화 코드
     * @return 실시간 환율 데이터 (Chart.js 포맷)
     */
    @GetMapping("/today")
    public Map<String, Object> getTodayData(@RequestParam String currencyCode) {
        List<DailyForex> forexList = forexService.getTodayData(currencyCode);
        return forexService.buildChartDataForDaily(forexList);
    }

    /**
     * 어제 00:00 ~ 23:59:59 실시간 환율 데이터
     * 
     * @param currencyCode 통화 코드
     * @return 어제의 환율 데이터 (Chart.js 포맷)
     */
    @GetMapping("/yesterday")
    public Map<String, Object> getYesterdayData(@RequestParam String currencyCode) {
        List<DailyForex> forexList = forexService.getYesterdayData(currencyCode);
        return forexService.buildChartDataForDaily(forexList);
    }

    /**
     * 최근 1주일 간 히스토리 데이터
     * 
     * @param currencyCode 통화 코드
     * @return 최근 1주일 간 환율 히스토리 데이터 (Chart.js 포맷)
     */
    @GetMapping("/oneweek")
    public Map<String, Object> getOneWeekData(@RequestParam String currencyCode) {
        List<HistoryForex> historyList = forexService.getOneWeekData(currencyCode);
        return forexService.buildChartDataForHistory(historyList);
    }

    /**
     * 최근 1개월 간 히스토리 데이터
     * 
     * @param currencyCode 통화 코드
     * @return 최근 1개월 간 환율 히스토리 데이터 (Chart.js 포맷)
     */
    @GetMapping("/onemonth")
    public Map<String, Object> getOneMonthData(@RequestParam String currencyCode) {
        List<HistoryForex> historyList = forexService.getOneMonthData(currencyCode);
        return forexService.buildChartDataForHistory(historyList);
    }

    /**
     * 최근 3개월 간 히스토리 데이터
     * 
     * @param currencyCode 통화 코드
     * @return 최근 3개월 간 환율 히스토리 데이터 (Chart.js 포맷)
     */
    @GetMapping("/threemonth")
    public Map<String, Object> getThreeMonthData(@RequestParam String currencyCode) {
        List<HistoryForex> historyList = forexService.getThreeMonthData(currencyCode);
        return forexService.buildChartDataForHistory(historyList);
    }

    /**
     * 통화 코드에 대한 최신 환율 요약 정보
     * 
     * @param currencyCode 통화 코드
     * @return 최신 환율 요약 정보
     */
    @GetMapping("/summary")
    public Map<String, Object> getForexSummary(@RequestParam String currencyCode) {
        return forexService.getForexSummary(currencyCode);
    }
}
