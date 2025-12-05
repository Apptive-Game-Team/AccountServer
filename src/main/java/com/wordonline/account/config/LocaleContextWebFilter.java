package com.wordonline.account.config;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

import reactor.core.publisher.Mono;

@Component
public class LocaleContextWebFilter implements WebFilter {

    private final LocaleContextResolver localeResolver = new AcceptHeaderLocaleContextResolver();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        LocaleContext localeContext = localeResolver.resolveLocaleContext(exchange);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(LocaleContext.class, localeContext));
    }
}