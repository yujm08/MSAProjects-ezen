package com.example.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final Environment environment;
    private JwtTokenUtil jwtTokenUtil;

    public JwtAuthorizationFilter(Environment environment, JwtTokenUtil jwtTokenUtil) {
        this.environment = environment;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 쿠키 배열에서 accessToken 쿠키를 찾아 값 추출
        String accessToken = getTokenFromCookies(request.getCookies(), "accessToken");
        if (accessToken != null) {
            // 2. 토큰 검증
            Claims claims = validateToken(accessToken);
            if (claims != null) {
                // 토큰이 유효하면 userId, roles(권한 목록) 추출
                String userId = claims.getSubject();
                var roles = claims.get("roles", java.util.List.class);

                // 3. Authentication 객체 생성 후 SecurityContextHolder에 저장
                Authentication authentication = createAuthentication(userId, roles);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("JWT Authentication successful for userId: {}", userId);
            }
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 주어진 쿠키 배열에서 특정 쿠키 이름(cookieName)을 찾아 해당 값을 반환.
     * 찾지 못하면 null 반환.
     */
    private String getTokenFromCookies(Cookie[] cookies, String cookieName) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * JWT 토큰을 검증하고, 유효하면 Claims 객체를 반환. 
     * 유효하지 않으면 null 반환.
     */
    private Claims validateToken(String token) {
        try {
            // JwtTokenUtil에서 재사용하는 secret key를 가져옴
            SecretKey secretKey = jwtTokenUtil.getSecretKey();

            // JJWT의 parser + verifyWith(secretKey)를 통해 서명 및 유효성 검증
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * userId(주체)와 roles(권한 목록)로 Authentication 객체를 생성하여 반환.
     */
    private Authentication createAuthentication(String userId, java.util.List roles) {
        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.toString()))
                .collect(Collectors.toList());

        // 생성자를 사용하여 인증된 토큰 생성
        // 첫 번째 파라미터: principal (userId)
        // 두 번째 파라미터: credentials (보안상 null 사용)
        // 세 번째 파라미터: authorities (권한 목록)
        return new UsernamePasswordAuthenticationToken(
                userId, 
                null, 
                (java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>) authorities
        );
    }
}
