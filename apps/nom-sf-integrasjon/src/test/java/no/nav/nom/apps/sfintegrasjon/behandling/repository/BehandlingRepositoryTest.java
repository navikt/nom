package no.nav.nom.apps.sfintegrasjon.behandling.repository;

import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.repository.SfOutboxRepository;
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

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(BehandlingRepositoryTest.ContainerConfig.class)
class BehandlingRepositoryTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 12, 0);

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
    private BehandlingRepository behandlingRepository;

    @Autowired
    private SfOutboxRepository sfOutboxRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Behandlinger der siste status er MOTTATT eller FEILET, er ubehandlede")
    void ubehandledeHarSisteStatusMottattEllerFeilet() {
        status(1L, BehandlingStatus.MOTTATT, T0);

        status(2L, BehandlingStatus.MOTTATT, T0);
        status(2L, BehandlingStatus.FEILET, T0.plusMinutes(1));

        status(3L, BehandlingStatus.MOTTATT, T0);
        status(3L, BehandlingStatus.SYNKET, T0.plusMinutes(1));

        status(4L, BehandlingStatus.MOTTATT, T0);
        status(4L, BehandlingStatus.FEILET, T0.plusMinutes(1));
        status(4L, BehandlingStatus.SYNKET, T0.plusMinutes(2));

        assertThat(behandlingRepository.getLatestNonProcessedBehandlinger())
            .extracting(Behandling::getBehandlingsId, Behandling::getStatus)
            .containsExactly(tuple(1L, BehandlingStatus.MOTTATT), tuple(2L, BehandlingStatus.FEILET));
    }

    @Test
    @DisplayName("Ved likt tidspunkt vinner statusen som ble lagret sist")
    void vedLiktTidspunktVinnerSistLagredeStatus() {
        status(1L, BehandlingStatus.MOTTATT, T0);
        status(1L, BehandlingStatus.SYNKET, T0);

        status(2L, BehandlingStatus.MOTTATT, T0);
        status(2L, BehandlingStatus.FEILET, T0);

        assertThat(behandlingRepository.getLatestNonProcessedBehandlinger())
            .extracting(Behandling::getBehandlingsId, Behandling::getStatus)
            .containsExactly(tuple(2L, BehandlingStatus.FEILET));
    }

    @Test
    @DisplayName("Ubehandlede sorteres etter tidspunktet for siste status")
    void sorteresEtterTidspunktForSisteStatus() {
        status(1L, BehandlingStatus.MOTTATT, T0);
        status(1L, BehandlingStatus.FEILET, T0.plusMinutes(10));
        status(2L, BehandlingStatus.MOTTATT, T0.plusMinutes(5));

        assertThat(behandlingRepository.getLatestNonProcessedBehandlinger())
            .extracting(Behandling::getBehandlingsId)
            .containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("SYNKET-status peker på outbox-raden som ble laget")
    void synketPekerPaaOutboxRaden() {
        var outbox = sfOutboxRepository.saveAndFlush(SfOutbox.builder()
            .orgEnhetId("aa0001")
            .orgenhet("{\"id\":\"aa0001\"}")
            .build());
        status(1L, BehandlingStatus.MOTTATT, T0);
        behandlingRepository.saveAndFlush(Behandling.builder()
            .behandlingsId(1L)
            .status(BehandlingStatus.SYNKET)
            .outboxSporingsId(outbox.getSporingsId())
            .behandlingTidspunkt(T0.plusMinutes(1))
            .build());
        entityManager.clear();

        assertThat(behandlingRepository.getBehandlingByBehandlingsId(1L))
            .extracting(Behandling::getStatus, Behandling::getOutboxSporingsId)
            .containsExactlyInAnyOrder(
                tuple(BehandlingStatus.MOTTATT, null),
                tuple(BehandlingStatus.SYNKET, outbox.getSporingsId())
            );
        assertThat(behandlingRepository.getLatestNonProcessedBehandlinger()).isEmpty();
    }

    private void status(long behandlingsId, BehandlingStatus status, LocalDateTime tidspunkt) {
        behandlingRepository.saveAndFlush(Behandling.builder()
            .behandlingsId(behandlingsId)
            .status(status)
            .behandlingTidspunkt(tidspunkt)
            .build());
    }
}
