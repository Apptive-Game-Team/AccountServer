package com.wordonline.account.controller;

import java.time.Duration;

import com.wordonline.account.domain.IssuedTokens;
import com.wordonline.account.dto.LoginRequest;
import com.wordonline.account.dto.TokenDelivery;
import com.wordonline.account.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class PageControllerTest {
    private static final String COOKIE = "accountAdminRefreshToken";

    @Test
    void refreshAndLogoutUseTheConsoleCookieAndLeaveTheWebsiteCookieAlone() {
        var authentication = mock(AuthenticationService.class);
        when(authentication.login(new LoginRequest("manager@example.com", "password"), TokenDelivery.COOKIE))
                .thenReturn(Mono.just(new IssuedTokens("access", "refresh", 3600)));
        when(authentication.refresh("refresh", TokenDelivery.COOKIE))
                .thenReturn(Mono.just(new IssuedTokens("new-access", "new-refresh", 3600)));
        when(authentication.logout("new-refresh")).thenReturn(Mono.empty());
        var controller = new PageController(authentication);
        var client = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler()).build();

        var login = MockServerWebExchange.from(MockServerHttpRequest.post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("email=manager%40example.com&password=password"));
        assertThat(controller.handleLogin(login).block()).isEqualTo("redirect:/admin");
        var refreshCookie = login.getResponse().getCookies().getFirst(COOKIE);
        assertThat(refreshCookie.getValue()).isEqualTo("refresh");
        assertThat(refreshCookie.getPath()).isEqualTo("/login/session");
        assertThat(refreshCookie.getMaxAge()).isEqualTo(Duration.ofDays(60));
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.isSecure()).isTrue();
        assertThat(refreshCookie.getSameSite()).isEqualTo("Strict");
        assertThat(login.getResponse().getCookies()).doesNotContainKey("refreshToken");
        client.post().uri("/login/session/refresh").header("X-Console-Session", "1")
                .cookie("refreshToken", "website-refresh").exchange().expectStatus().isUnauthorized();
        client.post().uri("/login/session/refresh").header("X-Console-Session", "1")
                .cookie(COOKIE, "refresh").exchange().expectStatus().isNoContent()
                .expectCookie().valueEquals("accessToken", "new-access")
                .expectCookie().valueEquals(COOKIE, "new-refresh");
        client.post().uri("/login/session/logout").header("X-Console-Session", "1")
                .cookie(COOKIE, "new-refresh").exchange().expectStatus().isNoContent()
                .expectCookie().maxAge(COOKIE, Duration.ZERO)
                .expectCookie().maxAge("accessToken", Duration.ZERO)
                .expectCookie().doesNotExist("refreshToken");
        verify(authentication).logout("new-refresh");
    }

    @Test
    void loginRendersTheThemeAndOnlyRestoresWhenAppropriate() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        var engine = new SpringWebFluxTemplateEngine();
        engine.setTemplateResolver(resolver);
        var thymeleaf = new ThymeleafReactiveViewResolver();
        thymeleaf.setTemplateEngine(engine);
        try (var context = new StaticApplicationContext()) {
            context.refresh();
            thymeleaf.setApplicationContext(context);
            var client = WebTestClient.bindToController(new PageController(mock(AuthenticationService.class)))
                    .viewResolvers(registry -> registry.viewResolver(thymeleaf)).build();

            client.get().uri("/login").exchange().expectStatus().isOk()
                    .expectBody(String.class).value(body -> assertThat(body)
                            .contains("href=\"/css/theevilent-theme.css\"",
                                    "data-session-refresh=\"/login/session/refresh\""));
            client.get().uri("/login?signedOut=true").exchange().expectStatus().isOk()
                    .expectBody(String.class).value(body -> assertThat(body).doesNotContain("data-session-refresh="));
        }
    }
}

