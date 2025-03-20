package com.example.data_collector_service.visualizationService;

import com.example.data_collector_service.entity.DailyForex;
import com.example.data_collector_service.entity.HistoryForex;
import com.example.data_collector_service.exception.DataNotFoundException;
import com.example.data_collector_service.repository.DailyForexRepository;
import com.example.data_collector_service.repository.HistoryForexRepository;
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

    private final DailyForexRepository dailyForexRepository;
    private final HistoryForexRepository historyForexRepository;

    /**
     * 오늘 00:00부터 현재 시각까지의 실시간 환율 데이터를 조회합니다.
     */
    public List<DailyForex> getTodayData(String currencyCode) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay(); // 오늘 00:00
        LocalDateTime now = LocalDateTime.now();
        List<DailyForex> result = dailyForexRepository.findByCurrencyCodeAndTimestampBetween(currencyCode, startOfToday, now);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("오늘의 환율 데이터를 찾을 수 없습니다. 통화 코드: " + currencyCode);
        }
        return result;
    }

    /**
     * 어제 00:00부터 어제 23:59:59까지의 실시간 환율 데이터를 조회합니다.
     */
    public List<DailyForex> getYesterdayData(String currencyCode) {
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay(); // 어제 00:00
        LocalDateTime endOfYesterday = LocalDate.now().minusDays(1).atTime(LocalTime.MAX); // 어제 23:59:59
        List<DailyForex> result = dailyForexRepository.findByCurrencyCodeAndTimestampBetween(currencyCode, startOfYesterday, endOfYesterday);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("어제의 환율 데이터를 찾을 수 없습니다. 통화 코드: " + currencyCode);
        }
        return result;
    }

    /**
     * 최근 1주일간의 환율 히스토리 데이터를 조회합니다.
     */
    public List<HistoryForex> getOneWeekData(String currencyCode) {
        LocalDate startDate = LocalDate.now().minusWeeks(1);
        LocalDate endDate = LocalDate.now();
        List<HistoryForex> result = historyForexRepository.findByCurrencyCodeAndDateBetween(currencyCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 1주일간의 환율 히스토리 데이터를 찾을 수 없습니다. 통화 코드: " + currencyCode);
        }
        return result;
    }

    /**
     * 최근 1개월(30일)간의 환율 히스토리 데이터를 조회합니다.
     */
    public List<HistoryForex> getOneMonthData(String currencyCode) {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<HistoryForex> result = historyForexRepository.findByCurrencyCodeAndDateBetween(currencyCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 1개월간의 환율 히스토리 데이터를 찾을 수 없습니다. 통화 코드: " + currencyCode);
        }
        return result;
    }

    /**
     * 최근 3개월(90일)간의 환율 히스토리 데이터를 조회합니다.
     */
    public List<HistoryForex> getThreeMonthData(String currencyCode) {
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now();
        List<HistoryForex> result = historyForexRepository.findByCurrencyCodeAndDateBetween(currencyCode, startDate, endDate);
        if(result == null || result.isEmpty()){
            throw new DataNotFoundException("최근 3개월간의 환율 히스토리 데이터를 찾을 수 없습니다. 통화 코드: " + currencyCode);
        }
        return result;
    }

    /**
     * 통화 코드를 입력받아 가장 최근 데이터(매매기준율, 변동률, 시간)를 반환합니다.
     */
    public DailyForex getLatestForexData(String currencyCode) {
        return dailyForexRepository.findTopByCurrencyCodeOrderByTimestamp(currencyCode)
                .orElseThrow(() -> new DataNotFoundException("최신 환율 데이터를 찾을 수 없습니다. 통화 코드: " + currencyCode));
    }    

    /**
     * 통화 코드를 입력받아 최신 데이터를 기준으로 통화 이름과 변동률 정보를 반환합니다.
     */
    public Map<String, Object> getForexSummary(String currencyCode) {
        DailyForex latestData = getLatestForexData(currencyCode);
        Map<String, Object> summary = new HashMap<>();
        summary.put("currencyCode", latestData.getCurrencyCode());
        summary.put("currencyName", latestData.getCurrencyName());
        summary.put("exchangeRate", latestData.getExchangeRate());
        summary.put("changeRate", latestData.getChangeRate());
        summary.put("timestamp", latestData.getTimestamp());
        return summary;
    }

    /**
     * 실시간 환율 데이터(오늘/어제)를 Chart.js에서 사용 가능한 형태로 변환합니다.
     * 
     * Chart.js 데이터 구조:
     * {
     *   "labels": ["2025-03-11 10:00", "2025-03-11 10:05", "2025-03-11 10:10"],
     *   "datasets": [
     *     { "label": "Exchange Rate", "data": [1320.5, 1321.0, 1319.8] }
     *   ]
     * }
     */
    public Map<String, Object> buildChartDataForDaily(List<DailyForex> forexList) {
        if(forexList == null || forexList.isEmpty()){
            throw new DataNotFoundException("차트 생성을 위한 환율 데이터가 없습니다.");
        }
        
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 시간 오름차순 정렬
        forexList.sort(Comparator.comparing(DailyForex::getTimestamp));

        for (DailyForex forex : forexList) {
            labels.add(forex.getTimestamp().toString());
            data.add(forex.getExchangeRate());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "환율");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }

    /**
     * 히스토리 환율 데이터(1주/1달/3달)를 Chart.js에서 사용 가능한 형태로 변환합니다.
     */
    public Map<String, Object> buildChartDataForHistory(List<HistoryForex> historyList) {
        if(historyList == null || historyList.isEmpty()){
            throw new DataNotFoundException("차트 생성을 위한 환율 히스토리 데이터가 없습니다.");
        }
        
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 날짜 오름차순 정렬
        historyList.sort(Comparator.comparing(HistoryForex::getDate));

        for (HistoryForex history : historyList) {
            labels.add(history.getDate().toString());
            data.add(history.getClosingRate());
        }

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "종가 환율");
        dataset.put("data", data);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));

        return chartData;
    }
}
