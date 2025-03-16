package com.example.data_collector_service.util;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

public class MarketTimeChecker {

    /**
     * 한국 주식 정규장 (KRX)
     * - 시간대: Asia/Seoul
     * - 거래 요일: 월 ~ 금 (주말 휴장)
     * - 거래 시간: 09:00 ~ 15:30
     */
    public static boolean isKoreanMarketOpen() {
        // 한국 시간대로 현재 시각 계산
        ZonedDateTime nowKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        // 주말이면 거래 안 함
        DayOfWeek day = nowKst.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime currentTime = nowKst.toLocalTime();
        // 거래 시작 시간 09:00, 종료 시간 15:30 (종료 시간 포함 여부는 비즈니스 규칙에 따라 조정)
        return !currentTime.isBefore(LocalTime.of(9, 0)) && !currentTime.isAfter(LocalTime.of(15, 30));
    }

    /**
     * 미국 주식 정규장 (NYSE, NASDAQ 등)
     * - 시간대: America/New_York
     * - 거래 요일: 월 ~ 금 (주말 휴장)
     * - 거래 시간: 09:30 ~ 16:00
     */
    public static boolean isUSMarketOpen() {
        // 미국 뉴욕 시간대로 현재 시각 계산
        ZonedDateTime nowNy = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DayOfWeek day = nowNy.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime currentTime = nowNy.toLocalTime();
        return !currentTime.isBefore(LocalTime.of(9, 30)) && !currentTime.isAfter(LocalTime.of(16, 0));
    }

    /**
     * 홍콩 주식 정규장 (HKEX)
     * - 시간대: Asia/Hong_Kong
     * - 거래 요일: 월 ~ 금 (주말 휴장)
     * - 거래 시간: 두 세션으로 운영
     *   - 오전 세션: 10:30 ~ 13:00
     *   - 오후 세션: 14:00 ~ 17:00
     */
    public static boolean isHongKongMarketOpen() {
        // 홍콩 시간대로 현재 시각 계산
        ZonedDateTime nowHk = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"));
        DayOfWeek day = nowHk.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime currentTime = nowHk.toLocalTime();
        boolean morning = !currentTime.isBefore(LocalTime.of(10, 30)) && currentTime.isBefore(LocalTime.of(13, 0));
        boolean afternoon = !currentTime.isBefore(LocalTime.of(14, 0)) && currentTime.isBefore(LocalTime.of(17, 0));
        return morning || afternoon;
    }
}
