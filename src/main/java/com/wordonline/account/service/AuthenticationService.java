package com.wordonline.account.service;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.wordonline.account.config.JwtProvider;
import com.wordonline.account.domain.Member;
import com.wordonline.account.domain.PrincipalDetails;
import com.wordonline.account.dto.AuthResponse;
import com.wordonline.account.dto.MemberRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final static String LOGIN_FAIL_MESSAGE = "이메일 또는 비밀번호가 잘못됐습니다.";
    private final static String EMAIL_REDUNDANT = "사용 중인 Email입니다.";

    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    public Mono<AuthResponse> join(MemberRequest memberRequest) {
        return memberService.getMember(memberRequest.email())
                .hasElement()
                .flatMap(exists ->
                        {
                            if (exists) {
                                return Mono.error(new IllegalArgumentException(EMAIL_REDUNDANT));
                            }
                            return memberService.createMember(memberRequest)
                                    .flatMap(id -> login(memberRequest));
                        });
    }

    public Mono<AuthResponse> login(MemberRequest memberRequest) {

        Mono<Member> memberMono = memberService.getMember(
                memberRequest.email());

        return memberMono.map(member -> new AuthResponse(jwtProvider.getJwt(member)));
    }
}
