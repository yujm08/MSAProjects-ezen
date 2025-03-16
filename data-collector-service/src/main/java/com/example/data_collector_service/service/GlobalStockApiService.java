package com.example.data_collector_service.service;

import com.example.data_collector_service.entity.GlobalDailyStock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalStockApiService {

    private final RestTemplate restTemplate;
    private final DailyStockService dailyStockService;
    private final OAuthTokenService oAuthTokenService;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.overseas-api-url}")
    private String overseasApiUrl;

    // 기존 accessToken 멤버 변수는 제거하고, 매 API 호출 시 최신 토큰을 가져옵니다.
    // private String accessToken; 

    @PostConstruct
    public void init() {
        // 기존에는 여기서 토큰을 한번 받아 저장했으나, 5시간마다 새로 발급받기 위해 매 호출 시 oAuthTokenService.getAccessToken()을 사용합니다.
        // 그래서 초기화 시 단순 로그만 남깁니다.
        log.info("GlobalStockApiService: 초기화 완료 (토큰은 매 API 호출 시 갱신)");
    }

    /**
     * 하드코딩된 해외 종목 목록 (20개)
     * "exchangeCode"는 EXCD (예: NAS, NYS, HKS 등)
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
            new ForeignStockInfo("BRK/B", "버크셔B주",      "NYS"),
            new ForeignStockInfo("TSM",   "TSMC",          "NYS"),
            new ForeignStockInfo("BABA",  "알리바바",       "NYS"),
            new ForeignStockInfo("NIO",   "니오(중국전기차)", "NYS"),
            new ForeignStockInfo("XOM",   "엑슨모빌",       "NYS"),
            new ForeignStockInfo("KO",    "코카콜라",       "NYS"),
            new ForeignStockInfo("JPM",   "JP모건",         "NYS"),
            new ForeignStockInfo("V",     "비자",           "NYS"),
            new ForeignStockInfo("09988","알리바바(홍콩)","HKS"),
            new ForeignStockInfo("09618","징둥닷컴",       "HKS"),
            new ForeignStockInfo("00700","텐센트",         "HKS")
    );

    /**
     * 해외 종목 전체 조회 메소드 (시장 타입별)
     * marketType: "GLOBAL"이면 HKS 제외, "HKS"이면 홍콩 종목만 조회
     */
    public void fetchForeignStocksByMarket(String marketType) {
        List<ForeignStockInfo> stocksToFetch;
        if ("HKS".equalsIgnoreCase(marketType)) {
            stocksToFetch = FOREIGN_STOCKS.stream()
                    .filter(stock -> "HKS".equalsIgnoreCase(stock.getExchangeCode()))
                    .collect(Collectors.toList());
        } else if ("GLOBAL".equalsIgnoreCase(marketType)) {
            stocksToFetch = FOREIGN_STOCKS.stream()
                    .filter(stock -> !"HKS".equalsIgnoreCase(stock.getExchangeCode()))
                    .collect(Collectors.toList());
        } else {
            stocksToFetch = FOREIGN_STOCKS;
        }

        for (ForeignStockInfo stock : stocksToFetch) {
            try {
                // 호출 제한 대응: 0.5초 대기 (모의투자의 경우 1초당 2건 제한)
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // API 문서에 따르면: 
                // QueryParam: AUTH="", EXCD=거래소코드, SYMB=종목코드
                // ex) ?AUTH=&EXCD=NAS&SYMB=TSLA
                String url = overseasApiUrl + "?AUTH=&EXCD={excd}&SYMB={symb}";

                HttpHeaders headers = new HttpHeaders();  // HTTP Header 설정
                headers.setContentType(MediaType.APPLICATION_JSON);  // 인증: "Bearer xxxxx" 형태로 지정

                // 수정된 부분: 매 API 호출 시 최신 토큰을 사용하도록 oAuthTokenService.getAccessToken() 호출
                String accessToken = oAuthTokenService.getAccessToken();
                headers.set("authorization", "Bearer " + accessToken);

                headers.set("appkey", appKey);  // 필수 헤더
                headers.set("appsecret", appSecret);
                headers.set("tr_id", "HHDFS00000300"); // "tr_id" = HHDFS00000300 (실전/모의투자 공통?)
                headers.set("custtype", "P");  //  문서에서 "custtype"=P(개인)

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

                    if (body != null && "0".equals(body.getRt_cd())) {  // 정상 처리
                        ForeignPriceOutput output = body.getOutput();

                        // 방어 코드: last와 rate 값이 null 또는 빈 문자열인지 확인
                        String lastStr = output.getLast();
                        String rateStr = output.getRate();
                        if (lastStr == null || lastStr.trim().isEmpty()) {
                            log.warn("해외주식 API 응답에서 last 값이 null 또는 빈 문자열입니다. 종목코드: {}", stock.getStockCode());
                            continue;
                        }
                        if (rateStr == null || rateStr.trim().isEmpty()) {
                            log.warn("해외주식 API 응답에서 rate 값이 null 또는 빈 문자열입니다. 종목코드: {}", stock.getStockCode());
                            continue;
                        }

                        GlobalDailyStock newData = new GlobalDailyStock();
                        newData.setStockCode(stock.getStockCode());
                        newData.setStockName(stock.getStockName());
                        newData.setExchangeCode(stock.getExchangeCode());
                        // 앞뒤 공백 제거 후 BigDecimal 생성
                        newData.setCurrentPrice(new BigDecimal(lastStr.trim()));
                        newData.setChangeRate(new BigDecimal(rateStr.trim()));
                        newData.setTimestamp(LocalDateTime.now());

                        dailyStockService.saveGlobalDailyStock(newData);  // DB 저장 (DailyStockService 활용)
                        log.info("해외주식 저장 완료: {} ({}), 현재가={}", 
                                 stock.getStockCode(), stock.getStockName(), lastStr);
                    } else {
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
     * 해외 종목 정보 DTO (하드코딩용)
     */
    @Data
    @AllArgsConstructor
    public static class ForeignStockInfo {
        private String stockCode;     // "TSLA"
        private String stockName;     // "테슬라"
        private String exchangeCode;  // "NAS"
    }

    /**
     * 해외 주식 API 응답 구조 (문서에 맞춤)
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
