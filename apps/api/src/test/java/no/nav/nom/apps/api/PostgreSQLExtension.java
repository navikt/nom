package no.nav.nom.apps.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import java.util.concurrent.TimeUnit;

@Slf4j
public class PostgreSQLExtension implements BeforeAllCallback, AfterAllCallback {

    private PostgreSQLContainer<?> postgreSQLContainer;

    @Override
    public void beforeAll(ExtensionContext context) {
        postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")
            .waitingFor(new PostgresSQLReadyStrategy());
        postgreSQLContainer.start();
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // do nothing, Testcontainers handles container shutdown
    }

    class PostgresSQLReadyStrategy extends AbstractWaitStrategy {
        private static final String SELECT_VERSION_QUERY = "select 666";
        private static final String TIMEOUT_ERROR = "Timed out waiting for PostgresSQL to be accessible for query execution";

        @Override
        protected void waitUntilReady() {
            Runnable testDatabaseQuery = () -> {
                try (DatabaseDelegate databaseDelegate = getDatabaseDelegate()) {
                    databaseDelegate.execute(SELECT_VERSION_QUERY, "", 1, false, false);
                }
            };

            // execute select version query until success or timeout
            try {
                Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                    getRateLimiter().doWhenReady(testDatabaseQuery);
                    return true;
                });
            } catch (Exception e) {
                throw new ContainerLaunchException(TIMEOUT_ERROR);
            }
        }

        private DatabaseDelegate getDatabaseDelegate() {
            if (waitStrategyTarget instanceof JdbcDatabaseContainer<?> container) {
                return new JdbcDatabaseDelegate(container, "");
            }
            throw new ContainerFetchException("Container was not of type " + JdbcDatabaseContainer.class.getSimpleName());
        }
    }
}