package com.example.data_visualization_service.service;

import com.example.data_visualization_service.document.ForexDocument;
import com.example.data_visualization_service.document.HistoryForexDocument;
import com.example.data_visualization_service.dto.ForexSummaryDTO;
import com.example.data_visualization_service.exception.DataNotFoundException;
import com.example.data_visualization_service.repository.ForexDocumentRepository;
import com.example.data_visualization_service.repository.HistoryForexDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ForexService {

    private final ForexDocumentRepository forexRepository;
    private final HistoryForexDocumentRepository historyForexRepository;

    /**
     * 오늘 00:00부터 현재 시각까지의 실시간 환율 데이터를 조회합니다.
     */
    public List<ForexDocument> getTodayData(String currencyCode) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay(); // 오늘 00:00
        LocalDateTime now = LocalDateTime.now();
        List<ForexDocument> result = forexRepository.findByCurrencyCodeAndTimestampBetween(currencyCode, startOfToday, now);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No forex data found for today for currencyCode: " + currencyCode);
        }
        return result;
    }

    /**
     * 어제 00:00부터 어제 23:59:59까지의 실시간 환율 데이터를 조회합니다.
     */
    public List<ForexDocument> getYesterdayData(String currencyCode) {
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay(); // 어제 00:00
        LocalDateTime endOfYesterday = LocalDate.now().minusDays(1).atTime(LocalTime.MAX); // 어제 23:59:59
        List<ForexDocument> result = forexRepository.findByCurrencyCodeAndTimestampBetween(currencyCode, startOfYesterday, endOfYesterday);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No forex data found for yesterday for currencyCode: " + currencyCode);
        }
        return result;
    }

    /**
     * 최근 1주일간의 환율 히스토리 데이터를 조회합니다.
     */
    public List<HistoryForexDocument> getOneWeekData(String currencyCode) {
        LocalDate startDate = LocalDate.now().minusWeeks(1);
        LocalDate endDate = LocalDate.now();
        List<HistoryForexDocument> result = historyForexRepository.findByCurrencyCodeAndDateBetween(currencyCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No forex history data found for one week for currencyCode: " + currencyCode);
        }
        return result;
    }

    /**
     * 최근 1개월(30일)간의 환율 히스토리 데이터를 조회합니다.
     */
    public List<HistoryForexDocument> getOneMonthData(String currencyCode) {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<HistoryForexDocument> result = historyForexRepository.findByCurrencyCodeAndDateBetween(currencyCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No forex history data found for one month for currencyCode: " + currencyCode);
        }
        return result;
    }

    /**
     * 최근 3개월(90일)간의 환율 히스토리 데이터를 조회합니다.
     */
    public List<HistoryForexDocument> getThreeMonthData(String currencyCode) {
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now();
        List<HistoryForexDocument> result = historyForexRepository.findByCurrencyCodeAndDateBetween(currencyCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("No forex history data found for three months for currencyCode: " + currencyCode);
        }
        return result;
    }

    /**
     * 통화 코드를 입력받아 최신 데이터를 기준으로
     * 통화 이름과 변동률 정보를 ForexSummaryDTO로 반환
     */
    public ForexSummaryDTO getForexSummary(String currencyCode) {
        Optional<ForexDocument> opt = forexRepository.findTopByCurrencyCodeOrderByTimestampDesc(currencyCode);
        return opt.map(doc -> new ForexSummaryDTO(doc.getCurrencyCode(), doc.getCurrencyName(), doc.getChangeRate()))
                  .orElseThrow(() -> new DataNotFoundException("No forex summary data found for currencyCode: " + currencyCode));
    }
    
    /**
     * 통화 코드를 입력받아 가장 최근 데이터(매매기준율, 변동률, 시간)를 반환
     */
    public ForexDocument getLatestForexData(String currencyCode) {
        return forexRepository.findTopByCurrencyCodeOrderByTimestampDesc(currencyCode)
                .orElseThrow(() -> new DataNotFoundException("No latest forex data found for currencyCode: " + currencyCode));
    }

    //-------------------------------날짜(labels)와 환율(Exchange Rate 또는 Closing Rate)만 반환
    //-------------------------------Chart.js에서 X축(날짜)과 Y축(환율)만 필요

    /**
     * Chart.js에서 사용하는 { labels, datasets } 구조로 변환합니다.
     * - 실시간 데이터(오늘/어제)는 ForexDocument 기준 (timestamp, exchangeRate)
     * 
     *  * 🔹 Chart.js 데이터 구조:
        * {
        *   "labels": ["2025-03-11 10:00", "2025-03-11 10:05", "2025-03-11 10:10"],
        *   "datasets": [
        *     { "label": "Exchange Rate", "data": [1320.5, 1321.0, 1319.8] }
        *   ]
        * }
     */
    public Map<String, Object> buildChartDataForTodayOrYesterday(List<ForexDocument> forexList) {
        // labels, data를 담을 리스트
        // X축(시간)과 Y축(환율) 데이터를 저장
        if(forexList == null || forexList.isEmpty()){
            throw new DataNotFoundException("No forex data available for chart building.");
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // timestamp 오름차순 정렬
        // Chart.js는 X축(labels)이 "시간 순서"로 정렬되어야 차트를 제대로 표시할 수 있음
        forexList.sort(Comparator.comparing(ForexDocument::getTimestamp));

        for (ForexDocument doc : forexList) {
            // 데이터 변환: ForexDocument → Chart.js 구조
            labels.add(doc.getTimestamp().toString()); // 예: "2025-03-11 10:05:30"
            data.add(doc.getExchangeRate()); // 환율 값 추가
        }

        // Chart.js의 datasets 구조 생성
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Exchange Rate");   // 차트에 표시할 데이터 라벨
        dataset.put("data", data);

        // 최종 Chart.js 데이터 구조
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels); // X축 (시간 데이터)
        chartData.put("datasets", Collections.singletonList(dataset)); // Y축 데이터

        return chartData;
    }

    /*
     * * Chart.js에서 사용하는 { labels, datasets } 구조로 변환합니다.
     * - 히스토리 데이터(1주/1달/3달)는 HistoryForexDocument 기준 (date, closingRate)
     */
    public Map<String, Object> buildChartDataForHistory(List<HistoryForexDocument> historyList) {
        if(historyList == null || historyList.isEmpty()){
            throw new DataNotFoundException("No forex history data available for chart building.");
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // date 오름차순 정렬
        historyList.sort(Comparator.comparing(HistoryForexDocument::getDate));

        for (HistoryForexDocument doc : historyList) {
            labels.add(doc.getDate().toString()); // 예: "2025-03-11"
            data.add(doc.getClosingRate());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Closing Rate");  // 히스토리 차트 라벨
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }
}
