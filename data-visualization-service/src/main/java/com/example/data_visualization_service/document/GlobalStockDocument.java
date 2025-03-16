package com.example.data_visualization_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "global_stock")
public class GlobalStockDocument {
    @Id
    private String id;
    private String stockCode;
    private String stockName;
    private String exchangeCode;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
    private LocalDateTime timestamp;
}
