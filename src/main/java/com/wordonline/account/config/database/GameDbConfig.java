package com.wordonline.account.config.database;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

@Configuration
@EnableR2dbcRepositories(
    basePackages = "com.wordonline.account.server.repository",
    entityOperationsRef = "gameEntityTemplate"
)
public class GameDbConfig {

    @Value("${spring.r2dbc.game.url}")
    private String url;
    @Value("${spring.r2dbc.game.username}")
    private String username;
    @Value("${spring.r2dbc.game.password}")
    private String password;

    @Bean(name = "gameConnectionFactory")
    public ConnectionFactory logConnectionFactory() {
        ConnectionFactoryOptions baseOptions = ConnectionFactoryOptions.parse(url);

        ConnectionFactoryOptions options = baseOptions.mutate()
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                .build();

        return ConnectionFactories.get(options);
    }

    @Bean(name = "gameEntityTemplate")
    public R2dbcEntityTemplate logEntityTemplate(@Qualifier("gameConnectionFactory") ConnectionFactory connectionFactory) {
        return new R2dbcEntityTemplate(connectionFactory);
    }
}