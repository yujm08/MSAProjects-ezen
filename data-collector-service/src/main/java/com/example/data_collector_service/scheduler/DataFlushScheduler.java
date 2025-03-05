package com.example.data_collector_service.scheduler;

import com.example.data_collector_service.buffer.RealTimeDataBuffer;
import com.example.data_collector_service.entity.KoreanDailyStock;
import com.example.data_collector_service.service.DailyStockService;
import com.example.data_collector_service.service.StockCollectorService;
import com.example.data_collector_service.util.MarketTimeChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;

/**
 * DataFlushScheduler
 * 
 * 1. 매 20초마다 버퍼에 있는 데이터를 확인하여 DB에 저장하는 역할을 합니다.
 * 2. 정규장 시간에만 데이터를 저장하도록 `MarketTimeChecker`를 통해 시장 개장 여부를 확인합니다.
 * 3. 저장된 후에는 버퍼에서 제거하여 중복 저장을 방지합니다.
 */
@Slf4j  // 로깅을 위한 Lombok 어노테이션
@Component  // Spring이 자동으로 관리하는 컴포넌트 지정
@RequiredArgsConstructor  // 생성자 주입을 위한 Lombok 어노테이션
public class DataFlushScheduler {

    private final RealTimeDataBuffer dataBuffer;  // 실시간 데이터를 저장하는 버퍼
    private final DailyStockService dailyStockService;  // DB 저장을 담당하는 서비스
    private final StockCollectorService stockCollectorService;

    //🇰🇷 국내 주식 WebSocket 데이터 저장 스케줄러
    @Scheduled(cron = "*/20 * * * * *", zone = "Asia/Seoul")  // 매 20초마다 실행
    public void flushKoreanData() {
        if (!MarketTimeChecker.isKoreanMarketOpen()) {
            log.info("국내 정규장이 아니므로 국내 데이터 flush를 건너뜁니다.");
            return;
        }

        // 버퍼에 저장된 국내 주식 데이터를 반복문을 통해 조회
        Iterator<Map.Entry<String, KoreanDailyStock>> iter = dataBuffer.getKoreanBuffer().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, KoreanDailyStock> entry = iter.next();
            log.info("국내 정규장이므로 데이터를 저장합니다");
            dailyStockService.saveKoreanDailyStock(entry.getValue());  // 데이터 저장
            log.info("데이터가 저장되었습니다 -DataFlushScheduler");
            iter.remove();  // 저장된 데이터는 버퍼에서 제거
        }
    }

    //해외 주식 REST API 데이터 저장 스케줄러
    @Scheduled(cron = "*/20 * * * * *", zone = "Asia/Seoul")  // 매 20초마다 실행
    public void fetchGlobalData() {
        if (MarketTimeChecker.isGlobalMarketOpen()) {
            log.info("[OverseasStockScheduler] 글로벌 시장 정규장, REST API 호출 시작");
            stockCollectorService.fetchAllOverseasStocks();
            log.info("[OverseasStockScheduler] REST API 호출 완료");
        } else {
            log.info("[OverseasStockScheduler] 글로벌 시장 정규장이 아니므로 호출 생략");
        }
    }
}
