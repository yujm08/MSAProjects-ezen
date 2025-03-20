package com.example.data_collector_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.data_collector_service.entity.HistoryForex;

public interface HistoryForexRepository extends JpaRepository<HistoryForex, Long>{
    
    // 특정 통화, 특정 날짜의 기록이 있는지 확인하기 위한 메서드
    List<HistoryForex> findByCurrencyCodeAndDate(String currencyCode, LocalDate date);

    // cutoffDate 이전의 기록들을 삭제하는 메서드 (삭제된 레코드 수 반환)
    int deleteByDateBefore(LocalDate cutoffDate);


    // 환율 데이터를 특정 기간(날짜 범위)에 대해 조회
    List<HistoryForex> findByCurrencyCodeAndDateBetween(String currencyCode, LocalDate startDate, LocalDate endDate);
}
