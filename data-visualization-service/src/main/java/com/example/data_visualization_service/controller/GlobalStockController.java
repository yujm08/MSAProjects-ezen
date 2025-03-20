package com.example.data_visualization_service.controller;

import com.example.data_visualization_service.document.GlobalDailyStockDocument;
import com.example.data_visualization_service.document.GlobalHistoryStockDocument;
import com.example.data_visualization_service.service.GlobalStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 해외 주식 데이터(실시간/히스토리) 조회 API
 */
@RestController
@RequestMapping("/api/global-stock")
@RequiredArgsConstructor
public class GlobalStockController {

    private final GlobalStockService globalStockService;

    /**
     * 오늘 데이터 (00:00 ~ 현재)
     */
    @GetMapping("/today")
    public Map<String, Object> getTodayData(@RequestParam String stockCode) {
        List<GlobalDailyStockDocument> stockList = globalStockService.getTodayData(stockCode);
        return globalStockService.buildChartDataForTodayOrYesterday(stockList);
    }

    /**
     * 어제 데이터 (00:00 ~ 23:59:59)
     */
    @GetMapping("/yesterday")
    public Map<String, Object> getYesterdayData(@RequestParam String stockCode) {
        List<GlobalDailyStockDocument> stockList = globalStockService.getYesterdayData(stockCode);
        return globalStockService.buildChartDataForTodayOrYesterday(stockList);
    }

    /**
     * 1주일 히스토리
     */
    @GetMapping("/oneweek")
    public Map<String, Object> getOneWeekData(@RequestParam String stockCode) {
        List<GlobalHistoryStockDocument> historyList = globalStockService.getOneWeekData(stockCode);
        return globalStockService.buildChartDataForHistory(historyList);
    }

    /**
     * 1개월 히스토리
     */
    @GetMapping("/onemonth")
    public Map<String, Object> getOneMonthData(@RequestParam String stockCode) {
        List<GlobalHistoryStockDocument> historyList = globalStockService.getOneMonthData(stockCode);
        return globalStockService.buildChartDataForHistory(historyList);
    }

    /**
     * 3개월 히스토리
     */
    @GetMapping("/threemonth")
    public Map<String, Object> getThreeMonthData(@RequestParam String stockCode) {
        List<GlobalHistoryStockDocument> historyList = globalStockService.getThreeMonthData(stockCode);
        return globalStockService.buildChartDataForHistory(historyList);
    }
}
