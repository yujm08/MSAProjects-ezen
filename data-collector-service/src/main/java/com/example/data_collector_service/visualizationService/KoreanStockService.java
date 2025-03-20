package com.example.data_collector_service.visualizationService;

import com.example.data_collector_service.entity.KoreanDailyStock;
import com.example.data_collector_service.entity.KoreanHistoryStock;
import com.example.data_collector_service.exception.DataNotFoundException;
import com.example.data_collector_service.repository.KoreanDailyStockRepository;
import com.example.data_collector_service.repository.KoreanHistoryStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KoreanStockService {

    private final KoreanDailyStockRepository koreanDailyStockRepository;
    private final KoreanHistoryStockRepository koreanHistoryStockRepository;

    /**
     * 오늘 00:00부터 현재까지 국내 주식 실시간 데이터 조회
     */
    public List<KoreanDailyStock> getTodayData(String stockCode) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        List<KoreanDailyStock> result = koreanDailyStockRepository.findByStockCodeAndTimestampBetween(stockCode, startOfToday, now);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("오늘의 국내 주식 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 어제 00:00 ~ 23:59:59 국내 주식 실시간 데이터 조회
     */
    public List<KoreanDailyStock> getYesterdayData(String stockCode) {
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime endOfYesterday = startOfYesterday.plusDays(1).minusNanos(1);
        List<KoreanDailyStock> result = koreanDailyStockRepository.findByStockCodeAndTimestampBetween(stockCode, startOfYesterday, endOfYesterday);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("어제의 국내 주식 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 1주일간 국내 주식 히스토리 데이터
     */
    public List<KoreanHistoryStock> getOneWeekData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusWeeks(1);
        LocalDate endDate = LocalDate.now();
        List<KoreanHistoryStock> result = koreanHistoryStockRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 1주일간의 국내 주식 히스토리 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 1개월간 국내 주식 히스토리 데이터
     */
    public List<KoreanHistoryStock> getOneMonthData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<KoreanHistoryStock> result = koreanHistoryStockRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 1개월간의 국내 주식 히스토리 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 3개월간 국내 주식 히스토리 데이터
     */
    public List<KoreanHistoryStock> getThreeMonthData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now();
        List<KoreanHistoryStock> result = koreanHistoryStockRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 3개월간의 국내 주식 히스토리 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 종목 코드를 입력받아 가장 최근 데이터(종가, 변동률, 시간)를 반환
     */
    public KoreanDailyStock getLatestKoreanStockData(String stockCode) {
        return koreanDailyStockRepository.findTopByStockCodeOrderByTimestamp(stockCode)
        .orElseThrow(() -> new DataNotFoundException("최신 국내 주식 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode));
    }

    /**
     * 종목 코드를 입력받아 최신 데이터를 기준으로 종목 이름과 변동률 정보를 반환합니다.
     */
    public Map<String, Object> getKoreanStockSummary(String stockCode) {
        KoreanDailyStock latestData = getLatestKoreanStockData(stockCode);
        Map<String, Object> summary = new HashMap<>();
        summary.put("stockCode", latestData.getStockCode());
        summary.put("stockName", latestData.getStockName());
        summary.put("currentPrice", latestData.getCurrentPrice());
        summary.put("changeRate", latestData.getChangeRate());
        summary.put("timestamp", latestData.getTimestamp());
        return summary;
    }

    /**
     * 실시간 국내 주식 데이터(오늘/어제)를 Chart.js에서 사용 가능한 형태로 변환합니다.
     * 
     * Chart.js 데이터 구조:
     * {
     *   "labels": ["2025-03-11 10:00", "2025-03-11 10:05", "2025-03-11 10:10"],
     *   "datasets": [
     *     { "label": "Stock Price", "data": [1320.5, 1321.0, 1319.8] }
     *   ]
     * }
     */
    public Map<String, Object> buildChartDataForTodayOrYesterday(List<KoreanDailyStock> stockList) {
        if(stockList == null || stockList.isEmpty()){
            throw new DataNotFoundException("차트 생성을 위한 국내 주식 데이터가 없습니다.");
        }
        
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 시간 오름차순 정렬
        stockList.sort(Comparator.comparing(KoreanDailyStock::getTimestamp));

        for (KoreanDailyStock stock : stockList) {
            labels.add(stock.getTimestamp().toString());
            data.add(stock.getCurrentPrice());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "주식 가격");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }

    /**
     * 히스토리 주식 데이터(1주/1달/3달)를 Chart.js에서 사용 가능한 형태로 변환합니다.
     */
    public Map<String, Object> buildChartDataForHistory(List<KoreanHistoryStock> historyList) {
        if(historyList == null || historyList.isEmpty()){
            throw new DataNotFoundException("차트 생성을 위한 국내 주식 히스토리 데이터가 없습니다.");
        }
        
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 날짜 오름차순 정렬
        historyList.sort(Comparator.comparing(KoreanHistoryStock::getDate));

        for (KoreanHistoryStock history : historyList) {
            labels.add(history.getDate().toString());
            data.add(history.getClosingPrice());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "종가 주식 가격");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }
}

