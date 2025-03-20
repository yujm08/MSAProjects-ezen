package com.example.apigateway_service.filter;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    private final WebClient.Builder webClientBuilder;
    Environment env;

    public AuthorizationHeaderFilter(Environment env, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.env = env;
        this.webClientBuilder = webClientBuilder;
    }

    public static class Config {
        // Put configuration properties here
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            log.debug("Request path: " + request.getPath());  
            log.debug("Request method: " + request.getMethod());  

            // 1. Authorization 헤더에서 JWT 추출
            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String jwt = null;

            if (authorizationHeader != null) {
                jwt = authorizationHeader.replace("Bearer ", "");
            } else {
                // 2. Authorization 헤더가 없으면 Cookie에서 accessToken 추출
                jwt = getJwtFromCookie(request);
            }

            // 3. accessToken이 존재하고 유효한 경우 요청 필터링 진행
            if (jwt != null && isJwtValid(jwt)) {
                return chain.filter(exchange);
            }

            // 4. accessToken이 없거나 유효하지 않은 경우, refreshToken으로 새로운 accessToken 발급
            String refreshToken = getJwtFromCookie(request); // 쿠키에서 refreshToken 가져오기
            if (refreshToken != null) {
                return refreshAccessToken(exchange, refreshToken)
                        .flatMap(response -> chain.filter(exchange));
            }

            // 5. refreshToken도 없거나 accessToken 재발급 실패 시, UNAUTHORIZED 응답 반환
            return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(err);

        byte[] bytes = "The requested token is invalid.".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));
    }

    private boolean isJwtValid(String jwt) {
        byte[] secretKeyBytes = Base64.getEncoder().encode(env.getProperty("token.secret").getBytes());
        SecretKey signingKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());

        boolean returnValue = true;
        String subject = null;

        try {
            JwtParser jwtParser = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build();

            subject = jwtParser.parseClaimsJws(jwt).getBody().getSubject();
        } catch (Exception ex) {
            returnValue = false;
        }

        if (subject == null || subject.isEmpty()) {
            returnValue = false;
        }

        return returnValue;
    }

    // Cookie에서 accessToken을 추출하는 메서드
    private String getJwtFromCookie(ServerHttpRequest request) {
        if (request.getCookies().containsKey("accessToken")) {
            return request.getCookies().getFirst("accessToken").getValue();
        }
        return null;
    }

    // refreshToken을 사용하여 새로운 accessToken을 발급받고, 이를 UserService에 요청하여 전달
    private Mono<Void> refreshAccessToken(ServerWebExchange exchange, String refreshToken) {
        return webClientBuilder
                .build()
                .post()
                .uri("https://localhost:8443/user-service/refresh")  // 실제 URL로 변경
                .bodyValue(Collections.singletonMap("refreshToken", refreshToken))  // 요청 본문에 refreshToken 포함
                .retrieve()
                .bodyToMono(String.class)
                .then();
    }
}
