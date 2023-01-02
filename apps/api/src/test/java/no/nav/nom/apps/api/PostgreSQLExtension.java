package no.nav.nom.apps.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.delegate.AbstractDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.exception.ConnectionCreationException;
import org.testcontainers.ext.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

@Slf4j
public class PostgreSQLExtension implements BeforeAllCallback, AfterAllCallback {

    private PostgreSQLContainer<?> postgreSQLContainer;

    @Override
    public void beforeAll(ExtensionContext context) {
        postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")
            .waitingFor(new TestContainerPostgresSQLWaitStrategy());
        postgreSQLContainer.start();
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // do nothing, Testcontainers handles container shutdown
    }

    class TestContainerPostgresSQLWaitStrategy extends AbstractWaitStrategy {
        private static final String SELECT_VERSION_QUERY = "select 666";
        private static final String TIMEOUT_ERROR = "Timed out waiting for PostgresSQL to be accessible for query execution";

        @Override
        protected void waitUntilReady() {
            // execute select version query until success or timeout
            try {
                retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                    getRateLimiter().doWhenReady(() -> {
                        try (DatabaseDelegate databaseDelegate = getDatabaseDelegate()) {
                            databaseDelegate.execute(SELECT_VERSION_QUERY, "", 1, false, false);
                        }
                    });
                    return true;
                });
            } catch (Exception e) {
                throw new ContainerLaunchException(TIMEOUT_ERROR);
            }
        }

        private DatabaseDelegate getDatabaseDelegate() {
            return new TestContainerPostgresSQLDelegate(waitStrategyTarget);
        }
    }
    class TestContainerPostgresSQLDelegate extends AbstractDatabaseDelegate<Connection> {

        private final ContainerState container;

        TestContainerPostgresSQLDelegate(ContainerState container) {
            this.container = container;
        }

        @Override
        protected Connection createNewConnection() {
            try {
                Properties connectionProps = new Properties();
                connectionProps.put("user", "test");
                connectionProps.put("password", "test");
                return DriverManager.getConnection(
                    String.format("jdbc:postgresql://localhost:%s/test", container.getFirstMappedPort()),
                    connectionProps);
            } catch (Exception e) {
                log.error("Could not obtain PostgresSQL connection");
                throw new ConnectionCreationException("Could not obtain PostgresSQL connection", e);
            }
        }

        @Override
        public void execute(String statement, String scriptPath, int lineNumber, boolean continueOnError, boolean ignoreFailedDrops) {
            try {
                ResultSet result = getConnection().prepareStatement(statement).executeQuery();
                result.next();
                if (result.getObject(1, Integer.class).equals(666)) {
                    log.debug("Statement {} was applied", statement);
                } else {
                    throw new ScriptUtils.ScriptStatementFailedException(statement, lineNumber, scriptPath);
                }
            } catch (Exception e) {
                throw new ScriptUtils.ScriptStatementFailedException(statement, lineNumber, scriptPath, e);
            }
        }

        @Override
        protected void closeConnectionQuietly(Connection connection) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Could not close PostgresSQL connection", e);
            }
        }
    }
}