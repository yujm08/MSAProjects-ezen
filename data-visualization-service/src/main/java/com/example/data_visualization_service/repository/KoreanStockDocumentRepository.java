package com.example.data_visualization_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.data_visualization_service.document.KoreanDailyStockDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KoreanStockDocumentRepository extends ElasticsearchRepository<KoreanDailyStockDocument, String> {
    List<KoreanDailyStockDocument> findByStockCodeAndTimestampBetween(String stockCode, LocalDateTime start, LocalDateTime end);
    Optional<KoreanDailyStockDocument> findTopByStockCodeOrderByTimestampDesc(String stockCode);
}
