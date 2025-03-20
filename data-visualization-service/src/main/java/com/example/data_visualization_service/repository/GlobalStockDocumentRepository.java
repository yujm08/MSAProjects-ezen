package com.example.data_visualization_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.data_visualization_service.document.GlobalDailyStockDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GlobalStockDocumentRepository extends ElasticsearchRepository<GlobalDailyStockDocument, String> {
    List<GlobalDailyStockDocument> findByStockCodeAndTimestampBetween(String stockCode, LocalDateTime start, LocalDateTime end);
    List<GlobalDailyStockDocument> findByExchangeCode(String exchangeCode);
    Optional<GlobalDailyStockDocument> findTopByStockCodeOrderByTimestampDesc(String stockCode);
}
