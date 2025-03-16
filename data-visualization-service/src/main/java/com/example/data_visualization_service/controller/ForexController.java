package com.example.data_visualization_service.controller;

import com.example.data_visualization_service.document.ForexDocument;
import com.example.data_visualization_service.document.HistoryForexDocument;
import com.example.data_visualization_service.service.ForexService;
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
     * 오늘 00:00 ~ 현재까지 실시간 데이터
     */
    @GetMapping("/today")
    public Map<String, Object> getTodayData(@RequestParam String currencyCode) {
        List<ForexDocument> forexList = forexService.getTodayData(currencyCode);
        return forexService.buildChartDataForTodayOrYesterday(forexList);
    }

    /**
     * 어제 00:00 ~ 23:59:59 실시간 데이터
     */
    @GetMapping("/yesterday")
    public Map<String, Object> getYesterdayData(@RequestParam String currencyCode) {
        List<ForexDocument> forexList = forexService.getYesterdayData(currencyCode);
        return forexService.buildChartDataForTodayOrYesterday(forexList);
    }

    /**
     * 최근 1주일 간 히스토리 데이터
     */
    @GetMapping("/oneweek")
    public Map<String, Object> getOneWeekData(@RequestParam String currencyCode) {
        List<HistoryForexDocument> historyList = forexService.getOneWeekData(currencyCode);
        return forexService.buildChartDataForHistory(historyList);
    }

    /**
     * 최근 1개월 간 히스토리 데이터
     */
    @GetMapping("/onemonth")
    public Map<String, Object> getOneMonthData(@RequestParam String currencyCode) {
        List<HistoryForexDocument> historyList = forexService.getOneMonthData(currencyCode);
        return forexService.buildChartDataForHistory(historyList);
    }

    /**
     * 최근 3개월 간 히스토리 데이터
     */
    @GetMapping("/threemonth")
    public Map<String, Object> getThreeMonthData(@RequestParam String currencyCode) {
        List<HistoryForexDocument> historyList = forexService.getThreeMonthData(currencyCode);
        return forexService.buildChartDataForHistory(historyList);
    }
}
