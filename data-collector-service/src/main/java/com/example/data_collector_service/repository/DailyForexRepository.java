package com.example.data_collector_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.data_collector_service.entity.DailyForex;

public interface DailyForexRepository extends JpaRepository<DailyForex, Long>{
    
}
