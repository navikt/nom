package no.nav.nom.apps.testdata;

import no.nav.nom.apps.testdata.common.repository.Audit;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetEvtsrcMsg;
import no.nav.nom.apps.testdata.orgenhet.OrgenhetState;
import no.nav.nom.apps.testdata.orgenhet.repository.OrgenhetEvtsrcMsgRepository;
import no.nav.nom.apps.testdata.orgenhet.repository.OrgenhetStateRepository;
import no.nav.nom.apps.testdata.testsett.TestSett;
import no.nav.nom.apps.testdata.testsett.repository.TestSettRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class AdditionalMigrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Test
    void additiveMigrationPreservesExistingRowsAndForeignKeys() {
        var dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var jdbcClient = JdbcClient.create(dataSource);

        var firstMigration = Flyway.configure()
            .dataSource(dataSource)
            .target("1")
            .load();
        assertThat(firstMigration.migrate().migrationsExecuted).isEqualTo(1);

        var testSettRepository = new TestSettRepository(jdbcClient);
        var stateRepository = new OrgenhetStateRepository(jdbcClient);
        var msgRepository = new OrgenhetEvtsrcMsgRepository(jdbcClient);
        var audit = new Audit.Endring(LocalDateTime.of(2026, 1, 1, 12, 0), "synthetic-editor");

        assertThat(countAdditionalTables(jdbcClient)).isZero();

        var originalTestSett = testSettRepository.create(
            new TestSett.Data("synthetic additive migration set", "synthetic description"), audit);
        var originalState = stateRepository.create(
            originalTestSett.id(), new OrgenhetState.Data(
                0, LocalDateTime.of(2026, 1, 2, 12, 0), "synthetic-fid", "synthetic-org-unit",
                "Synthetic unit", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                OrgEnhetsType.DIREKTORAT, NomNivaa.LINJEENHET, "synthetic-unique-agresso",
                "synthetic-agresso", "synthetic-level", "synthetic-type", "synthetic-access", "synthetic-cost"
            ), audit
        );
        String rawMessage = "\n  {\n not-json \n}  \n";
        var originalMessage = msgRepository.create(
            originalTestSett.id(), new OrgenhetEvtsrcMsg.Data(
                originalState.id(), 0, LocalDateTime.of(2026, 1, 3, 12, 0),
                "synthetic-object", "synthetic-key", rawMessage
            ), audit
        );
        var originalForeignKeys = existingForeignKeys(jdbcClient);

        assertThat(originalMessage.data().evtsrcMessage()).isEqualTo(rawMessage);

        var secondMigration = Flyway.configure().dataSource(dataSource).load();
        assertThat(secondMigration.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbcClient.sql(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")
            .query(String.class).single()).isEqualTo("2");
        secondMigration.validate();

        assertThat(countAdditionalTables(jdbcClient)).isEqualTo(8);
        assertThat(jdbcClient.sql(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'")
            .query(Integer.class).single()).isEqualTo(11);
        assertThat(existingForeignKeys(jdbcClient)).isEqualTo(originalForeignKeys);
        assertThat(testSettRepository.findById(originalTestSett.id())).contains(originalTestSett);
        assertThat(stateRepository.findById(originalTestSett.id(), originalState.id())).contains(originalState);
        assertThat(msgRepository.findById(originalTestSett.id(), originalMessage.id())).contains(originalMessage);
    }

    private static int countAdditionalTables(JdbcClient jdbcClient) {
        return jdbcClient.sql(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name IN ('ressurs_state', 'ressurs_evtsrc_msg', 'organisering_state', 'organisering_evtsrc_msg', 'orgenhet_leder_state', 'orgenhet_leder_evtsrc_msg', 'orgtilknytning_state', 'orgtilknytning_evtsrc_msg')")
            .query(Integer.class).single();
    }

    private static List<String> existingForeignKeys(JdbcClient jdbcClient) {
        return jdbcClient.sql(
                "SELECT t.relname || ':' || c.conname || ':' || pg_get_constraintdef(c.oid) FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid JOIN pg_namespace n ON n.oid = t.relnamespace WHERE n.nspname = current_schema() AND c.contype = 'f' AND t.relname IN ('test_sett', 'orgenhet_state', 'orgenhet_evtsrc_msg') ORDER BY t.relname, c.conname")
            .query(String.class).list();
    }
}
