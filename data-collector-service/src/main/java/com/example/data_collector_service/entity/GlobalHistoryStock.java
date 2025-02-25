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

    // 실시간 데이터 테이블의 id를 참조 (Foreign Key)
    @Column(name = "global_daily_stock_id", nullable = false)
    private Long globalDailyStockId;

    // 주식 코드 (Data Body 키: ticker)
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;

    // 주식 이름 (Data Body 키: name)
    @Column(name = "stock_name", length = 50, nullable = false)
    private String stockName;

    // 종가 (마감 가격, Data Body 키: c, DECIMAL(10,4))
    @Column(name = "closing_price", precision = 10, scale = 4, nullable = false)
    private BigDecimal closingPrice;

    // 데이터 수집 날짜 (Data Body 키: t; Unix Timestamp → DATE 변환 필요)
    @Column(name = "timestamp", nullable = false)
    private LocalDate timestamp;
}
