package com.wordonline.account.config.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AccountFlywayConfigTest {

    // localhost:1 is never listening, so migrate() fails the moment it opens a connection.
    private static final String UNREACHABLE_ACCOUNT_URL = "r2dbc:postgresql://localhost:1/account";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AccountFlywayConfig.class)
            .withPropertyValues(
                    "spring.r2dbc.account.username=account",
                    "spring.r2dbc.account.password=account");

    @Test
    void rewritesR2dbcSchemeToJdbc() {
        assertThat(AccountFlywayConfig.toJdbcUrl("r2dbc:postgresql://db.internal:5432/account"))
                .isEqualTo("jdbc:postgresql://db.internal:5432/account");
    }

    @Test
    void rejectsUrlThatIsNotR2dbcPostgresql() {
        assertThatThrownBy(() -> AccountFlywayConfig.toJdbcUrl("jdbc:postgresql://db.internal:5432/account"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(AccountFlywayConfig.ACCOUNT_URL_PROPERTY)
                .hasMessageContaining("jdbc:postgresql://");
    }

    @Test
    void rejectsNullUrl() {
        assertThatThrownBy(() -> AccountFlywayConfig.toJdbcUrl(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(AccountFlywayConfig.ACCOUNT_URL_PROPERTY);
    }

    @Test
    void failsStartupWhenAccountUrlIsNotR2dbcPostgresql() {
        contextRunner
                .withPropertyValues("spring.r2dbc.account.url=jdbc:postgresql://localhost:5432/account")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .hasStackTraceContaining(AccountFlywayConfig.ACCOUNT_URL_PROPERTY));
    }

    /**
     * Defining a Flyway bean disables FlywayAutoConfiguration, so nothing else would call
     * migrate(). Flyway only opens a connection from migrate(), never from load(): a connection
     * failure during refresh is therefore proof that startup really runs the migration.
     */
    @Test
    void runsMigrateWhileStartingUp() {
        contextRunner
                .withPropertyValues("spring.r2dbc.account.url=" + UNREACHABLE_ACCOUNT_URL)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(causeChainOf(context.getStartupFailure()))
                            .anyMatch(FlywayException.class::isInstance);
                    assertThat(context).getFailure()
                            .hasStackTraceContaining("Unable to obtain connection from database");
                });
    }

    /**
     * baselineOnMigrate only protects the live database if the baseline version is at least the
     * version of V001. Flyway reads `001_20260814` as version 001.20260814, not 1.
     */
    @Test
    void baselineVersionMarksTheBaselineMigrationAsApplied() {
        MigrationVersion baseline = MigrationVersion.fromVersion(AccountFlywayConfig.BASELINE_VERSION);
        MigrationVersion baselineMigration = MigrationVersion.fromVersion("001_20260814");

        assertThat(baselineMigration.compareTo(baseline)).isLessThanOrEqualTo(0);
    }

    private static List<Throwable> causeChainOf(Throwable failure) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            chain.add(current);
        }
        return chain;
    }
}
