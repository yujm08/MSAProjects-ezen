package com.example.data_collector_service.repository;

import com.example.data_collector_service.entity.KreanStockMaster;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * KreanStockMasterRepository
 * 
 * 국내 주식 종목코드-종목명 매핑 테이블(KreanStockMaster)에 접근하기 위한 JPA Repository.
 */
public interface KreanStockMasterRepository extends JpaRepository<KreanStockMaster, String> {
}
