package no.nav.nom.apps.sfintegrasjon.organisasjon.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(OrganisasjonHendelseRepositoryTest.ContainerConfig.class)
class OrganisasjonHendelseRepositoryTest {

    @TestConfiguration
    static class ContainerConfig {

        @SuppressWarnings("resource")
        static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

        static {
            POSTGRES.start();
        }

        @Bean
        @Primary
        DataSource testDataSource() {
            return DataSourceBuilder.create()
                .url(POSTGRES.getJdbcUrl())
                .username(POSTGRES.getUsername())
                .password(POSTGRES.getPassword())
                .build();
        }
    }

    @Autowired
    OrganisasjonHendelseRepository repository;

    @BeforeEach
    void ryddOpp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Lagrer og henter hendelse med alle felter")
    void lagrerOgHenterHendelseMedAllefelter() {
        var hendelse = OrganisasjonHendelse.builder()
            .mottattTidspunkt(OffsetDateTime.now())
            .fid("orgenhet-fid-123")
            .eventKey("kafka-key-abc")
            .orgEnhetId("854600")
            .eventType("OrgEnhetOpprettetEventV1")
            .melding("""
                {"type":"OrgEnhetOpprettetEventV1","fid":"orgenhet-fid-123"}
                """.strip())
            .build();

        var lagret = repository.save(hendelse);

        assertThat(lagret.getId()).isNotNull();

        var hentet = repository.findById(lagret.getId()).orElseThrow();
        assertThat(hentet.getFid()).isEqualTo("orgenhet-fid-123");
        assertThat(hentet.getEventKey()).isEqualTo("kafka-key-abc");
        assertThat(hentet.getOrgEnhetId()).isEqualTo("854600");
        assertThat(hentet.getEventType()).isEqualTo("OrgEnhetOpprettetEventV1");
        assertThat(hentet.getMelding()).contains("OrgEnhetOpprettetEventV1");
        assertThat(hentet.getMottattTidspunkt()).isNotNull();
        assertThat(hentet.getBehandlingsId()).isNull();
    }

    @Test
    @DisplayName("id genereres av databasen (GENERATED ALWAYS AS IDENTITY)")
    void idGenerereAvDatabasen() {
        var h1 = lagHendelse("fid-1", "OrgEnhetOpprettetEventV1");
        var h2 = lagHendelse("fid-2", "OrgEnhetAvvikletEventV1");

        assertThat(h1.getId()).isNotNull();
        assertThat(h2.getId()).isNotNull();
        assertThat(h1.getId()).isNotEqualTo(h2.getId());
        assertThat(h2.getId()).isGreaterThan(h1.getId());
    }

    @Test
    @DisplayName("behandlings_id er null ved lagring")
    void behandlingsIdErNullVedLagring() {
        var lagret = repository.save(lagHendelseBuilder("fid-1", "OrgEnhetOpprettetEventV1").build());

        assertThat(lagret.getBehandlingsId()).isNull();
    }

    @Test
    @DisplayName("orgEnhetId er NOT NULL — null-verdi kaster DataIntegrityViolationException")
    void orgEnhetIdKanIkkeVaereNull() {
        var hendelse = lagHendelseBuilder("fid-null", "OrgEnhetOpprettetEventV1")
            .orgEnhetId(null)
            .build();

        assertThatThrownBy(() -> repository.saveAndFlush(hendelse))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("JSONB-feltet melding lagres og hentes korrekt")
    void jsonbFeltetMeldingLagresOgHentesKorrekt() {
        var jsonMelding = """
            {"type":"OrganiseringOpprettetEventV1","fid":"org-fid-99","data":{"id":"org-99","navn":"Nav Oslo"}}
            """.strip();

        var lagret = repository.save(lagHendelseBuilder("org-fid-99", "OrganiseringOpprettetEventV1")
            .melding(jsonMelding)
            .build());

        var hentet = repository.findById(lagret.getId()).orElseThrow();
        assertThat(hentet.getMelding()).isEqualTo(jsonMelding);
    }

    // --- hjelpemetoder ---

    private OrganisasjonHendelse lagHendelse(String fid, String eventType) {
        return repository.save(lagHendelseBuilder(fid, eventType).build());
    }

    private OrganisasjonHendelse.OrganisasjonHendelseBuilder lagHendelseBuilder(String fid, String eventType) {
        return OrganisasjonHendelse.builder()
            .mottattTidspunkt(OffsetDateTime.now())
            .fid(fid)
            .eventKey("key-" + fid)
            .orgEnhetId("854600")
            .eventType(eventType)
            .melding("{\"type\":\"" + eventType + "\",\"fid\":\"" + fid + "\"}");
    }
}
