package com.example.data_visualization_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "korean_history_stock")
public class KoreanHistoryStockDocument {
    @Id
    private String id;
    private String stockCode;
    private String stockName;
    private BigDecimal closingPrice;
    private BigDecimal changeRate;
    private LocalDate timestamp;
}
