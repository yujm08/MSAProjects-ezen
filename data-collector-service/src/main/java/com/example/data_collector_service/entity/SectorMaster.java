package com.example.data_collector_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sector_master")
public class SectorMaster {

    // 업종 코드가 기본키 (VARCHAR(10))
    @Id
    @Column(name = "sector_code", length = 10)
    private String sectorCode;

    // 업종 이름 (VARCHAR(100))
    @Column(name = "sector_name", length = 100, nullable = false)
    private String sectorName;
}
