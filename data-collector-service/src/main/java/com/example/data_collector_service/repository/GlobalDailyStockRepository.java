package com.example.data_collector_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.data_collector_service.entity.GlobalDailyStock;

public interface GlobalDailyStockRepository extends JpaRepository<GlobalDailyStock, Long>{

    // 최신(가장 최근) 주가 데이터를 가져오는 메서드
    GlobalDailyStock findTopByStockCodeOrderByTimestampDesc(String stockCode);

    //지정 기간(`start` ~ `end`) 동안 존재하는 모든 종목 코드(Stock Code)를 중복 없이 조회
    @Query("SELECT DISTINCT g.stockCode FROM GlobalDailyStock g WHERE g.timestamp BETWEEN :start AND :end")
    List<String> findDistinctStockCodesByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // 지정 기간 내의 해당 종목 데이터를 삭제
    void deleteByStockCodeAndTimestampBetween(String stockCode, LocalDateTime start, LocalDateTime end);

    //특정 기간 동안의 모든 주식 데이터를 조회
    List<GlobalDailyStock> findAllByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // 특정 날짜 범위에 해당하는 특정 주식 레코드를 조회
    List<GlobalDailyStock> findByStockCodeAndTimestampBetween(String stockCode, LocalDateTime start, LocalDateTime end);

    // 특정 거래소에 해당하는 데이터 조회
    List<GlobalDailyStock> findByExchangeCode(String exchangeCode);

    Optional<GlobalDailyStock> findTopByStockCodeOrderByTimestamp(String stockCode);
}
