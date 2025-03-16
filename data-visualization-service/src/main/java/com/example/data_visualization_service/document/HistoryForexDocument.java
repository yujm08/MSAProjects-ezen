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
@Document(indexName = "history_forex")
public class HistoryForexDocument {
    @Id
    private String id;
    private String currencyCode;
    private String currencyName;
    private BigDecimal closingRate;
    private LocalDate date;
}
