package com.wordonline.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

@Configuration
public class WebFluxConfig {

    @Bean
    public AcceptHeaderLocaleContextResolver localeContextResolver() {
        return new AcceptHeaderLocaleContextResolver();
    }
}
