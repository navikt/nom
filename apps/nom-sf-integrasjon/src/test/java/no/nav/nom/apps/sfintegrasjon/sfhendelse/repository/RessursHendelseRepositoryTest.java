package no.nav.nom.apps.sfintegrasjon.sfhendelse.repository;

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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({RessursHendelseRepositoryTest.ContainerConfig.class})
public class RessursHendelseRepositoryTest {

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
    private RessursHendelseRepository hendelseRepository;

    @Test
    public void komplettHendelseLagres() {
        var hendelse = RessursHendelse.builder()
            .sporingsId("1234")
            .endretAv("A00010001,A0002002")
            .mottattTidspunkt(LocalDateTime.now())
            .hendelseType("RessursOpprettet, OrgtilknytningEndret")
            .navident("R001001")
            .data("""
                  {"navident": "R001001"}
                  """.strip())
            .build();

        var saved = hendelseRepository.save(hendelse);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNavident()).isNotNull();
        assertThat(saved.getData().contains("navident"));
    }
}
