package com.example.apigateway_service.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileUploadHeaderFilter extends AbstractGatewayFilterFactory<FileUploadHeaderFilter.Config> {

    public FileUploadHeaderFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String method = exchange.getRequest().getMethod().name();
            
            if (("POST".equals(method) || "PUT".equals(method)) &&
                headers.containsKey(HttpHeaders.CONTENT_TYPE) &&
                headers.get(HttpHeaders.CONTENT_TYPE).toString().contains("multipart/form-data")) {
                
                log.debug("Multipart request detected with Content-Type: {}", headers.getContentType());
                
                // Content-Type 헤더의 boundary 정보 유지
                // 아무 것도 변경하지 않고 그대로 전달
            }
            
            return chain.filter(exchange);
        };
    }

    public static class Config {
    }

    @Override
    public String name() {
        return "FileUploadHeader";
    }
}
