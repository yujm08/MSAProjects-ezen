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
@Table(name = "korean_history_stock")
public class KoreanHistoryStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주식 코드
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;

    // 주식 이름
    @Column(name = "stock_name", length = 50, nullable = false)
    private String stockName;

    // 종가
    @Column(name = "closing_price", precision = 10, scale = 4, nullable = false)
    private BigDecimal closingPrice;

    // 변동률 (예: 전일 대비 변동률 %)
    @Column(name = "change_rate", precision = 5, scale = 2)
    private BigDecimal changeRate;

    // 데이터 수집 날짜
    @Column(name = "timestamp", nullable = false)
    private LocalDate timestamp;
}
