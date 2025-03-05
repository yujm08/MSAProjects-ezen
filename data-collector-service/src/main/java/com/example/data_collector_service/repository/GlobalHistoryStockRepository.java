package com.example.data_collector_service.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.data_collector_service.entity.GlobalHistoryStock;

public interface GlobalHistoryStockRepository extends JpaRepository<GlobalHistoryStock, Long>{
    
    // 체크: 해당 종목에 대해 해당 날짜(하루) 데이터가 이미 있는지
    boolean existsByStockCodeAndTimestamp(String stockCode, LocalDate timestamp);

    // 삭제: 지정 날짜보다 이전의 레코드 삭제 (3달 이상)
    void deleteByTimestampBefore(LocalDate cutoffDate);
}
