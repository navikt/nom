package no.nav.nom.apps.sfintegrasjon.outbox.repository;

import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.Leder;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.OverOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
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
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(SfOutboxRepositoryTest.ContainerConfig.class)
class SfOutboxRepositoryTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TestConfiguration
    static class ContainerConfig {

        static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

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
    private SfOutboxRepository sfOutboxRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Alle feltene lagres og leses tilbake uendret, og ny rad får status IKKE_SENDT")
    void alleFelteneLagresOgLesesTilbake() {
        var orgenhet = SfOrgEnhet.builder()
            .id("aa0001")
            .navn("Seksjon for testing")
            .nivaa(NomNivaa.ARBEIDSOMRAADE)
            .erLovnode(true)
            .type(OrgEnhetsType.NAV_KONTOR)
            .tilgangsenhet("cc0003")
            .kostnadssted("0123400")
            .gyldigFom(LocalDate.of(2024, 1, 1))
            .gyldigTom(null)
            .ledere(List.of(new Leder("Z990002", LocalDate.of(2024, 2, 1), null)))
            .overOrgEnheter(List.of(
                new OverOrgEnhet("bb0002", NomNivaa.LINJEENHET, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                new OverOrgEnhet("dd0004", NomNivaa.LINJEENHET, LocalDate.of(2024, 1, 1), null)
            ))
            .build();
        var lagret = sfOutboxRepository.saveAndFlush(SfOutbox.builder()
            .orgEnhetId("aa0001")
            .endretAv(Set.of("Z990001", "Z990002"))
            .endringshendelser(Set.of("OrgEnhetKorrigertEventV1", "OrgEnhetLederOpprettetEventV1"))
            .orgenhet(JSON.writeValueAsString(orgenhet))
            .build());
        entityManager.clear();

        var lest = sfOutboxRepository.findById(lagret.getSporingsId()).orElseThrow();

        assertThat(lest.getSfStatus()).isEqualTo(SfStatus.IKKE_SENDT);
        assertThat(lest.getFeilmelding()).isNull();
        assertThat(lest.getOrgEnhetId()).isEqualTo("aa0001");
        assertThat(lest.getEndretAv()).containsExactlyInAnyOrder("Z990001", "Z990002");
        assertThat(lest.getEndringshendelser()).containsExactlyInAnyOrder("OrgEnhetKorrigertEventV1", "OrgEnhetLederOpprettetEventV1");
        // jsonb endrer rekkefølgen på feltene og mellomrom, så vi sammenligner verdien og ikke teksten
        assertThat(JSON.readValue(lest.getOrgenhet(), SfOrgEnhet.class)).isEqualTo(orgenhet);
    }

    @Test
    @DisplayName("Status og feilmelding oppdateres på samme rad, slik SfOutboxService gjør med toBuilder")
    void statusOgFeilmeldingOppdateresPaaSammeRad() {
        var lagret = sfOutboxRepository.saveAndFlush(rad("aa0001"));
        var sporingsId = lagret.getSporingsId();

        sfOutboxRepository.saveAndFlush(lagret.toBuilder().sfStatus(SfStatus.FEILET).feilmelding("HTTP 503").build());
        entityManager.clear();

        assertThat(sfOutboxRepository.findAll()).singleElement().satisfies(rad -> {
            assertThat(rad.getSporingsId()).isEqualTo(sporingsId);
            assertThat(rad.getSfStatus()).isEqualTo(SfStatus.FEILET);
            assertThat(rad.getFeilmelding()).isEqualTo("HTTP 503");
        });
    }

    @Test
    @DisplayName("getAllBySfStatusIn henter bare rader med de valgte statusene")
    void henterBareRaderMedValgteStatuser() {
        for (var status : SfStatus.values()) {
            sfOutboxRepository.save(rad("aa0001").toBuilder().sfStatus(status).build());
        }
        sfOutboxRepository.flush();
        entityManager.clear();

        assertThat(sfOutboxRepository.getAllBySfStatusIn(List.of(SfStatus.IKKE_SENDT, SfStatus.FEILET)))
            .extracting(SfOutbox::getSfStatus)
            .containsExactlyInAnyOrder(SfStatus.IKKE_SENDT, SfStatus.FEILET);
    }

    private static SfOutbox rad(String orgEnhetId) {
        return SfOutbox.builder()
            .orgEnhetId(orgEnhetId)
            .orgenhet("{\"id\":\"%s\"}".formatted(orgEnhetId))
            .build();
    }
}
