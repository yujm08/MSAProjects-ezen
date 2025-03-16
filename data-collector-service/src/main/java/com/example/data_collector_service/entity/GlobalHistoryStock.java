package com.example.data_collector_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "global_history_stock")
public class GlobalHistoryStock {

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

    // 종가
    @Column(name = "closing_price", precision = 10, scale = 4, nullable = false)
    private BigDecimal closingPrice;

    // 변동률 (예: 전일 대비 변동률 %)
    @Column(name = "change_rate", precision = 5, scale = 2)
    private BigDecimal changeRate;

    // 데이터 수집 날짜 (Unix Timestamp → DATE 변환 필요)
    @Column(name = "timestamp", nullable = false)
    private LocalDate timestamp;
}
