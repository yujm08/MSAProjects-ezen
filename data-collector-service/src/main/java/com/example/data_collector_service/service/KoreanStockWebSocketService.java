package com.example.data_collector_service.service;

import com.example.data_collector_service.buffer.RealTimeDataBuffer;
import com.example.data_collector_service.entity.KoreanDailyStock;
import com.example.data_collector_service.util.MarketTimeChecker;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * KoreanStockWebSocketService
 * 
 * 국내 주식 실시간체결가 데이터를 WebSocket을 통해 수집합니다.
 * 정규장(09:00 ~ 15:30) 시간 동안에만 동작하며, 응답 데이터를 파싱해 버퍼에 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KoreanStockWebSocketService {

    // OkHttpClient: WebSocket 연결에 사용
    private final OkHttpClient okHttpClient;
    // 실시간 데이터를 저장할 버퍼
    private final RealTimeDataBuffer dataBuffer;
    // JSON 직렬화/역직렬화에 사용 (Gson)
    private final Gson gson = new Gson();
    // OAuth 키(approvalKey) 발급 서비스
    private final OAuthApprovalKeyService oauthService;

    // application.yml에 정의된 국내 주식 웹소켓 URL (예: ws://ops.koreainvestment.com:31000)
    @Value("${kis.ws-url-domestic}")
    private String wsUrlDomestic;

    // 현재 활성화된 WebSocket 세션을 저장하는 변수 (동시성 이슈를 방지하기 위해 volatile 사용)
    private volatile WebSocket currentWebSocket;

    /**
     * 국내 주식 종목 구독을 시작하는 메서드
     * (예: stockCode = "005930"과 같이 6자리 종목 코드를 사용)
     */
    public synchronized void subscribeKoreanStock(String stockCode) {
        // 정규장 시간이 아니면 구독을 시작하지 않음
        if (!MarketTimeChecker.isKoreanMarketOpen()) {
            log.info("국내 정규장이 아니므로 국내 주식 구독을 시작하지 않습니다.");
            return;
        }
        // 종목 코드가 6자리 숫자인지 확인 (형식이 맞지 않으면 경고 후 종료)
        if (stockCode.length() != 6) {
            log.warn("tr_key 값은 6자리 숫자여야 합니다. 현재 값: {}", stockCode);
            return;
        }
        
        // 만약 이미 활성화된 WebSocket 세션이 있으면, 새 연결을 시도하지 않고 해당 세션에 구독 메시지를 전송
        if (currentWebSocket != null) {
            log.warn("이미 활성화된 WebSocket 세션이 있습니다. 기존 세션으로 구독 메시지를 전송합니다.");
            sendSubscriptionMessage(currentWebSocket, stockCode);
            return;
        }
        
        // OAuth 서비스로부터 approvalKey 발급 받음
        String approvalKey = oauthService.getApprovalKey();
        // WebSocket 연결을 위한 요청(Request) 생성
        Request request = new Request.Builder()
                // wsUrlDomestic에 "/H0STCNT0" 경로를 추가 (실제 트랜잭션 ID에 맞춤)
                .url(wsUrlDomestic + "/H0STCNT0")
                // 웹소켓 연결에 필요한 헤더 설정 (공식 문서에 명시된 값)
                .addHeader("approval_key", approvalKey)
                .addHeader("custtype", "P")
                .addHeader("tr_type", "1")
                .addHeader("content-type", "utf-8")
                .build();

        // OkHttpClient를 사용하여 WebSocket 연결 시작
        currentWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // 연결 성공 시 로그 출력
                log.info("국내 주식 WebSocket 연결 성공, 종목: {}", stockCode);
                // 최초 연결 시, 구독 메시지를 전송합니다.
                sendSubscriptionMessage(webSocket, stockCode);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    // PING 메시지 감지: "PINGPONG" 메시지 수신 시 PONG 응답 전송
                    if (text.contains("\"tr_id\":\"PINGPONG\"")) {
                        log.info("🔄 [WebSocket] PING 메시지 수신: {}", text);
                        String pongMessage = "{\"header\":{\"tr_id\":\"PONG\"}}";
                        webSocket.send(pongMessage);
                        log.info("✅ [WebSocket] PONG 응답 전송: {}", pongMessage);
                        return;
                    }
                    // 서버로부터 수신한 메시지를 '|' 구분자로 분리하여 처리
                    String[] parts = text.split("\\|");
                    if (parts.length < 4) {
                        log.warn("국내 주식 응답 데이터 길이 부족: {}", text);
                        return;
                    }

                    // 데이터 파싱 후 로그 출력
                    log.info("🔍 [WebSocket] 분리된 데이터: {}", (Object) parts);
                    
                    // 네 번째 파트(실제 데이터 부분)를 '^' 구분자로 분리
                    String dataPart = parts[3];
                    String[] tokens = dataPart.split("\\^");
                    if (tokens.length < 6) {
                        log.warn("국내 주식 응답 토큰 수 부족: {}", text);
                        return;
                    }

                    // 최종 데이터 매핑 후 로그 출력
                    log.info("📌 [WebSocket] 매핑된 종목: {}, 현재가: {}, 전일 대비율: {}", 
                            tokens[0], tokens[2], tokens[5]);
                    
                    // 수신한 데이터를 KoreanDailyStock 엔티티로 매핑
                    KoreanDailyStock data = new KoreanDailyStock();
                    data.setStockCode(tokens[0]);               // 종목 코드
                    data.setStockName("미매핑");                  // 종목명 (추후 매핑 필요)
                    data.setCurrentPrice(new BigDecimal(tokens[2])); // 현재가
                    data.setChangeRate(new BigDecimal(tokens[5]));     // 전일 대비율
                    data.setTimestamp(LocalDateTime.now());          // 현재 시간 (한국 시간 기준)
                    
                    log.info("💾 [WebSocket] 저장할 종목 데이터 - 종목코드: {}, 현재가: {}, 변동률: {}, 시간: {}",
                            data.getStockCode(), data.getCurrentPrice(), data.getChangeRate(), data.getTimestamp());

                    // 데이터를 실시간 데이터 버퍼에 저장
                    dataBuffer.putKoreanData(data.getStockCode(), data);
                } catch (Exception e) {
                    log.error("국내 주식 데이터 파싱 오류: {}", e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                // 연결 실패 시 오류 메시지 출력 및 현재 세션 상태 초기화
                String errorMsg = (t != null) ? t.getMessage() : "Unknown error";
                log.error("국내 주식 WebSocket 오류: {}", errorMsg);
                currentWebSocket = null;
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                // 연결 종료 시 로그 출력 및 현재 세션 상태 초기화
                log.info("국내 주식 WebSocket 연결 종료: code={}, reason={}", code, reason);
                currentWebSocket = null;
            }
        });
    }

    /**
     * 주어진 WebSocket 세션을 통해 지정된 종목코드에 대한 구독 메시지를 전송하는 메서드
     */
    private void sendSubscriptionMessage(WebSocket webSocket, String stockCode) {
        // 요청 메시지의 header 부분 생성 (승인 키, 고객 유형 등)
        String approvalKey = oauthService.getApprovalKey(); // 구독 요청 시마다 새 approvalKey를 사용할 수도 있습니다.
        Map<String, Object> header = Map.of(
                "approval_key", approvalKey,
                "custtype", "P",
                "tr_type", "1",
                "content-type", "utf-8"
        );
        // 요청 메시지의 body 부분 생성 (종목 코드 정보)
        Map<String, Object> input = Map.of(
                "tr_id", "H0STCNT0",
                "tr_key", stockCode
        );
        Map<String, Object> body = Map.of("input", input);
        // 전체 메시지: header와 body를 합쳐 JSON 형식으로 변환
        Map<String, Object> message = Map.of(
                "header", header,
                "body", body
        );
        String json = gson.toJson(message);
        log.info("📨 [WebSocket] 구독 메시지 전송 - 종목코드: {}, 메시지: {}", stockCode, json);
        webSocket.send(json);
    }
}


