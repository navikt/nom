package no.nav.nom.apps.sfintegrasjon.outbox;

import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.Leder;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.OverOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OutboxJsonMigrationTest {

    private static final String MIGRATION_RESOURCE = "db/migration/V6_0__rename_outbox_json_fields.sql";
    private static final String CONFLICT_MESSAGE = "Motstridende feltverdier i outbox.orgenhet. Migreringen er avbrutt.";
    private static final String INSERT_FIXTURE = """
        INSERT INTO outbox (
            sf_status, feilmelding, orgenhet_id, endret_av, endringshendelser, orgenhet
        )
        VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb))
        """;
    private static final String OLD_ORGENHET = """
        {
          "id": "aa0001",
          "navn": "Seksjon for test",
          "nivaa": "ARBEIDSOMRAADE",
          "erLovnode": true,
          "type": "NAV_KONTOR",
          "tilgangsenhet": "cc0003",
          "kostnadssted": "0123400",
          "gyldigFom": "2024-01-01",
          "gyldigTom": null,
          "ledere": [
            {"navident": "TEST001", "gyldigFom": "2024-02-01", "gyldigTom": null}
          ],
          "overOrgEnheter": [
            {
              "id": "bb0002",
              "nivaa": "LINJEENHET",
              "gyldigFom": "2023-01-01",
              "gyldigTom": "2023-12-31"
            },
            {
              "id": "dd0004",
              "nivaa": "LINJEENHET",
              "gyldigFom": "2024-01-01",
              "gyldigTom": null
            }
          ]
        }
        """;
    private static final SfOrgEnhet EXPECTED_ORGENHET = SfOrgEnhet.builder()
        .id("aa0001")
        .navn("Seksjon for test")
        .nivaa(NomNivaa.ARBEIDSOMRAADE)
        .lovnode(true)
        .type(OrgEnhetsType.NAV_KONTOR)
        .tilgangsenhet("cc0003")
        .kostnadssted("0123400")
        .gyldigFom(LocalDate.of(2024, 1, 1))
        .gyldigTom(null)
        .ledere(List.of(new Leder("TEST001", LocalDate.of(2024, 2, 1), null)))
        .overenheter(List.of(
            new OverOrgEnhet("bb0002", NomNivaa.LINJEENHET, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
            new OverOrgEnhet("dd0004", NomNivaa.LINJEENHET, LocalDate.of(2024, 1, 1), null)
        ))
        .build();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    @BeforeAll
    static void startPostgres() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @BeforeEach
    void migrateSchemaThroughVersionFive() {
        var flyway = newFlyway("5.0");
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void migrererPayloadsForAlleStatuserOgBevarerMetadata() throws Exception {
        for (var status : SfStatus.values()) {
            insertFixture(status, OLD_ORGENHET);
        }
        var originalRows = rows();

        newFlyway(null).migrate();

        var migratedRows = rows();
        assertThat(migratedRows).hasSize(SfStatus.values().length);
        for (int index = 0; index < migratedRows.size(); index++) {
            var original = originalRows.get(index);
            var migrated = migratedRows.get(index);
            assertThat(original.sfStatus()).isEqualTo(SfStatus.values()[index].name());
            assertThat(migrated.sporingsId()).isEqualTo(original.sporingsId());
            assertThat(migrated.sfStatus()).isEqualTo(original.sfStatus());
            assertThat(migrated.feilmelding()).isEqualTo(original.feilmelding());
            assertThat(migrated.orgEnhetId()).isEqualTo(original.orgEnhetId());
            assertThat(migrated.endretAv()).isEqualTo(original.endretAv());
            assertThat(migrated.endringshendelser()).isEqualTo(original.endringshendelser());
            assertThat(JSON.readValue(migrated.orgenhet(), SfOrgEnhet.class)).isEqualTo(EXPECTED_ORGENHET);
        }
    }

    @Test
    void bevarerFalseNullTomlisterOgDelvisNyeFeltnavn() throws Exception {
        var falseAndEmptyLists = insertFixture(SfStatus.IKKE_SENDT, """
            {"erLovnode": false, "overOrgEnheter": []}
            """);
        var explicitNulls = insertFixture(SfStatus.SENDT, """
            {"erLovnode": null, "overOrgEnheter": null}
            """);
        var oldBooleanAndNewList = insertFixture(SfStatus.FEILET, """
            {"erLovnode": true, "overenheter": [{"id": "bb0002"}]}
            """);
        var newBooleanAndOldList = insertFixture(SfStatus.AVVIST, """
            {"lovnode": false, "overOrgEnheter": []}
            """);
        var onlyOldBoolean = insertFixture(SfStatus.ERSTATTET, """
            {"erLovnode": true}
            """);
        var onlyOldList = insertFixture(SfStatus.ERSTATTET, """
            {"overOrgEnheter": []}
            """);

        newFlyway(null).migrate();

        assertPayload(falseAndEmptyLists, """
            {"lovnode": false, "overenheter": []}
            """);
        assertPayload(explicitNulls, """
            {"lovnode": null, "overenheter": null}
            """);
        assertPayload(oldBooleanAndNewList, """
            {"lovnode": true, "overenheter": [{"id": "bb0002"}]}
            """);
        assertPayload(newBooleanAndOldList, """
            {"lovnode": false, "overenheter": []}
            """);
        assertPayload(onlyOldBoolean, """
            {"lovnode": true}
            """);
        assertPayload(onlyOldList, """
            {"overenheter": []}
            """);
    }

    @Test
    void larGjeldendeOgIkkeObjektPayloadStaUendret() throws Exception {
        insertFixture(SfStatus.IKKE_SENDT, """
            {"lovnode": true, "overenheter": [{"id": "aa0001"}], "other": "current"}
            """);
        insertFixture(SfStatus.SENDT, """
            {"id": "aa0001", "nested": {"erLovnode": true, "overOrgEnheter": []}}
            """);
        insertFixture(SfStatus.FEILET, """
            ["erLovnode", "overOrgEnheter", {"erLovnode": true}]
            """);
        insertFixture(SfStatus.AVVIST, "null");
        insertFixture(SfStatus.ERSTATTET, "\"erLovnode\"");
        var originalRows = rows();

        newFlyway(null).migrate();

        assertThat(rows()).isEqualTo(originalRows);
    }

    @Test
    void slaarSammenLikeGamleOgNyeFeltnavn() throws Exception {
        var rowId = insertFixture(SfStatus.IKKE_SENDT, """
            {
              "id": "aa0001",
              "erLovnode": true,
              "lovnode": true,
              "overOrgEnheter": [{"id": "bb0002", "nivaa": "LINJEENHET"}],
              "overenheter": [{"id": "bb0002", "nivaa": "LINJEENHET"}],
              "nested": {"erLovnode": false, "overOrgEnheter": []}
            }
            """);
        var nulls = insertFixture(SfStatus.SENDT, """
            {"erLovnode": null, "lovnode": null, "overOrgEnheter": null, "overenheter": null}
            """);

        newFlyway(null).migrate();

        assertPayload(rowId, """
            {
              "id": "aa0001",
              "lovnode": true,
              "overenheter": [{"id": "bb0002", "nivaa": "LINJEENHET"}],
              "nested": {"erLovnode": false, "overOrgEnheter": []}
            }
            """);
        assertPayload(nulls, """
            {"lovnode": null, "overenheter": null}
            """);
    }

    @Test
    void kjoererMigreringsSqlPaaNyttUtenEndringer() throws Exception {
        insertFixture(SfStatus.IKKE_SENDT, OLD_ORGENHET);
        insertFixture(SfStatus.SENDT, """
            {"lovnode": false, "overenheter": []}
            """);
        newFlyway(null).migrate();
        var beforeSecondRun = rows();

        executeMigrationScriptAgain();

        assertThat(rows()).isEqualTo(beforeSecondRun);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conflictingPayloads")
    void avbryterVedMotstridendeFeltnavnOgBevarerAlleRader(String scenario, String conflictingPayload) throws Exception {
        insertFixture(SfStatus.FEILET, conflictingPayload);
        insertFixture(SfStatus.IKKE_SENDT, OLD_ORGENHET);
        var originalRows = rows();

        var failure = catchThrowable(() -> newFlyway(null).migrate());

        assertThat(failure).as(scenario).isNotNull();
        assertThat(exceptionMessages(failure)).as(scenario).contains(CONFLICT_MESSAGE);
        assertThat(schemaVersion()).as(scenario).isEqualTo("5.0");
        assertThat(rows()).as(scenario).isEqualTo(originalRows);
    }

    private static Stream<Arguments> conflictingPayloads() {
        return Stream.of(
            Arguments.of("different boolean values", """
                {"erLovnode": true, "lovnode": false}
                """),
            Arguments.of("different parent lists", """
                {"overOrgEnheter": [{"id": "aa0001"}], "overenheter": [{"id": "bb0002"}]}
                """),
            Arguments.of("JSON null versus a value", """
                {"erLovnode": null, "lovnode": false}
                """)
        );
    }

    private static Flyway newFlyway(String targetVersion) {
        var configuration = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .cleanDisabled(false)
            .table("schema_version")
            .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        return configuration.load();
    }

    private static long insertFixture(SfStatus status, String orgenhet) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(INSERT_FIXTURE, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, status.name());
            statement.setString(2, "Synthetic fixture for " + status.name());
            statement.setString(3, "aa0001");
            statement.setArray(4, connection.createArrayOf("text", new String[] {"TEST001", "TEST002"}));
            statement.setArray(5, connection.createArrayOf("text", new String[] {"SyntheticEventV1", "MigrationTestV1"}));
            statement.setString(6, orgenhet);
            assertThat(statement.executeUpdate()).isEqualTo(1);
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                assertThat(generatedKeys.next()).isTrue();
                return generatedKeys.getLong(1);
            }
        }
    }

    private static List<OutboxRow> rows() throws SQLException {
        var rows = new ArrayList<OutboxRow>();
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                 SELECT sporings_id, sf_status, feilmelding, orgenhet_id, endret_av, endringshendelser, orgenhet
                 FROM outbox
                 ORDER BY sporings_id
                 """)) {
            while (resultSet.next()) {
                rows.add(new OutboxRow(
                    resultSet.getLong("sporings_id"),
                    resultSet.getString("sf_status"),
                    resultSet.getString("feilmelding"),
                    resultSet.getString("orgenhet_id"),
                    textArray(resultSet, "endret_av"),
                    textArray(resultSet, "endringshendelser"),
                    resultSet.getString("orgenhet")
                ));
            }
        }
        return List.copyOf(rows);
    }

    private static List<String> textArray(ResultSet resultSet, String column) throws SQLException {
        Array array = resultSet.getArray(column);
        if (array == null) {
            return null;
        }
        try {
            return List.copyOf(Arrays.asList((String[]) array.getArray()));
        } finally {
            array.free();
        }
    }

    private static void assertPayload(long sporingsId, String expectedPayload) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT orgenhet = CAST(? AS jsonb) FROM outbox WHERE sporings_id = ?")) {
            statement.setString(1, expectedPayload);
            statement.setLong(2, sporingsId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isTrue();
            }
        }
    }

    private static String schemaVersion() throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                 SELECT version
                 FROM schema_version
                 WHERE success
                 ORDER BY installed_rank DESC
                 LIMIT 1
                 """)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private static String exceptionMessages(Throwable failure) {
        var messages = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append('\n');
        }
        return messages.toString();
    }

    private static void executeMigrationScriptAgain() throws SQLException, IOException {
        String migrationScript;
        try (InputStream input = OutboxJsonMigrationTest.class.getClassLoader().getResourceAsStream(MIGRATION_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing migration resource " + MIGRATION_RESOURCE);
            }
            migrationScript = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute(migrationScript);
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private record OutboxRow(
        long sporingsId,
        String sfStatus,
        String feilmelding,
        String orgEnhetId,
        List<String> endretAv,
        List<String> endringshendelser,
        String orgenhet
    ) {
    }
}
