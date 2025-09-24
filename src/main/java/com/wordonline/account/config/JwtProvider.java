package com.wordonline.account.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.stream.Collectors;

import com.wordonline.account.domain.PrincipalDetails;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    private final JwtUtil jwtUtil;

    public String createToken(Authentication authentication) {

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(String::toUpperCase)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("auth", authorities)
                .claim("memberId", ((PrincipalDetails) authentication.getPrincipal()).getMemberId())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(jwtUtil.getSecretKey())
                .compact();
    }
}
