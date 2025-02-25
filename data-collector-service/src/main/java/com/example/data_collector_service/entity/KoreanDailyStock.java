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
@Table(name = "korean_daily_stock", indexes = {
    @Index(name = "idx_korean_daily_stock", columnList = "stock_code, timestamp")
})
public class KoreanDailyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주식 코드 (Data Body 키: MKSC_SHRN_ISCD)
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;

    // 주식 이름 (업종 데이터 참조)
    @Column(name = "stock_name", length = 50, nullable = false)
    private String stockName;

    // 현재 가격 (Data Body 키: STCK_PRPR, DECIMAL(10,4))
    @Column(name = "current_price", precision = 10, scale = 4, nullable = false)
    private BigDecimal currentPrice;

    // 변동률(%) (Data Body 키: PRDY_CTRT, DECIMAL(5,2))
    @Column(name = "change_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal changeRate;

    // 데이터 수집 시각 (Data Body 키: STCK_CNTG_HOUR; 변환 필요)
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
