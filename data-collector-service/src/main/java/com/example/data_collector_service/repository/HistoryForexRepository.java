package com.example.data_collector_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.data_collector_service.entity.HistoryForex;

public interface HistoryForexRepository extends JpaRepository<HistoryForex, Long>{
    
}
