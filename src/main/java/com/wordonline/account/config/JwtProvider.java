package com.wordonline.account.config;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.wordonline.account.domain.Member;
import com.wordonline.account.domain.PrincipalDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtEncoder jwtEncoder;

    private final long expiry = 36000L;

    public String getJwt(Member member) {
        PrincipalDetails principal = new PrincipalDetails(member);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());

        Instant now = Instant.now();

        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(authentication.getName())
                .claim("scope", scope)
                .claim("memberId", member.getId())
                .build();

        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
        return jwt.getTokenValue();
    }

    public String generateServerToken(String scope, Long expiryMinutes) {
        Instant now = Instant.now();
        
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .subject("server")
                .claim("scope", scope)
                .claim("type", "server_token");

        // If expiryMinutes is null, token never expires (or set to a very long time)
        if (expiryMinutes != null) {
            claimsBuilder.expiresAt(now.plusSeconds(expiryMinutes * 60));
        } else {
            // Set to 100 years in the future for "infinite" tokens
            claimsBuilder.expiresAt(now.plusSeconds(60L * 60 * 24 * 365 * 100));
        }

        JwtClaimsSet claims = claimsBuilder.build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
        return jwt.getTokenValue();
    }
}
