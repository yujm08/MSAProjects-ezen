package com.example.data_visualization_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.data_visualization_service.document.GlobalHistoryStockDocument;

import java.time.LocalDate;
import java.util.List;

public interface GlobalHistoryStockDocumentRepository extends ElasticsearchRepository<GlobalHistoryStockDocument, String> {
    List<GlobalHistoryStockDocument> findByStockCodeAndTimestampBetween(String stockCode, LocalDate start, LocalDate end);
}
