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
@Document(indexName = "forex")
public class ForexDocument {
    @Id
    private String id;
    private String currencyCode;
    private String currencyName;
    private BigDecimal exchangeRate;
    private BigDecimal changeRate;
    private LocalDateTime timestamp;  // ISO 8601 (초 단위)
}

