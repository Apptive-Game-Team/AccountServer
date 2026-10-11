package com.wordonline.account.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({TurnstileProperties.class, SignupRateLimitProperties.class})
public class SignupGuardConfig {

    @Bean
    Clock signupGuardClock() {
        return Clock.systemUTC();
    }
}
