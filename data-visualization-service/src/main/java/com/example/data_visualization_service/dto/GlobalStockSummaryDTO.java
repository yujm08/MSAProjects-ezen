package com.example.data_visualization_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class GlobalStockSummaryDTO {
    private String stockCode;
    private String stockName;
    private BigDecimal changeRate;
}
