package com.example.userservice.security;

import javax.crypto.SecretKey;
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
}
