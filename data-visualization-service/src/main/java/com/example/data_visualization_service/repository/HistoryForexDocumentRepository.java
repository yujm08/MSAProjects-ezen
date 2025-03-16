package com.example.data_visualization_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.data_visualization_service.document.HistoryForexDocument;

import java.time.LocalDate;
import java.util.List;

public interface HistoryForexDocumentRepository extends ElasticsearchRepository<HistoryForexDocument, String> {
    List<HistoryForexDocument> findByCurrencyCodeAndDateBetween(String currencyCode, LocalDate start, LocalDate end);
}
