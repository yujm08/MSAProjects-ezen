package com.example.data_visualization_service.service;

import com.example.data_visualization_service.document.GlobalDailyStockDocument;
import com.example.data_visualization_service.dto.GlobalStockSummaryDTO;
import com.example.data_visualization_service.document.GlobalHistoryStockDocument;
import com.example.data_visualization_service.exception.DataNotFoundException;
import com.example.data_visualization_service.repository.GlobalStockDocumentRepository;
import com.example.data_visualization_service.repository.GlobalHistoryStockDocumentRepository;
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

    private final GlobalStockDocumentRepository globalStockRepository;
    private final GlobalHistoryStockDocumentRepository globalHistoryRepository;

    /**
     * 오늘 00:00부터 현재까지 해외 주식 실시간 데이터 조회
     */
    public List<GlobalDailyStockDocument> getTodayData(String stockCode) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        List<GlobalDailyStockDocument> result = globalStockRepository.findByStockCodeAndTimestampBetween(stockCode, startOfToday, now);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No global stock data found for today for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 어제 00:00 ~ 23:59:59 해외 주식 실시간 데이터 조회
     */
    public List<GlobalDailyStockDocument> getYesterdayData(String stockCode) {
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime endOfYesterday = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);
        List<GlobalDailyStockDocument> result = globalStockRepository.findByStockCodeAndTimestampBetween(stockCode, startOfYesterday, endOfYesterday);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No global stock data found for yesterday for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 1주일간 해외 주식 히스토리 데이터
     */
    public List<GlobalHistoryStockDocument> getOneWeekData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusWeeks(1);
        LocalDate endDate = LocalDate.now();
        List<GlobalHistoryStockDocument> result = globalHistoryRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No global stock history data found for one week for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 1개월간 해외 주식 히스토리 데이터
     */
    public List<GlobalHistoryStockDocument> getOneMonthData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<GlobalHistoryStockDocument> result = globalHistoryRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No global stock history data found for one month for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 3개월간 해외 주식 히스토리 데이터
     */
    public List<GlobalHistoryStockDocument> getThreeMonthData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now();
        List<GlobalHistoryStockDocument> result = globalHistoryRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No global stock history data found for three months for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 거래소 코드를 입력받아 해당 거래소에 속한 종목 코드 리스트를 반환
     */
    public List<String> getStockCodesByExchange(String exchangeCode) {
        List<GlobalDailyStockDocument> docs = globalStockRepository.findByExchangeCode(exchangeCode);
        if(docs == null || docs.isEmpty()){
            throw new DataNotFoundException("No global stock data found for exchangeCode: " + exchangeCode);
        }
        return docs.stream()
                   .map(GlobalDailyStockDocument::getStockCode)
                   .distinct()
                   .collect(Collectors.toList());
    }
    
    /**
     * 종목(통화) 코드를 입력받아 최신 데이터를 기준으로
     * 종목 이름과 변동률 정보를 GlobalStockSummaryDTO로 반환
     */
    public GlobalStockSummaryDTO getGlobalStockSummary(String stockCode) {
        return globalStockRepository.findTopByStockCodeOrderByTimestampDesc(stockCode)
                  .map(doc -> new GlobalStockSummaryDTO(doc.getStockCode(), doc.getStockName(), doc.getChangeRate()))
                  .orElseThrow(() -> new DataNotFoundException("No global stock summary data found for stockCode: " + stockCode));
    }
    
    /**
     * 종목(통화) 코드를 입력받아 가장 최근 데이터(종가, 변동률, 시간)를 반환
     */
    public GlobalDailyStockDocument getLatestGlobalStockData(String stockCode) {
        return globalStockRepository.findTopByStockCodeOrderByTimestampDesc(stockCode)
                .orElseThrow(() -> new DataNotFoundException("No latest global stock data found for stockCode: " + stockCode));
    }

    /**
     * 실시간(오늘/어제) 데이터를 Chart.js 포맷으로 변환
     */
    public Map<String, Object> buildChartDataForTodayOrYesterday(List<GlobalDailyStockDocument> stockList) {
        if(stockList == null || stockList.isEmpty()){
            throw new DataNotFoundException("No global stock data available for chart building.");
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // timestamp 오름차순
        stockList.sort(Comparator.comparing(GlobalDailyStockDocument::getTimestamp));

        for (GlobalDailyStockDocument doc : stockList) {
            labels.add(doc.getTimestamp().toString());
            data.add(doc.getCurrentPrice());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Global Stock Price");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }

    /**
     * 히스토리(1주/1달/3달) 데이터를 Chart.js 포맷으로 변환
     */
    public Map<String, Object> buildChartDataForHistory(List<GlobalHistoryStockDocument> historyList) {
        if(historyList == null || historyList.isEmpty()){
            throw new DataNotFoundException("No global stock history data available for chart building.");
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 날짜 오름차순
        historyList.sort(Comparator.comparing(GlobalHistoryStockDocument::getTimestamp));

        for (GlobalHistoryStockDocument doc : historyList) {
            labels.add(doc.getTimestamp().toString());
            data.add(doc.getClosingPrice());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Global Stock Closing Price");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }
}
