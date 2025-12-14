package com.wordonline.account.config.database;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

@Configuration
@EnableR2dbcRepositories(
    basePackages = "com.wordonline.account.repository",
    entityOperationsRef = "accountEntityTemplate"
)
public class AccountDbConfig {

    @Value("${spring.r2dbc.account.url}")
    private String url;
    @Value("${spring.r2dbc.account.username}")
    private String username;
    @Value("${spring.r2dbc.account.password}")
    private String password;

    @Bean(name = "accountConnectionFactory")
    public ConnectionFactory accountConnectionFactory() {
        ConnectionFactoryOptions baseOptions = ConnectionFactoryOptions.parse(url);

        ConnectionFactoryOptions options = baseOptions.mutate()
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                .build();

        return ConnectionFactories.get(options);
    }
}