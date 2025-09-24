package com.wordonline.account.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.wordonline.account.domain.PrincipalDetails;
import com.wordonline.account.service.MemberService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {

    @Getter
    private final SecretKey secretKey;
    private final MemberService memberService;

    public JwtUtil(@Value("${jwt.secret}") String secretKey, MemberService memberService) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.memberService = memberService;
    }

    private Claims getClaims(String jwt) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    public Authentication getAuthentication(String jwt) {
        Claims claims = getClaims(jwt);

        String auth = Optional.ofNullable(claims.get("auth", String.class))
                .orElseThrow(() -> new RuntimeException("JWT_AUTH required"));

        Long memberId = Optional.ofNullable(claims.get("memberId", Long.class))
                .orElseThrow(() -> new RuntimeException("Member ID required"));

        Collection<GrantedAuthority> authorities = Arrays.stream(auth.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());


        PrincipalDetails principal = new PrincipalDetails(
                memberService.getMember(memberId)
                        .block()
        );
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Not Valid JWT", e.getMessage());
            return false;
        }

    }
}
