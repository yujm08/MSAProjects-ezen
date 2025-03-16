package com.example.data_visualization_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ForexSummaryDTO {
    private String currencyCode;
    private String currencyName;
    private BigDecimal changeRate;
}
