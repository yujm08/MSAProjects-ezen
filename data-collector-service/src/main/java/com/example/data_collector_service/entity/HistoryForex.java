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
@Table(name = "history_forex")
public class HistoryForex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 통화 코드 (Data Body 키: CUR_CD)
    @Column(name = "currency_code", length = 10, nullable = false)
    private String currencyCode;

    // 통화 이름 (Data Body 키: CUR_NM)
    @Column(name = "currency_name", length = 50, nullable = false)
    private String currencyName;

    // 하루 종료 시점의 매매기준율 (종가, Data Body 키: DEAL_BASC_RT) DECIMAL(10,4)
    @Column(name = "closing_rate", precision = 10, scale = 4, nullable = false)
    private BigDecimal closingRate;

    // 등록일자 (Data Body 키: REG_DT)
    @Column(name = "date", nullable = false)
    private LocalDate date;
}