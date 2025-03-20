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
@Document(indexName = "korean_daily_stock")
public class KoreanDailyStockDocument {
    @Id
    private String id;
    private String stockCode;
    private String stockName;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
    private LocalDateTime timestamp;
}
