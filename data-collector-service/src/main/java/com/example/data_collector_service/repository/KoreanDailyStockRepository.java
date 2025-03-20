package com.example.data_collector_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.data_collector_service.entity.KoreanDailyStock;

public interface KoreanDailyStockRepository extends JpaRepository<KoreanDailyStock, Long>{
    
    // 특정 종목의 가장 최근 레코드를 가져옴
    KoreanDailyStock findTopByStockCodeOrderByTimestampDesc(String stockCode);

    // 지정 기간(`start` ~ `end`) 동안 존재하는 모든 종목 코드(Stock Code)를 중복 없이 조회
    @Query("SELECT DISTINCT k.stockCode FROM KoreanDailyStock k WHERE k.timestamp BETWEEN :start AND :end")
    List<String> findDistinctStockCodesByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // 지정 기간 내의 해당 종목 데이터를 삭제
    void deleteByStockCodeAndTimestampBetween(String stockCode, LocalDateTime start, LocalDateTime end);

    // 특정 날짜 범위에 해당하는 특정 주식 레코드를 조회
    List<KoreanDailyStock> findByStockCodeAndTimestampBetween(String stockCode, LocalDateTime start, LocalDateTime end);

    Optional<KoreanDailyStock> findTopByStockCodeOrderByTimestamp(String stockCode);
}
