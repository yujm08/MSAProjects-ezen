package com.example.data_collector_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.data_collector_service.entity.GlobalDailyStock;

public interface GlobalDailyStockRepository extends JpaRepository<GlobalDailyStock, Long>{
    
}
