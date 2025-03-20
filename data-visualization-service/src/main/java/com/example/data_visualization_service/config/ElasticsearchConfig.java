package com.example.data_visualization_service.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;  // ★ 주의
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.data.elasticsearch.rest.uris}")
    private String elasticsearchUris;

    @Value("${spring.data.elasticsearch.rest.headers.Authorization}")
    private String authHeader;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 1. 인증 헤더 설정
        var defaultHeaders = new BasicHeader[] {
            new BasicHeader("Authorization", authHeader)
        };

        // 2. Low-level RestClient 생성
        RestClient restClient = RestClient.builder(HttpHost.create(elasticsearchUris))
                                          .setDefaultHeaders(defaultHeaders)
                                          .build();

        // 3. ElasticsearchTransport 생성 (JacksonJsonpMapper 사용)
        ElasticsearchTransport transport = new RestClientTransport(
            restClient,
            new JacksonJsonpMapper()
        );

        // 4. ElasticsearchClient 생성
        return new ElasticsearchClient(transport);
    }

    @Bean(name = "elasticsearchTemplate")
    public ElasticsearchOperations elasticsearchOperations(ElasticsearchClient client) {
        // 공식 Java Client 기반의 Spring Data Elasticsearch Template
        return new ElasticsearchTemplate(client);
    }
}
