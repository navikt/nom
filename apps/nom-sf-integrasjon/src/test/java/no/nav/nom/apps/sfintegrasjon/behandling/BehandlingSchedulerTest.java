package no.nav.nom.apps.sfintegrasjon.behandling;


import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingRepository;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingStatus;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelse;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelseRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.time.OffsetDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({BehandlingSchedulerTest.ContainerConfig.class, BehandlingScheduler.class, BehandlingService.class})
public class BehandlingSchedulerTest {

    @MockitoBean
    private LeaderElection leaderElection;

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
    private OrganisasjonHendelseRepository organisasjonHendelseRepository;

    @Autowired
    private BehandlingScheduler behandlingScheduler;

    @Test
    public void registrererMottattStatusForOrganisasjonHedelser() {
        when(leaderElection.isLeader()).thenReturn(true);
        var organisasjonHendelse = organisasjonHendelseRepository.save(
            OrganisasjonHendelse.builder()
                .fid("abc")
                .eventType("")
                .eventKey("")
                .orgEnhetId("abc")
                .melding("{\"a\": \"b\"}")
                .mottattTidspunkt(OffsetDateTime.now().minusSeconds(5))
                .build());
        behandlingScheduler.setMottattStatusForOrganisasjonsHendelser();

        var orgranisasjonHendelse = organisasjonHendelseRepository.getReferenceById(organisasjonHendelse.getId());
        assertThat(orgranisasjonHendelse.getBehandlingsId()).isNotNull();

        var mottattStatus = behandlingRepository.getBehandlingByBehandlingsId(organisasjonHendelse.getBehandlingsId())
            .stream().filter(behandling -> behandling.getStatus() == BehandlingStatus.MOTTATT).findAny();
        assertThat(mottattStatus.isPresent());

        var ubehandlede = behandlingRepository.getLatestNonProcessedBehandlinger();
        assertThat(ubehandlede.size()).isEqualTo(1);
        assertThat(ubehandlede.getFirst().getBehandlingsId()).isEqualTo(organisasjonHendelse.getBehandlingsId());
        assertThat(ubehandlede.getFirst().getOutboxSporingsId()).isNull();
    }
}
