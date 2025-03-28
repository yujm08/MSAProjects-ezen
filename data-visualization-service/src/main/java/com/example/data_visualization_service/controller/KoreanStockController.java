package com.example.data_visualization_service.controller;

import com.example.data_visualization_service.document.KoreanDailyStockDocument;
import com.example.data_visualization_service.document.KoreanHistoryStockDocument;
import com.example.data_visualization_service.service.KoreanStockService;
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
     */
    @GetMapping("/today")
    public Map<String, Object> getTodayData(@RequestParam String stockCode) {
        List<KoreanDailyStockDocument> stockList = koreanStockService.getTodayData(stockCode);
        return koreanStockService.buildChartDataForTodayOrYesterday(stockList);
    }

    /**
     * 어제 데이터 (00:00 ~ 23:59:59)
     */
    @GetMapping("/yesterday")
    public Map<String, Object> getYesterdayData(@RequestParam String stockCode) {
        List<KoreanDailyStockDocument> stockList = koreanStockService.getYesterdayData(stockCode);
        return koreanStockService.buildChartDataForTodayOrYesterday(stockList);
    }

    /**
     * 1주일 히스토리
     */
    @GetMapping("/oneweek")
    public Map<String, Object> getOneWeekData(@RequestParam String stockCode) {
        List<KoreanHistoryStockDocument> historyList = koreanStockService.getOneWeekData(stockCode);
        return koreanStockService.buildChartDataForHistory(historyList);
    }

    /**
     * 1개월 히스토리
     */
    @GetMapping("/onemonth")
    public Map<String, Object> getOneMonthData(@RequestParam String stockCode) {
        List<KoreanHistoryStockDocument> historyList = koreanStockService.getOneMonthData(stockCode);
        return koreanStockService.buildChartDataForHistory(historyList);
    }

    /**
     * 3개월 히스토리
     */
    @GetMapping("/threemonth")
    public Map<String, Object> getThreeMonthData(@RequestParam String stockCode) {
        List<KoreanHistoryStockDocument> historyList = koreanStockService.getThreeMonthData(stockCode);
        return koreanStockService.buildChartDataForHistory(historyList);
    }
}
