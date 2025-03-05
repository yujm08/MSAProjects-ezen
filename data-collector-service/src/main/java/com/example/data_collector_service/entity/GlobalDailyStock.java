package com.example.data_collector_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "global_daily_stock", indexes = {
    @Index(name = "idx_global_daily_stock", columnList = "stock_code, timestamp")
})
public class GlobalDailyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 종목 코드
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;

    // 종목 이름
    @Column(name = "stock_name", length = 50, nullable = false)
    private String stockName;

    // 거래소 코드
    @Column(name = "exchange_code", length = 10)
    private String exchangeCode;

    // 현재 가격
    @Column(name = "current_price", precision = 10, scale = 4, nullable = false)
    private BigDecimal currentPrice;

    // 변동률(%) – 계산식: ((c - o) / o) * 100, DECIMAL(5,2)
    @Column(name = "change_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal changeRate;

    // 데이터 수집 시각 (Data Body 키: t; Unix Timestamp → LocalDateTime 변환 필요)
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
