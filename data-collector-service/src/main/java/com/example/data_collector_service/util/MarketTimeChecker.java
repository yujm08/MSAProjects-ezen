package com.example.data_collector_service.util;

import java.time.LocalTime;

//각 시장의 정규 거래 시간이 언제인지를 판별하는 유틸리티.
public class MarketTimeChecker {

    // 한국 주식 정규장: 09:00 ~ 15:30
    public static boolean isKoreanMarketOpen() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(LocalTime.of(9, 0)) && !now.isAfter(LocalTime.of(15, 30));
    }

    
    // 해외(미국) 주식 정규장: 한국시간으로 23:30 ~ 06:00
    public static boolean isGlobalMarketOpen() {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(23, 30)) || now.isBefore(LocalTime.of(6, 0));
    }

    /**
     * 홍콩시장 정규장 시간: 오전 세션 10:30 ~ 13:00, 오후 세션 14:00 ~ 17:00.
     */
    public static boolean isHongKongMarketOpen() {
        LocalTime now = LocalTime.now();
        boolean morning = !now.isBefore(LocalTime.of(10, 30)) && !now.isAfter(LocalTime.of(13, 0));
        boolean afternoon = !now.isBefore(LocalTime.of(14, 0)) && !now.isAfter(LocalTime.of(17, 0));
        return morning || afternoon;
    }
}
