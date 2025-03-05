package com.example.data_collector_service.config;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

/*OkHttpClient

OkHttpClient는 HTTP 요청을 보내고 받을 수 있는 라이브러리야.
WebSocket 클라이언트로도 사용할 수 있어서, 서버와의 WebSocket 통신을 쉽게 구현 가능.
비동기 HTTP 요청을 보내는 데 많이 사용됨. 
OkHttp 라이브러리는 기본적으로 ping/pong 컨트롤 프레임을 자동으로 처리 */

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                // 필요하면 timeout 등 설정
                .build();
    }
}
