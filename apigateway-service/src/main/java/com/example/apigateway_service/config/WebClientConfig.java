package com.example.apigateway_service.config;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

@Configuration
public class WebClientConfig {
    // 이 클래스는 Spring의 설정 클래스입니다.
    // WebClient를 설정하기 위해 @Configuration을 사용하고 있습니다.
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder() // WebClient.Builder 객체를 빌드하는 메소드
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().secure(t -> 
                    // ReactorNetty의 HttpClient를 사용하여 HTTPS 연결을 설정합니다.
                    // secure(t -> ...): 보안 설정을 구성하는 메소드입니다.
                    t.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE))))) 
                    // SSLContext를 생성하고 InsecureTrustManagerFactory.INSTANCE를 사용하여 인증서를 검증하지 않도록 설정합니다.
                    // 이 설정은 개발 환경에서만 사용해야 하며, 운영 환경에서는 인증서 검증을 반드시 활성화해야 합니다.
                
                .baseUrl("https://localhost:8443"); // 기본 URL을 https://localhost:8443으로 설정
                // 모든 WebClient 요청에서 기본적으로 이 URL을 사용하게 됩니다.
                // API Gateway의 대상 서비스가 이 URL에서 실행되고 있어야 합니다.
    }
}

