package com.example.data_collector_service.buffer;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.data_collector_service.entity.GlobalDailyStock;
import com.example.data_collector_service.entity.KoreanDailyStock;

// WebSocket으로 수신한 최신 데이터를 주식 종목별로 임시 저장하는 버퍼 클래스.
@Component
public class RealTimeDataBuffer {
    
    /**
     * 국내 주식 데이터를 저장하는 버퍼
     * Key: 주식 종목 코드 (stockCode)
     * Value: 해당 종목의 최신 주식 데이터 (KoreanDailyStock)
     */

    // 국내 주식: stockCode -> 최신 데이터
    private final ConcurrentHashMap<String, KoreanDailyStock> koreanBuffer = new ConcurrentHashMap<>();
    // 해외 주식: stockCode -> 최신 데이터
    private final ConcurrentHashMap<String, GlobalDailyStock> globalBuffer = new ConcurrentHashMap<>();

    public void putKoreanData(String stockCode, KoreanDailyStock data) {
        koreanBuffer.put(stockCode, data);
    }

    public ConcurrentHashMap<String, KoreanDailyStock> getKoreanBuffer() {
        return koreanBuffer;
    }

    public void putGlobalData(String stockCode, GlobalDailyStock data) {
        globalBuffer.put(stockCode, data);
    }

    public ConcurrentHashMap<String, GlobalDailyStock> getGlobalBuffer() {
        return globalBuffer;
    }

}
