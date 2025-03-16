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

    @PostConstruct  // 애플리케이션 시작 후 국내 주식 구독 자동 호출
    public void init() {
        subscribeAllKoreanStocks();
    }

    /**
     * 국내 종목 WebSocket 구독 시작
     * - 애플리케이션 시작 시 전체 종목에 대해 subscribe
     */
    public void subscribeAllKoreanStocks() {
        kreanStockMasterRepository.findAll().forEach(master -> {
            String code = master.getStockCode();
            log.info("[StockCollectorService] 국내 종목 WebSocket 구독: {}", code);
            koreanStockWebSocketService.subscribeKoreanStock(code);
        });
    }

    /**
     * 해외 글로벌 시장 REST API 조회
     * - 글로벌 시장(미국 등)의 데이터를 가져옵니다.
     */
    public void fetchAllOverseasStocksForGlobal() {
        log.info("[StockCollectorService] 글로벌 시장 해외 종목 REST API 호출 시작");
        globalStockApiService.fetchForeignStocksByMarket("GLOBAL");
        log.info("[StockCollectorService] 글로벌 시장 해외 종목 REST API 호출 완료");
    }

    /**
     * 해외 홍콩 시장 REST API 조회
     * - 홍콩 시장의 데이터를 가져옵니다.
     */
    public void fetchAllOverseasStocksForHongKong() {
        log.info("[StockCollectorService] 홍콩 시장 해외 종목 REST API 호출 시작");
        globalStockApiService.fetchForeignStocksByMarket("HKS");
        log.info("[StockCollectorService] 홍콩 시장 해외 종목 REST API 호출 완료");
    }
}
