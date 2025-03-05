package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.GlobalDailyStock;
import com.example.data_collector_service.repository.GlobalDailyStockRepository;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalStockApiService {

    private final RestTemplate restTemplate;
    private final DailyStockService dailyStockService; // 해외 데이터 최종 저장 시 사용
    private final OAuthApprovalService oAuthApprovalService;

    // [application.yml]에서 주입받는 값들
    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.overseas-api-url}")
    private String overseasApiUrl;

    private String accessToken; 

    // Bean 초기화 후 accessToken 초기화
    @PostConstruct
    public void init() {
        this.accessToken = oAuthApprovalService.getApprovalKey();
        log.info("GlobalStockApiService: accessToken 초기화 완료");
    }
    /**
     * 1) 하드코딩된 해외 종목 목록 (20개)
     *    - "exchangeCode"는 EXCD (예: NAS, NYS, HKS 등)
     */
    private static final List<ForeignStockInfo> FOREIGN_STOCKS = List.of(
            new ForeignStockInfo("TSLA",  "테슬라",         "NAS"),
            new ForeignStockInfo("AAPL",  "애플",           "NAS"),
            new ForeignStockInfo("NVDA",  "엔비디아",       "NAS"),
            new ForeignStockInfo("MSFT",  "마이크로소프트", "NAS"),
            new ForeignStockInfo("AMZN",  "아마존",         "NAS"),
            new ForeignStockInfo("GOOG",  "구글(알파벳)",   "NAS"),
            new ForeignStockInfo("META",  "메타(페이스북)", "NAS"),
            new ForeignStockInfo("AMD",   "AMD",           "NAS"),
            new ForeignStockInfo("NFLX",  "넷플릭스",       "NAS"),
            new ForeignStockInfo("BRK.B", "버크셔B주",      "NYS"),
            new ForeignStockInfo("TSM",   "TSMC",          "NYS"),
            new ForeignStockInfo("BABA",  "알리바바",       "NYS"),
            new ForeignStockInfo("NIO",   "니오(중국전기차)", "NYS"),
            new ForeignStockInfo("XOM",   "엑슨모빌",       "NYS"),
            new ForeignStockInfo("KO",    "코카콜라",       "NYS"),
            new ForeignStockInfo("JPM",   "JP모건",         "NYS"),
            new ForeignStockInfo("V",     "비자",           "NYS"),
            new ForeignStockInfo("9988.HK","알리바바(홍콩)","HKS"),
            new ForeignStockInfo("9618.HK","징둥닷컴",       "HKS"),
            new ForeignStockInfo("700.HK","텐센트",         "HKS")
    );

    /**
     * 2) 해외 종목 전체 조회 메소드
     *    - for문을 돌면서 각 종목에 대해 REST API 호출
     *    - 응답(JSON)을 파싱 후 DB에 저장
     */
    public void fetchAllForeignStocks() {
        for (ForeignStockInfo stock : FOREIGN_STOCKS) {
            try {
                // API 문서에 따르면: 
                // QueryParam: AUTH="", EXCD=거래소코드, SYMB=종목코드
                // ex) ?AUTH=&EXCD=NAS&SYMB=TSLA
                String url = overseasApiUrl + "?AUTH=&EXCD={excd}&SYMB={symb}";

                // HTTP Header 설정
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // 인증: "Bearer xxxxx" 형태로 지정
                headers.set("authorization", accessToken);
                // 필수 헤더
                headers.set("appkey", appKey);
                headers.set("appsecret", appSecret);
                // 문서에서 "tr_id" = HHDFS00000300 (실전/모의투자 공통?), "custtype"=P(개인)
                headers.set("tr_id", "HHDFS00000300");
                headers.set("custtype", "P");

                // 요청 엔티티 생성
                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                // REST API 호출
                log.info("[OverseasStockApiService] 해외주식 API 호출 - EXCD={}, SYMB={}", stock.getExchangeCode(), stock.getStockCode());
                ResponseEntity<ForeignPriceResponse> responseEntity = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        ForeignPriceResponse.class,
                        stock.getExchangeCode(),
                        stock.getStockCode()
                );

                // 응답 처리
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    ForeignPriceResponse body = responseEntity.getBody();
                    if (body != null && "0".equals(body.getRt_cd())) {
                        // 정상 처리
                        ForeignPriceOutput output = body.getOutput();
                        // output.last(현재가), output.rate(등락률) 등 파싱
                        GlobalDailyStock newData = new GlobalDailyStock();
                        newData.setStockCode(stock.getStockCode());
                        newData.setStockName(stock.getStockName());
                        newData.setExchangeCode(stock.getExchangeCode());
                        newData.setCurrentPrice(new BigDecimal(output.getLast()));
                        newData.setChangeRate(new BigDecimal(output.getRate().trim()));
                        newData.setTimestamp(LocalDateTime.now());

                        // DB 저장 (DailyStockService 활용)
                        dailyStockService.saveGlobalDailyStock(newData);
                        log.info("해외주식 저장 완료: {} ({}), 현재가={}", 
                                 stock.getStockCode(), stock.getStockName(), output.getLast());
                    } else {
                        // 오류 응답
                        String msg = (body != null) ? body.getMsg1() : "응답 body=null";
                        log.warn("해외주식 API 오류 - 종목코드: {}, msg: {}", stock.getStockCode(), msg);
                    }
                } else {
                    log.error("해외주식 API 호출 실패 - 종목코드: {}, HTTP Status: {}",
                            stock.getStockCode(), responseEntity.getStatusCode());
                }

            } catch (Exception e) {
                log.error("해외주식 조회 예외 발생 - 종목코드: {}, err: {}", stock.getStockCode(), e.getMessage(), e);
            }
        }
    }

    /**
     * 3) 해외 종목 정보 DTO (하드코딩용)
     */
    @Data
    @AllArgsConstructor
    public static class ForeignStockInfo {
        private String stockCode;     // "TSLA"
        private String stockName;     // "테슬라"
        private String exchangeCode;  // "NAS"
    }

    /**
     * 4) 해외 주식 API 응답 구조 (문서에 맞춤)
     */
    @Data
    public static class ForeignPriceResponse {
        private String rt_cd;   // "0" 이면 성공
        private String msg_cd;
        private String msg1;
        private ForeignPriceOutput output;
    }

    @Data
    public static class ForeignPriceOutput {
        private String rsym;   // "DNASTSLA"
        private String zdiv;   // 소수점 자리수
        private String base;   // 전일 종가
        private String pvol;   // 전일 거래량
        private String last;   // 현재가
        private String sign;   // 대비 기호
        private String diff;   // 전일 대비
        private String rate;   // 등락율
        private String tvol;   // 거래량
        private String tamt;   // 거래대금
        private String ordy;   // 매수가능여부
    }
}
