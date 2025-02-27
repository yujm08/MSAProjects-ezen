package com.example.userservice.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
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
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
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

        // === 새로 추가된 부분: 직접 "Set-Cookie" 헤더 작성 ===
        int accessMaxAgeSeconds = (int) (accessTokenValidityInMillis / 1000);
        int refreshMaxAgeSeconds = (int) (refreshTokenValidityInMillis / 1000);

        // SameSite=None; Secure; HttpOnly; Path=/
        // => HTTPS 환경에서 Cross-Site 쿠키 허용
        //이지만 테스트할 때에는 HTTP에서도 허용할 것이기 때문에 배포 전에 Secure; 로 바꾸기
        String accessCookieHeader = String.format(
            "accessToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
            accessToken,
            accessMaxAgeSeconds
        );
        String refreshCookieHeader = String.format(
            "refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
            refreshToken,
            refreshMaxAgeSeconds
        );
        // Cookie accessCookie = new Cookie("accessToken", accessToken);
        // accessCookie.setHttpOnly(true);
        // accessCookie.setSecure(false);  // 로컬 http 테스트용이었음
        // accessCookie.setPath("/");
        // accessCookie.setMaxAge((int) (accessTokenValidityInMillis / 1000));
        //
        // Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        // refreshCookie.setHttpOnly(true);
        // refreshCookie.setSecure(false); // 로컬 http 테스트용이었음
        // refreshCookie.setPath("/");
        // refreshCookie.setMaxAge((int) (refreshTokenValidityInMillis / 1000));
        //
        // res.addCookie(accessCookie);
        // res.addCookie(refreshCookie);

        // === 이제는 addCookie() 대신, 직접 "Set-Cookie" 헤더만 사용 ===
        res.addHeader("Set-Cookie", accessCookieHeader);
        res.addHeader("Set-Cookie", refreshCookieHeader);

        // 필요한 경우, 로그인 성공 응답 바디나 상태 코드를 설정할 수도 있음
        // res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write("로그인 성공");
    }


}
