package com.example.data_collector_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.data_collector_service.entity.KoreanHistoryStock;

public interface KoreanHistoryStockRepository extends JpaRepository<KoreanHistoryStock, Long>{
    
    // 체크: 해당 종목에 대해 해당 날짜(하루) 데이터가 이미 있는지
    boolean existsByStockCodeAndTimestamp(String stockCode, LocalDate timestamp);

    // 삭제: 지정 날짜보다 이전의 레코드 삭제 (3달 이상)
    void deleteByTimestampBefore(LocalDate cutoffDate);

    // 특정 주식에 대해 특정 날짜 범위에 해당하는 히스토리 데이터를 조회
    List<KoreanHistoryStock> findByStockCodeAndTimestampBetween(String stockCode, LocalDate startDate, LocalDate endDate);
}
