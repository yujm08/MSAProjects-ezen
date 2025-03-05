package com.example.data_collector_service.service;

import com.example.data_collector_service.repository.KreanStockMasterRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCollectorService {

    private final KoreanStockWebSocketService koreanStockWebSocketService;
    private final KreanStockMasterRepository kreanStockMasterRepository;
    private final GlobalStockApiService globalStockApiService;

    @PostConstruct  //Spring이 의존성 주입(DI, Dependency Injection)을 완료한 후 자동으로 실행되는 초기화 메서드
    public void init() {
        subscribeAllKoreanStocks();
    }

    /**
     * 국내 종목 WebSocket 구독 시작
     * - 예: 애플리케이션 시작 시점에 전체 종목에 대해 subscribe
     */
    public void subscribeAllKoreanStocks() {
        kreanStockMasterRepository.findAll().forEach(master -> {
            String code = master.getStockCode();
            log.info("[StockCollectorService] 국내 종목 WebSocket 구독: {}", code);
            koreanStockWebSocketService.subscribeKoreanStock(code);
        });
    }

    /**
     * 해외 종목 REST API 조회
     * - 원하는 시점(스케줄러 or 수동 호출)에서 실행
     */
    public void fetchAllOverseasStocks() {
        log.info("[StockCollectorService] 해외 종목 REST API 호출 시작");
        globalStockApiService.fetchAllForeignStocks();
        log.info("[StockCollectorService] 해외 종목 REST API 호출 완료");
    }
}
