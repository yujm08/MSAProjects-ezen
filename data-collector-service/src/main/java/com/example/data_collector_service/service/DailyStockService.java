package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.GlobalDailyStock;
import com.example.data_collector_service.entity.KoreanDailyStock;
import com.example.data_collector_service.entity.KreanStockMaster;
import com.example.data_collector_service.repository.GlobalDailyStockRepository;
import com.example.data_collector_service.repository.KoreanDailyStockRepository;
import com.example.data_collector_service.repository.KreanStockMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * DailyStockService
 * 
 * 실시간으로 수신된 국내/해외 주식 데이터를 Daily 테이블에 저장합니다.
 * 저장 전에, 국내 주식의 경우 KreanStockMasterRepository를 통해 종목명을 조회하여 newData에 설정합니다.
 * 동일 종목의 당일 최신 데이터와 비교하여 가격 변동이 있을 때만 저장하도록 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyStockService {

    private final KoreanDailyStockRepository koreanRepo;
    private final GlobalDailyStockRepository globalRepo;
    private final KreanStockMasterRepository kreanStockMasterRepo; 
    // SectorMasterRepository -> KreanStockMasterRepository 로 변경

    /**
     * 국내 주식 데이터 저장
     * newData 수신된 국내 주식 데이터
     */
    public void saveKoreanDailyStock(KoreanDailyStock newData) {
        try {
            // 1. 종목 코드로 KreanStockMaster에서 종목명을 조회
            // (예: "005930" -> "삼성전자")
            Optional<KreanStockMaster> masterOpt = kreanStockMasterRepo.findById(newData.getStockCode());
            if (masterOpt.isPresent()) {
                KreanStockMaster master = masterOpt.get();
                // newData 엔티티의 stockName 필드를 마스터에서 가져온 이름으로 설정
                newData.setStockName(master.getStockName());
                log.info("종목 마스터 조회 성공: {} - {}", newData.getStockCode(), master.getStockName());
            } else {
                log.warn("종목 마스터 조회 실패 - 종목코드: {}. 기본값 유지합니다.", newData.getStockCode());
            }
        } catch (Exception e) {
            log.error("종목 마스터 조회 도중 예외 발생 - 종목코드: {}. 기본값 유지합니다.", newData.getStockCode(), e);
        }
    
        // 2. 현재 시간 기준 오늘 날짜 추출
        LocalDate today = newData.getTimestamp().toLocalDate();
        log.info("오늘 날짜: {}", today);
    
        // 3. 동일 종목의 당일 최신 데이터 조회
        KoreanDailyStock lastRecord = koreanRepo.findTopByStockCodeOrderByTimestampDesc(newData.getStockCode());
    
        if (lastRecord == null) {
            log.info("해당 종목의 오늘 기록이 없습니다. (종목코드: {})", newData.getStockCode());
        } else {
            log.info("마지막 기록 - 시각: {}, 현재 가격: {} (종목코드: {})",
                    lastRecord.getTimestamp(), lastRecord.getCurrentPrice(), newData.getStockCode());
        }
    
        // 4. 기록이 없거나 오늘 데이터가 없거나, 가격이 변동된 경우에만 저장
        if (lastRecord == null
            || !lastRecord.getTimestamp().toLocalDate().equals(today)
            || lastRecord.getCurrentPrice().compareTo(newData.getCurrentPrice()) != 0) {
            try {
                koreanRepo.save(newData);
                log.info("새 데이터 저장 완료 - 종목코드: {}, 시각: {}", newData.getStockCode(), newData.getTimestamp());
            } catch (Exception e) {
                log.error("데이터 저장 실패 - 종목코드: {}", newData.getStockCode(), e);
            }
        } else {
            log.info("가격 변동 없음 - 저장하지 않음 (종목코드: {})", newData.getStockCode());
        }
    }    

    /**
     * 해외 주식 데이터 저장
     * newData 수신된 해외 주식 데이터
     */
    public void saveGlobalDailyStock(GlobalDailyStock newData) {
        // 해외 주식은 현재 별도 마스터가 없으므로, stockName은 WebSocket에서 "미매핑" 등으로 설정 가능
        // 또는 종목명을 저장하지 않고, stockName 필드 자체를 없앨 수도 있음. -> 없앰
    
        try {
            LocalDate today = newData.getTimestamp().toLocalDate();
            log.info("saveGlobalDailyStock 시작 - 종목코드: {} / 오늘 날짜: {}", newData.getStockCode(), today);
    
            GlobalDailyStock lastRecord = globalRepo.findTopByStockCodeOrderByTimestampDesc(newData.getStockCode());
            if (lastRecord == null) {
                log.info("해당 해외 종목의 오늘 기록이 없습니다. (종목코드: {})", newData.getStockCode());
            } else {
                log.info("마지막 해외 기록 - 시각: {}, 현재 가격: {} (종목코드: {})",
                    lastRecord.getTimestamp(), lastRecord.getCurrentPrice(), newData.getStockCode());
            }
    
            if (lastRecord == null
                || !lastRecord.getTimestamp().toLocalDate().equals(today)
                || lastRecord.getCurrentPrice().compareTo(newData.getCurrentPrice()) != 0) {
                try {
                    globalRepo.save(newData);
                    log.info("새 해외 데이터 저장 완료 - 종목코드: {}, 시각: {}", newData.getStockCode(), newData.getTimestamp());
                } catch (Exception e) {
                    log.error("해외 데이터 저장 실패 - 종목코드: {}", newData.getStockCode(), e);
                }
            } else {
                log.info("해외 종목 가격 변동 없음 - 저장하지 않음 (종목코드: {})", newData.getStockCode());
            }
        } catch (Exception e) {
            log.error("saveGlobalDailyStock 처리 도중 예외 발생 - 종목코드: {}", newData.getStockCode(), e);
        }
    }
    
}
