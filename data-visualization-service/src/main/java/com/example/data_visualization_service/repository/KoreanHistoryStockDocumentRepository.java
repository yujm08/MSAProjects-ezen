package com.example.data_visualization_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.data_visualization_service.document.KoreanHistoryStockDocument;

import java.time.LocalDate;
import java.util.List;

public interface KoreanHistoryStockDocumentRepository extends ElasticsearchRepository<KoreanHistoryStockDocument, String> {
    List<KoreanHistoryStockDocument> findByStockCodeAndTimestampBetween(String stockCode, LocalDate start, LocalDate end);
}
