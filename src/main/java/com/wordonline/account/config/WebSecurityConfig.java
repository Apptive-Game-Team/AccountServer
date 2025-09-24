package com.wordonline.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/api/members",
                                "/api/members/login",
                                "/join").permitAll()
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        .pathMatchers("/my-page/**").hasAuthority("ROLE_USER")

                        .anyExchange().authenticated()
                );

        http.formLogin(form -> form.loginPage("/login"));
        http.logout(logout -> logout.logoutUrl("/logout"));

        http.csrf(CsrfSpec::disable);
        return http.build();
    }
}
