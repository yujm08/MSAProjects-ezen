package com.example.data_collector_service.scheduler;

import com.example.data_collector_service.service.ForexWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 📌 **ForexScheduledSaver**
 * - **4분마다 WebSocket에서 가져온 최신 환율을 DB에 저장**
 * - WebSocket을 통해 실시간으로 받은 데이터를 일정 주기로 저장하여 분석 및 조회 가능
 */
@Slf4j
@Component
public class ForexScheduledSaver {

    private final ForexWebSocketService forexWebSocketService;

    /**
     * ForexScheduledSaver 생성자 (ForexWebSocketService 주입)
     * @param forexWebSocketService WebSocket으로부터 최신 환율을 가져오는 서비스
     */
    public ForexScheduledSaver(ForexWebSocketService forexWebSocketService) {
        this.forexWebSocketService = forexWebSocketService;
    }

    /**
     * 🌍 **4분 간격으로 WebSocket에서 가져온 최신 환율을 DB에 저장**
     * - `cron = "0 0/4 * * * *"` → 매 4분마다 실행 (정각 기준 4분 단위)
     * - WebSocket을 통해 실시간으로 받은 환율을 DB에 저장 (EUR/USD)
     * - 변동이 있을 때만 저장하여 불필요한 DB 저장 방지
     * - 예외 발생 시 로그 기록
     */
    @Scheduled(cron = "0 0/4 * * * *") // 매 4분마다 실행 (예: 12:00, 12:04, 12:08 ...)
    public void saveUsdKrw() {
        try {
            log.info("🔄 [ForexScheduledSaver] EUR/USD 최신 환율 저장 시도...");
            forexWebSocketService.saveLatestPriceIfChanged();
            log.info("✅ [ForexScheduledSaver] EUR/USD 환율 저장 완료.");
        } catch (Exception e) {
            log.error("❌ [ForexScheduledSaver] EUR/USD 저장 실패 (WebSocket 데이터 처리 오류)", e);
        }
    }
}

