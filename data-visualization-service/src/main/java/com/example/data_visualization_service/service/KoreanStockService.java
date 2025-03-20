package com.example.data_visualization_service.service;

import com.example.data_visualization_service.dto.KoreanStockSummaryDTO;
import com.example.data_visualization_service.document.KoreanDailyStockDocument;
import com.example.data_visualization_service.document.KoreanHistoryStockDocument;
import com.example.data_visualization_service.exception.DataNotFoundException;
import com.example.data_visualization_service.repository.KoreanStockDocumentRepository;
import com.example.data_visualization_service.repository.KoreanHistoryStockDocumentRepository;
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

    private final KoreanStockDocumentRepository koreanStockRepository;
    private final KoreanHistoryStockDocumentRepository koreanHistoryRepository;

    /**
     * 오늘 00:00부터 현재까지 국내 주식 실시간 데이터 조회
     */
    public List<KoreanDailyStockDocument> getTodayData(String stockCode) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        List<KoreanDailyStockDocument> result = koreanStockRepository.findByStockCodeAndTimestampBetween(stockCode, startOfToday, now);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No Korean stock data found for today for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 어제 00:00 ~ 23:59:59 국내 주식 실시간 데이터 조회
     */
    public List<KoreanDailyStockDocument> getYesterdayData(String stockCode) {
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime endOfYesterday = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);
        List<KoreanDailyStockDocument> result = koreanStockRepository.findByStockCodeAndTimestampBetween(stockCode, startOfYesterday, endOfYesterday);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No Korean stock data found for yesterday for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 1주일간 국내 주식 히스토리 데이터
     */
    public List<KoreanHistoryStockDocument> getOneWeekData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusWeeks(1);
        LocalDate endDate = LocalDate.now();
        List<KoreanHistoryStockDocument> result = koreanHistoryRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No Korean stock history data found for one week for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 1개월간 국내 주식 히스토리 데이터
     */
    public List<KoreanHistoryStockDocument> getOneMonthData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<KoreanHistoryStockDocument> result = koreanHistoryRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No Korean stock history data found for one month for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 최근 3개월간 국내 주식 히스토리 데이터
     */
    public List<KoreanHistoryStockDocument> getThreeMonthData(String stockCode) {
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now();
        List<KoreanHistoryStockDocument> result = koreanHistoryRepository.findByStockCodeAndTimestampBetween(stockCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No Korean stock history data found for three months for stockCode: " + stockCode);
        }
        return result;
    }

    /**
     * 종목 코드를 입력받아 최신 데이터를 기준으로
     * 종목 이름과 변동률 정보를 KoreanStockSummaryDTO로 반환
     */
    public KoreanStockSummaryDTO getKoreanStockSummary(String stockCode) {
        return koreanStockRepository.findTopByStockCodeOrderByTimestampDesc(stockCode)
                  .map(doc -> new KoreanStockSummaryDTO(doc.getStockCode(), doc.getStockName(), doc.getChangeRate()))
                  .orElseThrow(() -> new DataNotFoundException("No Korean stock summary data found for stockCode: " + stockCode));
    }
    
    /**
     * 종목 코드를 입력받아 가장 최근 데이터(종가, 변동률, 시간)를 반환
     */
    public KoreanDailyStockDocument getLatestKoreanStockData(String stockCode) {
        return koreanStockRepository.findTopByStockCodeOrderByTimestampDesc(stockCode)
                  .orElseThrow(() -> new DataNotFoundException("No latest Korean stock data found for stockCode: " + stockCode));
    }

    /**
     * 실시간(오늘/어제) 데이터를 Chart.js 포맷으로 변환
     */
    public Map<String, Object> buildChartDataForTodayOrYesterday(List<KoreanDailyStockDocument> stockList) {
        if(stockList == null || stockList.isEmpty()){
            throw new DataNotFoundException("No Korean stock data available for chart building.");
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // timestamp 오름차순
        stockList.sort(Comparator.comparing(KoreanDailyStockDocument::getTimestamp));

        for (KoreanDailyStockDocument doc : stockList) {
            labels.add(doc.getTimestamp().toString());
            data.add(doc.getCurrentPrice());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Korean Stock Price");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }

    /**
     * 히스토리(1주/1달/3달) 데이터를 Chart.js 포맷으로 변환
     */
    public Map<String, Object> buildChartDataForHistory(List<KoreanHistoryStockDocument> historyList) {
        if(historyList == null || historyList.isEmpty()){
            throw new DataNotFoundException("No Korean stock history data available for chart building.");
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 날짜 오름차순
        historyList.sort(Comparator.comparing(KoreanHistoryStockDocument::getTimestamp));

        for (KoreanHistoryStockDocument doc : historyList) {
            labels.add(doc.getTimestamp().toString());
            data.add(doc.getClosingPrice());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Korean Stock Closing Price");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }
}
