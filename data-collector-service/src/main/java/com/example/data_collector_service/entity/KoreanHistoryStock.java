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

    // 실시간 데이터 테이블의 id를 참조 (Foreign Key)
    @Column(name = "korean_daily_stock_id", nullable = false)
    private Long koreanDailyStockId;

    // 주식 코드 (Data Body 키: MKSC_SHRN_ISCD)
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;

    // 주식 이름 (업종 데이터 참조)
    @Column(name = "stock_name", length = 50, nullable = false)
    private String stockName;

    // 종가 (마감 가격, Data Body 키: STCK_PRPR, DECIMAL(10,4))
    @Column(name = "closing_price", precision = 10, scale = 4, nullable = false)
    private BigDecimal closingPrice;

    // 데이터 수집 날짜 (Data Body 키: BSOP_DATE)
    @Column(name = "timestamp", nullable = false)
    private LocalDate timestamp;
}
