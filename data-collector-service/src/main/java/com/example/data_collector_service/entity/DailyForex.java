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
@Table(name = "daily_forex", indexes = {
    @Index(name = "idx_daily_forex", columnList = "currency_code, timestamp")
})
public class DailyForex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 통화 코드 (Data Body 키: CUR_CD)
    @Column(name = "currency_code", length = 10, nullable = false)
    private String currencyCode;

    // 통화 이름 (Data Body 키: CUR_NM)
    @Column(name = "currency_name", length = 50, nullable = false)
    private String currencyName;

    // 매매기준율 (Data Body 키: DEAL_BASC_RT) DECIMAL(10,4)
    @Column(name = "exchange_rate", precision = 10, scale = 4, nullable = false)
    private BigDecimal exchangeRate;

    // 변동률(%) – 계산 값, DECIMAL(5,2)
    @Column(name = "change_rate", precision = 5, scale = 2)
    private BigDecimal changeRate;

    // 고시 시각 (Data Body 키: PBLD_TM)
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
