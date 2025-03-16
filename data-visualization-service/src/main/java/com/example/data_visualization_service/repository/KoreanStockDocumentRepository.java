package com.example.data_visualization_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.data_visualization_service.document.KoreanStockDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KoreanStockDocumentRepository extends ElasticsearchRepository<KoreanStockDocument, String> {
    List<KoreanStockDocument> findByStockCodeAndTimestampBetween(String stockCode, LocalDateTime start, LocalDateTime end);
    Optional<KoreanStockDocument> findTopByStockCodeOrderByTimestampDesc(String stockCode);
}
