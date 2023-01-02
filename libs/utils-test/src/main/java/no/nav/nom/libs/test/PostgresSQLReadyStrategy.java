package no.nav.nom.libs.test;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import java.util.concurrent.TimeUnit;

public class PostgresSQLReadyStrategy extends AbstractWaitStrategy {
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