package com.example.data_collector_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.data_collector_service.entity.DailyForex;

public interface DailyForexRepository extends JpaRepository<DailyForex, Long>{
    
    //특정 통화에 대해 가장 최근(timestamp가 가장 큰) 레코드를 조회
    DailyForex findTopByCurrencyCodeOrderByTimestampDesc(String currencyCode);

    Optional<DailyForex> findTopByCurrencyCodeOrderByTimestamp(String currencyCode);

    //특정 날짜 범위에 해당하는 레코드를 조회
    @Query("SELECT d FROM DailyForex d " +
           "WHERE d.currencyCode = :currencyCode " +
           "AND d.timestamp >= :startOfDay AND d.timestamp <= :endOfDay " +
           "ORDER BY d.timestamp DESC")
    List<DailyForex> findAllForDateAndCurrency(@Param("currencyCode") String currencyCode,
                                               @Param("startOfDay") LocalDateTime startOfDay,
                                               @Param("endOfDay") LocalDateTime endOfDay);

        // 실시간 환율 데이터를 특정 날짜 범위로 조회
    @Query("SELECT d FROM DailyForex d " +
    "WHERE d.currencyCode = :currencyCode " +
    "AND d.timestamp >= :startOfDay AND d.timestamp <= :endOfDay " +
    "ORDER BY d.timestamp DESC")
List<DailyForex> findByCurrencyCodeAndTimestampBetween(@Param("currencyCode") String currencyCode,
                                                    @Param("startOfDay") LocalDateTime startOfDay,
                                                    @Param("endOfDay") LocalDateTime endOfDay);
}
