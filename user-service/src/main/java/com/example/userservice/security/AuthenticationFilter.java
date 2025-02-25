package com.example.userservice.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.example.userservice.dto.UserDto;
import com.example.userservice.service.UserService;
import com.example.userservice.vo.RequestLogin;

import org.springframework.security.core.userdetails.User;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

@Slf4j
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter{
	private UserService userService;
    private Environment environment;
    private JwtTokenUtil jwtTokenUtil;

    public AuthenticationFilter(AuthenticationManager authenticationManager,
                                   UserService userService, Environment environment, JwtTokenUtil jwtTokenUtil) {
        super(authenticationManager);
        this.userService = userService;
        this.environment = environment;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
            throws AuthenticationException {
        try {

            RequestLogin creds = new ObjectMapper().readValue(req.getInputStream(), RequestLogin.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(creds.getEmail(), creds.getPassword(), new ArrayList<>()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        // 사용자 이름을 가져오고, 사용자 정보를 DB에서 조회
        String userName = ((User) auth.getPrincipal()).getUsername();
        UserDto userDetails = userService.getUserDetailsByEmail(userName);

        // JwtTokenUtil에서 재사용하는 secret key를 가져옴
        SecretKey secretKey = jwtTokenUtil.getSecretKey();
        Instant now = Instant.now();

        // Access Token 생성 (짧은 유효기간: 15분)
        long accessTokenValidityInMillis = Long.parseLong(environment.getProperty("token.expiration_time"));
        String accessToken = Jwts.builder()
                .setSubject(userDetails.getUserId())
                // 사용자 권한 정보를 토큰의 클레임에 추가
                .claim("roles", auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .setExpiration(Date.from(now.plusMillis(accessTokenValidityInMillis)))
                .setIssuedAt(Date.from(now))
                .signWith(secretKey)
                .compact();

        // Refresh Token 생성 (긴 유효기간: 7일)
        long refreshTokenValidityInMillis = Long.parseLong(environment.getProperty("token.refresh_expiration_time"));
        String refreshToken = Jwts.builder()
                .setSubject(userDetails.getUserId())
                .setExpiration(Date.from(now.plusMillis(refreshTokenValidityInMillis)))
                .setIssuedAt(Date.from(now))
                .signWith(secretKey)
                .compact();

        // Access Token을 HttpOnly, Secure 쿠키로 생성하여 응답에 추가 (쿠키 만료 시간은 토큰 유효기간과 동일)
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true); // 클라이언트 사이드 스크립트 접근 불가 (XSS 방어)
        accessCookie.setSecure(true);   // HTTPS 전송만 허용
        accessCookie.setPath("/");      // 전체 애플리케이션에서 사용
        accessCookie.setMaxAge((int) (accessTokenValidityInMillis / 1000)); // 초 단위

        // Refresh Token을 HttpOnly, Secure 쿠키로 생성
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (refreshTokenValidityInMillis / 1000));

        // 응답에 쿠키 추가하여 클라이언트로 전송
        res.addCookie(accessCookie);
        res.addCookie(refreshCookie);
    }

}
