package com.example.data_collector_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "krean_stock_master")
public class KreanStockMaster {

    @Id
    @Column(name = "stock_code", length = 10)
    private String stockCode;

    @Column(name = "stock_name", length = 100, nullable = false)
    private String stockName;
}

