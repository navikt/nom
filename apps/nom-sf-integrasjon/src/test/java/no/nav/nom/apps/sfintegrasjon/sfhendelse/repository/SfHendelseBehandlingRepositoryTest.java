package no.nav.nom.apps.sfintegrasjon.sfhendelse.repository;

import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingRepository;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.VisningsnavnEndring;
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

import javax.sql.DataSource;
import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({SfHendelseBehandlingRepositoryTest.ContainerConfig.class})
public class SfHendelseBehandlingRepositoryTest {

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
    private SfHendelseBehandlingRepository hendelseBehandlingRepository;

    @Autowired
    private BehandlingRepository behandlingRepository;

    @Test
    public void henterHendelseBehandlingerMedIder() {
        var hendelseBehandling = SfHendelseBehandling.builder()
            .navident("A000001")
            .behandlingsId(behandlingRepository.getNextBehandlingsId())
            .endring(List.of(new VisningsnavnEndring("", "")))
            .build();

        hendelseBehandlingRepository.save(hendelseBehandling);
        hendelseBehandlingRepository.getSfHendelseBehandlingsByIdIsIn(List.of(hendelseBehandling.getBehandlingsId()));
    }
}
