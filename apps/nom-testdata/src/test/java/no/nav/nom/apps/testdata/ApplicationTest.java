package no.nav.nom.apps.testdata;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
    "unleash.enabled=false",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.invalid",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://issuer.invalid/jwks",
    "spring.security.oauth2.resourceserver.jwt.audiences=test-client",
    "nom.security.admin-group-id=test-admin",
    "nom.security.pre-authorized-apps=[{\"clientId\":\"test-caller\",\"name\":\"test\"}]"
})
class ApplicationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private final JdbcTemplate jdbcTemplate;
    private final WebApplicationContext webApplicationContext;

    @Autowired
    ApplicationTest(JdbcTemplate jdbcTemplate,
                    WebApplicationContext webApplicationContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.webApplicationContext = webApplicationContext;
    }

    @Test
    void readinessIncludesHealthyDatabaseWithoutKafkaBroker() {
        RestTestClient client = RestTestClient.bindToApplicationContext(webApplicationContext).build();

        var result = client.get().uri("/internal/health/readiness").exchange().returnResult(String.class);

        assertThat(result.getStatus().is2xxSuccessful()).isTrue();
        assertThat(result.getResponseBody()).contains("\"status\":\"UP\"");
        assertThat(jdbcTemplate.queryForObject("select 1", Integer.class)).isEqualTo(1);
    }
}
