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

    // 데이터 수집 날짜 (Unix Timestamp → DATE 변환 필요)
    @Column(name = "timestamp", nullable = false)
    private LocalDate timestamp;
}
