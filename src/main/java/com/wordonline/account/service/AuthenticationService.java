package com.wordonline.account.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

        return memberMono.map(member -> {
                    PrincipalDetails principal = new PrincipalDetails(member);

                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities());

                    String jwtToken = jwtProvider.createToken(authentication);
                    return new AuthResponse(jwtToken);
                }
        );
    }
}
