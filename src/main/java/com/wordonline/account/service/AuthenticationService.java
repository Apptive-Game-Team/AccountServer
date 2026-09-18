package com.wordonline.account.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.wordonline.account.config.JwtProvider;
import com.wordonline.account.domain.GuestTokens;
import com.wordonline.account.domain.IssuedTokens;
import com.wordonline.account.domain.Member;
import com.wordonline.account.dto.JoinRequest;
import com.wordonline.account.dto.LoginRequest;
import com.wordonline.account.dto.TokenDelivery;
import com.wordonline.account.util.NicknameGenerator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final static String LOGIN_FAIL_MESSAGE = "이메일 또는 비밀번호가 잘못됐습니다.";
    private final static String EMAIL_REDUNDANT = "사용 중인 Email입니다.";
    private final static SecureRandom SECURE_RANDOM = new SecureRandom();
    private final static int GUEST_PASSWORD_BYTES = 24;

    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final NicknameGenerator nicknameGenerator;
    private final RefreshTokenService refreshTokenService;

    public Mono<IssuedTokens> join(JoinRequest joinRequest, TokenDelivery delivery) {
        return registerThenLogin(joinRequest, delivery,
                () -> memberService.createMember(joinRequest));
    }

    private Mono<IssuedTokens> registerThenLogin(JoinRequest joinRequest, TokenDelivery delivery,
            Supplier<Mono<Member>> createMember) {
        return memberService.getMember(joinRequest.email())
                .hasElement()
                .flatMap(exists ->
                {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                EMAIL_REDUNDANT));
                    }
                    return createMember.get()
                            .flatMap(created -> login(new LoginRequest(joinRequest), delivery));
                });
    }

    public Mono<IssuedTokens> login(LoginRequest memberRequest, TokenDelivery delivery) {
        return authenticate(memberRequest)
                .flatMap(member -> issueTokens(member, delivery));
    }

    /**
     * The Thymeleaf login form keeps the access token in its own cookie and has no use for a
     * refresh token, so signing in there does not open a token family.
     */
    public Mono<String> issueAccessToken(LoginRequest memberRequest) {
        return authenticate(memberRequest)
                .map(jwtProvider::getJwt);
    }

    /**
     * The member row is written with is_guest set, and login() reads it back, so the access token
     * this returns already carries the guest claim.
     */
    public Mono<GuestTokens> joinGuest(String name, TokenDelivery delivery) {
        return getRandomJoinRequest(name)
                .flatMap(joinRequest ->
                        registerThenLogin(joinRequest, delivery,
                                () -> memberService.createGuest(joinRequest))
                                .map(tokens -> new GuestTokens(tokens, joinRequest.password())));
    }

    public Mono<IssuedTokens> refresh(String presentedToken, TokenDelivery delivery) {
        return refreshTokenService.rotate(presentedToken, delivery.platform())
                .flatMap(rotated -> memberService.getMember(rotated.memberId())
                        .switchIfEmpty(Mono.error(() -> new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED, LOGIN_FAIL_MESSAGE)))
                        .map(member -> new IssuedTokens(
                                jwtProvider.getJwt(member),
                                rotated.refreshToken(),
                                jwtProvider.getAccessTokenExpirySeconds())));
    }

    public Mono<Void> logout(String presentedToken) {
        return refreshTokenService.revoke(presentedToken);
    }

    private Mono<Member> authenticate(LoginRequest memberRequest) {
        return memberService.getMember(memberRequest.email())
                .onErrorMap(
                        throwable -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                LOGIN_FAIL_MESSAGE)
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        LOGIN_FAIL_MESSAGE)))
                .handle((member, sink) -> {
                    if (!member.validatePassword(memberRequest.password(),
                            passwordEncoder)) {
                        sink.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                LOGIN_FAIL_MESSAGE));
                        return;
                    }
                    sink.next(member);
                });
    }

    private Mono<IssuedTokens> issueTokens(Member member, TokenDelivery delivery) {
        String accessToken = jwtProvider.getJwt(member);
        return refreshTokenService.issue(member.getId(), delivery.platform())
                .map(refreshToken -> new IssuedTokens(
                        accessToken,
                        refreshToken,
                        jwtProvider.getAccessTokenExpirySeconds()));
    }

    public Mono<JoinRequest> getRandomJoinRequest(String name) {
        String uniqueEmail = "guest_" + UUID.randomUUID() + "@example.com";
        String password = generateGuestPassword();
        if (name == null || name.isBlank() ) {
            return Mono.deferContextual(ctx -> {
                        LocaleContext localeContext = ctx.get(LocaleContext.class);
                        String generatedName = nicknameGenerator.generate(getLocaleString(localeContext));
                        return Mono.just(new JoinRequest(uniqueEmail, generatedName, password));
                    }
            );
        }
        return Mono.just(new JoinRequest(uniqueEmail, name, password));
    }

    private static String generateGuestPassword() {
        byte[] bytes = new byte[GUEST_PASSWORD_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String getLocaleString(LocaleContext localeContext) {
        Locale locale = localeContext.getLocale();
        return Objects.requireNonNullElse(locale, Locale.KOREAN).getLanguage();
    }
}
