package com.wordonline.account.service;

import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.wordonline.account.config.JwtProvider;
import com.wordonline.account.domain.Member;
import com.wordonline.account.dto.AuthResponse;
import com.wordonline.account.dto.JoinRequest;
import com.wordonline.account.dto.LoginRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final static String LOGIN_FAIL_MESSAGE = "이메일 또는 비밀번호가 잘못됐습니다.";
    private final static String EMAIL_REDUNDANT = "사용 중인 Email입니다.";

    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    public Mono<AuthResponse> join(JoinRequest joinRequest) {
        return memberService.getMember(joinRequest.email())
                .hasElement()
                .flatMap(exists ->
                        {
                            if (exists) {
                                return Mono.error(new IllegalArgumentException(EMAIL_REDUNDANT));
                            }
                            return memberService.createMember(joinRequest)
                                    .flatMap(id -> login(new LoginRequest(joinRequest)));
                        });
    }

    public Mono<AuthResponse> login(LoginRequest memberRequest) {

        Mono<Member> memberMono = memberService.getMember(memberRequest.email());

        return memberMono
                .onErrorMap(
                        throwable -> new AuthorizationDeniedException(LOGIN_FAIL_MESSAGE)
                )
                .handle((member, sink) -> {
                    if (member == null || !member.validatePassword(memberRequest.password(), passwordEncoder)) {
                        sink.error(new AuthorizationDeniedException(LOGIN_FAIL_MESSAGE));
                        return;
                    }
                    sink.next(new AuthResponse(jwtProvider.getJwt(member)));
                });
    }
}
