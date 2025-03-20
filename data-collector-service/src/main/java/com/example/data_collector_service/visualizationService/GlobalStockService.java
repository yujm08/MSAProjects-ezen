package com.example.data_collector_service.visualizationService;

import com.example.data_collector_service.entity.GlobalDailyStock;
import com.example.data_collector_service.entity.GlobalHistoryStock;
import com.example.data_collector_service.exception.DataNotFoundException;
import com.example.data_collector_service.repository.GlobalDailyStockRepository;
import com.example.data_collector_service.repository.GlobalHistoryStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalStockService {

    private final GlobalDailyStockRepository globalDailyStockRepository;
    private final GlobalHistoryStockRepository globalHistoryStockRepository;

    /**
     * 오늘 00:00부터 현재까지 해외 주식 실시간 데이터 조회
     */
    public List<GlobalDailyStock> getTodayData(String stockCode) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        List<GlobalDailyStock> result = globalDailyStockRepository.findByStockCodeAndTimestampBetween(stockCode, startOfToday, now);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("오늘의 해외 주식 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 어제 00:00 ~ 23:59:59 해외 주식 실시간 데이터 조회
     */
    public List<GlobalDailyStock> getYesterdayData(String stockCode) {
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime endOfYesterday = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);
        List<GlobalDailyStock> result = globalDailyStockRepository.findByStockCodeAndTimestampBetween(stockCode, startOfYesterday, endOfYesterday);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("어제의 해외 주식 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 1주일간 해외 주식 히스토리 데이터
     */
    public List<GlobalHistoryStock> getOneWeekData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusWeeks(1);
        LocalDate endDate = LocalDate.now();
        List<GlobalHistoryStock> result = globalHistoryStockRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 1주일간의 해외 주식 히스토리 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 1개월간 해외 주식 히스토리 데이터
     */
    public List<GlobalHistoryStock> getOneMonthData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<GlobalHistoryStock> result = globalHistoryStockRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 1개월간의 해외 주식 히스토리 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 3개월간 해외 주식 히스토리 데이터
     */
    public List<GlobalHistoryStock> getThreeMonthData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now();
        List<GlobalHistoryStock> result = globalHistoryStockRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 3개월간의 해외 주식 히스토리 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode);
        }
        return result;
    }

    /**
     * 거래소 코드를 입력받아 해당 거래소에 속한 종목 코드 리스트를 반환
     */
    public List<String> getStockCodesByExchange(String exchangeCode) {
        List<GlobalDailyStock> docs = globalDailyStockRepository.findByExchangeCode(exchangeCode);
        if(docs == null || docs.isEmpty()){
            throw new DataNotFoundException("해당 거래소에 속한 종목이 없습니다. 거래소 코드: " + exchangeCode);
        }
        return docs.stream()
                .map(GlobalDailyStock::getStockCode)
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 종목 코드를 입력받아 가장 최근 데이터(종가, 변동률, 시간)를 반환
     */
    public GlobalDailyStock getLatestGlobalStockData(String stockCode) {
        return globalDailyStockRepository.findTopByStockCodeOrderByTimestamp(stockCode)
                .orElseThrow(() -> new DataNotFoundException("최신 해외 주식 데이터를 찾을 수 없습니다. 종목 코드: " + stockCode));
    }
    
    /**
     * 종목 코드를 입력받아 최신 데이터를 기준으로 종목 이름과 변동률 정보를 반환
     */
    public Map<String, Object> getGlobalStockSummary(String stockCode) {
        GlobalDailyStock latestData = getLatestGlobalStockData(stockCode);
        Map<String, Object> summary = new HashMap<>();
        summary.put("stockCode", latestData.getStockCode());
        summary.put("stockName", latestData.getStockName());
        summary.put("currentPrice", latestData.getCurrentPrice());
        summary.put("changeRate", latestData.getChangeRate());
        summary.put("timestamp", latestData.getTimestamp());
        return summary;
    }

    /**
     * 실시간(오늘/어제) 데이터를 Chart.js 포맷으로 변환
     */
    public Map<String, Object> buildChartDataForDaily(List<GlobalDailyStock> stockList) {
        if(stockList == null || stockList.isEmpty()){
            throw new DataNotFoundException("차트 생성을 위한 해외 주식 데이터가 없습니다.");
        }
        
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 시간 오름차순 정렬
        stockList.sort(Comparator.comparing(GlobalDailyStock::getTimestamp));

        for (GlobalDailyStock stock : stockList) {
            labels.add(stock.getTimestamp().toString());
            data.add(stock.getCurrentPrice());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "해외 주식 가격");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }

    /**
     * 히스토리(1주/1달/3달) 데이터를 Chart.js 포맷으로 변환
     */
    public Map<String, Object> buildChartDataForHistory(List<GlobalHistoryStock> historyList) {
        if(historyList == null || historyList.isEmpty()){
            throw new DataNotFoundException("차트 생성을 위한 해외 주식 히스토리 데이터가 없습니다.");
        }
        
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 날짜 오름차순 정렬
        historyList.sort(Comparator.comparing(GlobalHistoryStock::getTimestamp));

        for (GlobalHistoryStock history : historyList) {
            labels.add(history.getTimestamp().toString());
            data.add(history.getClosingPrice());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "해외 주식 종가");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }
}
