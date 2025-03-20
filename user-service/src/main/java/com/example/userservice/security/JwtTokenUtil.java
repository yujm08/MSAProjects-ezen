package com.example.userservice.security;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {
    private final SecretKey secretKey;
    
    public JwtTokenUtil(Environment environment) {
        // secret key를 단 한 번만 생성하여 재사용
        String secretStr = environment.getProperty("token.secret");
        // 여기서는 Base64 인코딩 없이 바로 bytes로 변환
        byte[] secretKeyBytes = secretStr.getBytes();
        this.secretKey = Keys.hmacShaKeyFor(secretKeyBytes);
    }
    
    public SecretKey getSecretKey() {
        return this.secretKey;
    }

    public String generateAccessToken(String userId, List<String> roles) {
        Instant now = Instant.now();
        long accessTokenValidityInMillis = 15 * 60 * 1000; // 15분
        return Jwts.builder()
                .setSubject(userId)
                .claim("roles", roles)
                .setExpiration(Date.from(now.plusMillis(accessTokenValidityInMillis)))
                .setIssuedAt(Date.from(now))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null; // 유효하지 않은 토큰
        }
    }
}
