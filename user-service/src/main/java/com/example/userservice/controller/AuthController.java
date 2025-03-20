package com.example.userservice.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.userservice.security.JwtTokenUtil;
import com.example.userservice.service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/")
public class AuthController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(UserService userService, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    // JWT 쿠키 검증이 통과된 상태면 SecurityContextHolder에 Authentication 존재
    @GetMapping("/users/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // 쿠키가 없거나 토큰이 무효 → 인증 실패
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }
        // principal = userId (JwtAuthorizationFilter에서 setSubject(userId))
        String userId = (String) authentication.getPrincipal();
        
        // 필요하다면 DB 조회로 사용자 상세정보(UserDto) 반환
        // 간단히 userId만 리턴하는 예:
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("isLoggedIn", true);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken != null) {
            Claims claims = jwtTokenUtil.validateToken(refreshToken);
            if (claims != null) {
                String userId = claims.getSubject();
                List<String> roles = claims.get("roles", List.class);

                // 새로운 accessToken 생성
                String newAccessToken = jwtTokenUtil.generateAccessToken(userId, roles);

                // 새로운 accessToken을 쿠키로 설정
                Cookie accessTokenCookie = new Cookie("accessToken", newAccessToken);
                accessTokenCookie.setHttpOnly(true);
                accessTokenCookie.setSecure(true); // 프로덕션에서는 반드시 true로 설정
                accessTokenCookie.setPath("/"); // 쿠키의 유효 범위 설정
                accessTokenCookie.setMaxAge(15 * 60); // 15분

                // 쿠키를 응답에 추가
                response.addCookie(accessTokenCookie);

                return ResponseEntity.ok(Collections.singletonMap("newAccessToken", newAccessToken));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
    }

    // 쿠키 무효화 (로그아웃)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // 실제 원본 쿠키에서 확인한 값들:
        // Domain=localhost
        // Path=/
        // HttpOnly, Secure, SameSite=None
        // (만약 원본이 HostOnly(도메인 미지정)였다면, Domain=... 부분을 제거해야 합니다.)

        // 1) accessToken 만료
        String deleteAccessCookie = String.join("; ",
            "accessToken=",                 // 쿠키값을 빈 문자열로
            "Max-Age=0",                    // 즉시 만료
            "Path=/",                       // 원본과 동일
            "Domain=localhost",            // 원본이 domain=localhost였다면
            "HttpOnly",
            "Secure",
            "SameSite=None"
        );

        // 2) refreshToken 만료
        String deleteRefreshCookie = String.join("; ",
            "refreshToken=",
            "Max-Age=0",
            "Path=/",
            "Domain=localhost",
            "HttpOnly",
            "Secure",
            "SameSite=None"
        );

        // 3) 직접 Set-Cookie 헤더로 추가
        response.addHeader("Set-Cookie", deleteAccessCookie);
        response.addHeader("Set-Cookie", deleteRefreshCookie);

        return ResponseEntity.ok("Logout successful");
    }

}

