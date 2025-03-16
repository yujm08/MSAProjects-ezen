package com.example.data_visualization_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.data_visualization_service.document.GlobalStockDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GlobalStockDocumentRepository extends ElasticsearchRepository<GlobalStockDocument, String> {
    List<GlobalStockDocument> findByStockCodeAndTimestampBetween(String stockCode, LocalDateTime start, LocalDateTime end);
    List<GlobalStockDocument> findByExchangeCode(String exchangeCode);
    Optional<GlobalStockDocument> findTopByStockCodeOrderByTimestampDesc(String stockCode);
}
