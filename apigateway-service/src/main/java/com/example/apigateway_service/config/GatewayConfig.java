package com.example.apigateway_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.Part;

@Configuration
public class GatewayConfig {
    @Bean
    public HttpMessageReader<Part> partHttpMessageReader() {
        return new DefaultPartHttpMessageReader();
    }

    @Bean
    public MultipartBodyBuilder multipartBodyBuilder() {
        return new MultipartBodyBuilder();
    }
}
