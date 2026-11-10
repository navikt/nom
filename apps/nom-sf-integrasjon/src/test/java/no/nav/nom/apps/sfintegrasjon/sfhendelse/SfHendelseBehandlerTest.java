package no.nav.nom.apps.sfintegrasjon.sfhendelse;

import no.nav.nom.apps.sfintegrasjon.orgenhet.NomApiClient;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.NyRessurs;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.OrgtilknytningerEndring;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.VisningsnavnEndring;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.RessursHendelse;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({SfHendelseBehandlerTest.ContainerConfig.class, SfHendelseBehandler.class})
public class SfHendelseBehandlerTest {

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

    @MockitoBean
    private NomApiClient nomApiClient;

    @MockitoBean
    private RessursStateRepository ressursStateRepository;

    @MockitoBean
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @MockitoBean
    private SfHendelseResolver hendelseResolver;

    @Autowired
    private SfHendelseBehandler hendelseBehandler;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void init() {
        when(hendelseResolver.fornavn()).thenReturn("");
        when(hendelseResolver.fornavn()).thenReturn("");
        when(hendelseResolver.etternavn()).thenReturn("");
        when(hendelseResolver.dolOt()).thenReturn("");
    }


    @Test
    public void nyRessursOgOtEndringerForIkkeEksisterendeRessurs() {
        var ressursHendelse = RessursHendelse.builder().navident("A000001").data("").build();

        when(hendelseResolver.from(anyString())).thenReturn(hendelseResolver);
        when(hendelseResolver.tilknytningtype()).thenReturn("FAST");
        when(hendelseResolver.sfStillinger()).thenReturn(List.of(
            Stilling.builder().stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true)).gyldigFom(LocalDate.now()).build()
        ));

        var endringer = hendelseBehandler.utledEndringer(ressursHendelse);

        assertEquals(2, endringer.size());
        assertThat(endringer.getFirst()).isInstanceOf(NyRessurs.class);
        assertThat(endringer.getLast()).isInstanceOf(OrgtilknytningerEndring.class);
        assertEquals(1, ((OrgtilknytningerEndring) endringer.getLast()).nye().size());
    }

    @Test
    public void kunOtEndringerForEksisterendeRessurs() {
        var ressursHendelse = RessursHendelse.builder().navident("A000001").data("").build();

        when(ressursStateRepository.findByNavident(ressursHendelse.getNavident())).thenReturn(Optional.of(RessursState.builder().visningsnavn(new Visningsnavn("", Optional.empty(), "")).build()));
        when(hendelseResolver.from(anyString())).thenReturn(hendelseResolver);
        when(hendelseResolver.tilknytningtype()).thenReturn("FAST");
        when(hendelseResolver.visningsnavn()).thenReturn(" ");
        when(hendelseResolver.sfStillinger()).thenReturn(List.of(
            Stilling.builder()
                .stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true))
                .gyldigFom(LocalDate.now())
                .build()
        ));

        var endringer = hendelseBehandler.utledEndringer(ressursHendelse);

        assertEquals(1, endringer.size());
        endringer.forEach(endring -> assertThat(endring).isInstanceOf(OrgtilknytningerEndring.class));
    }

    @Test
    public void visningsnavnOgOtEndringer() {
        var ressursHendelse = RessursHendelse.builder().navident("A000001").data("").build();

        when(ressursStateRepository.findByNavident(ressursHendelse.getNavident())).thenReturn(Optional.of(RessursState.builder().visningsnavn(new Visningsnavn("", Optional.empty(), "")).build()));
        when(hendelseResolver.from(anyString())).thenReturn(hendelseResolver);
        when(hendelseResolver.tilknytningtype()).thenReturn("FAST");
        when(hendelseResolver.visningsnavn()).thenReturn("");
        when(hendelseResolver.sfStillinger()).thenReturn(List.of(
            Stilling.builder()
                .stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true))
                .gyldigFom(LocalDate.now())
                .build()
        ));

        var endringer = hendelseBehandler.utledEndringer(ressursHendelse);

        assertEquals(2, endringer.size());
        assertThat(endringer.getFirst()).isInstanceOf(VisningsnavnEndring.class);
        assertThat(endringer.getLast()).isInstanceOf(OrgtilknytningerEndring.class);
    }

    @Test
    public void ikkeOverlappendeOterTolkesSomSeparate() {
        var stillinger = List.of(
            Stilling.builder()
                .stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true))
                .gyldigFom(LocalDate.now())
                .gyldigTom(LocalDate.now().plusMonths(1))
                .build(),
            Stilling.builder()
                .stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true))
                .gyldigFom(LocalDate.now().plusMonths(1).plusDays(1))
                .gyldigTom(LocalDate.now().plusMonths(2))
                .build()
        );

        var otEndringer = hendelseBehandler.strukturerOgKategoriserOter("A000001", stillinger);

        assertEquals(2, otEndringer.nye().size());
    }

    @Test
    public void overlappendeOterSlaasSammen() {
        var gyldigFom = LocalDate.parse("2026-10-30");
        var stillinger = List.of(
            Stilling.builder()
                .stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true))
                .gyldigFom(LocalDate.now().plusMonths(1).minusDays(1))
                .build(),
            Stilling.builder()
                .stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true))
                .gyldigFom(gyldigFom)
                .gyldigTom(LocalDate.now().plusMonths(1))
                .build()
        );

        var otEndringer = hendelseBehandler.strukturerOgKategoriserOter("A000001", stillinger);

        assertEquals(1, otEndringer.nye().size());
        assertEquals(otEndringer.nye().getFirst().gyldigFom(), gyldigFom);
        assertThat(otEndringer.nye().getFirst().gyldigTom()).isNull();
    }

    @Test
    public void inklusiveOterIgnoreres() {
        var stillinger = List.of(
            Stilling.builder()
                .stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true))
                .gyldigFom(LocalDate.now())
                .build(),
            Stilling.builder()
                .stillingId(new Stilling.StillingId("xyz123", Tilknytningtype.FAST, true))
                .gyldigFom(LocalDate.now().plusMonths(1).minusDays(1))
                .build()
        );

        var otEndringer = hendelseBehandler.strukturerOgKategoriserOter("A000001", stillinger);

        assertEquals(1, otEndringer.nye().size());
    }
}
