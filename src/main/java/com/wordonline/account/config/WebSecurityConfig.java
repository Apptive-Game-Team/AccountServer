package com.wordonline.account.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class WebSecurityConfig {

    private final CookieAuthenticationFilter cookieAuthenticationFilter;

    @Value("${jwt.access-public-key}")
    private RSAPublicKey rsaPublicKey;

    @Value("${jwt.access-private-key}")
    private RSAPrivateKey rsaPrivateKey;

    @Bean
    public ReactiveAuthenticationManager authenticationManager(ReactiveJwtDecoder jwtDecoder) {
        return new JwtReactiveAuthenticationManager(jwtDecoder);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.rsaPublicKey).privateKey(this.rsaPrivateKey).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withPublicKey(this.rsaPublicKey).build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverterAdapter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String scope = jwt.getClaimAsString("scope");
            if (scope == null || scope.trim().isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.stream(scope.split(" "))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter
    ) {
        http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/api/members/me",
                                "/api/members",
                                "/api/members/login",
                                "/login",
                                "/join").permitAll()
//                        .pathMatchers("/admin/**").hasRole("ADMIN")
//                        .pathMatchers("/my-page/**").hasAuthority("ROLE_USER")

                        .anyExchange().authenticated()
                );

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((exchange, e) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(URI.create("/login"));
                    return exchange.getResponse().setComplete();
                })
        );

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        http.logout(ServerHttpSecurity.LogoutSpec::disable);

        // Add cookie filter before authentication
        http.addFilterBefore(cookieAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .jwtDecoder(jwtDecoder)
                .jwtAuthenticationConverter(jwtAuthenticationConverter)
        ));
        http.csrf(CsrfSpec::disable);
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}