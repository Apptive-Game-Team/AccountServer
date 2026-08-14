package com.wordonline.account.config.database;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountFlywayConfig {

    static final String ACCOUNT_URL_PROPERTY = "spring.r2dbc.account.url";
    static final String R2DBC_POSTGRESQL_SCHEME = "r2dbc:postgresql://";
    static final String MIGRATION_LOCATION = "classpath:db/migration";

    /**
     * The version of V001_20260814__baseline.sql. Flyway reads `_` as a version separator, so
     * that file is version 001.20260814, not 1; a baseline of "1" would leave it pending and
     * replay the baseline dump over the live schema. Baselining at its own version is what
     * marks it applied on an existing database.
     */
    static final String BASELINE_VERSION = "001.20260814";

    private static final String R2DBC_SCHEME = "r2dbc:";
    private static final String JDBC_SCHEME = "jdbc:";

    @Value("${spring.r2dbc.account.url}")
    private String url;
    @Value("${spring.r2dbc.account.username}")
    private String username;
    @Value("${spring.r2dbc.account.password}")
    private String password;

    /**
     * Declaring a Flyway bean makes Spring Boot's FlywayAutoConfiguration back off, so no
     * FlywayMigrationInitializer is registered and nothing would call migrate() on its own.
     * initMethod restores that: the container runs migrate() while initialising this bean.
     */
    @Bean(name = "accountFlyway", initMethod = "migrate")
    public Flyway accountFlyway() {
        return Flyway.configure()
                .dataSource(toJdbcUrl(url), username, password)
                .locations(MIGRATION_LOCATION)
                .baselineOnMigrate(true)
                .baselineVersion(BASELINE_VERSION)
                .load();
    }

    /**
     * The account server configures only an R2DBC URL, and every deployed .env lives in a
     * separate repository. Rewriting the scheme keeps Flyway on the existing properties
     * instead of requiring a new JDBC URL variable in every environment.
     */
    static String toJdbcUrl(String r2dbcUrl) {
        if (r2dbcUrl == null || !r2dbcUrl.startsWith(R2DBC_POSTGRESQL_SCHEME)) {
            throw new IllegalStateException(
                    ACCOUNT_URL_PROPERTY + " must start with \"" + R2DBC_POSTGRESQL_SCHEME
                            + "\" so Flyway can derive a JDBC URL, but its scheme was \""
                            + schemeOf(r2dbcUrl) + "\"");
        }
        return JDBC_SCHEME + r2dbcUrl.substring(R2DBC_SCHEME.length());
    }

    private static String schemeOf(String r2dbcUrl) {
        if (r2dbcUrl == null) {
            return "<null>";
        }
        int schemeEnd = r2dbcUrl.indexOf("://");
        return schemeEnd < 0 ? r2dbcUrl : r2dbcUrl.substring(0, schemeEnd + "://".length());
    }
}
