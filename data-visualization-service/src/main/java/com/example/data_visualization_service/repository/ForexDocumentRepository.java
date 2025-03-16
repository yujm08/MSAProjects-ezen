package com.example.data_visualization_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.data_visualization_service.document.ForexDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ForexDocumentRepository extends ElasticsearchRepository<ForexDocument, String> {
    List<ForexDocument> findByCurrencyCodeAndTimestampBetween(String currencyCode, LocalDateTime start, LocalDateTime end);
    Optional<ForexDocument> findTopByCurrencyCodeOrderByTimestampDesc(String currencyCode);
}
