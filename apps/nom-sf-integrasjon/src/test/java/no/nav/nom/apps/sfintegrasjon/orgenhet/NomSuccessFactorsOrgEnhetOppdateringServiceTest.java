package no.nav.nom.apps.sfintegrasjon.orgenhet;

import io.getunleash.Unleash;
import no.nav.nom.apps.sfintegrasjon.behandling.BehandlingService;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.Behandling;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingStatus;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelse;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelseRepository;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.OverOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.repository.SfOutboxRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NomSuccessFactorsOrgEnhetOppdateringServiceTest {

    private static final LocalDate IDAG = LocalDate.now();
    private static final String AGGREGERING_TOGGLE = "nom-sf-integrasjon.orgenhet-aggregering-service";

    @Mock OrganisasjonHendelseRepository organisasjonHendelseRepository;
    @Mock SfOutboxRepository sfOutboxRepository;
    @Mock BehandlingService behandlingService;
    @Mock NomApiClient nomApiClient;
    @Mock NomStateOrganiseringListener nomStateOrganiseringListener;
    @Mock NomStateOrgEnhetListener nomStateOrgEnhetListener;
    @Mock NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener;
    @Mock LeaderElection leaderElection;
    @Mock Unleash unleash;

    // Ekte state i minnet, slik at testene bruker nom-state sin tolkning av datoer og slettede oppheng
    private final OrgEnhetStateRepositoryImpl orgEnhetStateRepository = new OrgEnhetStateRepositoryImpl();
    private final OrganiseringStateRepositoryImpl organiseringStateRepository = new OrganiseringStateRepositoryImpl();
    private final OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository = new OrgEnhetLederStateRepositoryImpl();

    private final List<SfOutbox> lagretIOutbox = new ArrayList<>();
    private final AtomicLong idTeller = new AtomicLong();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private NomSuccessFactorsOrgEnhetOppdateringService service;

    @BeforeEach
    void setUp() {
        lenient().when(leaderElection.isLeader()).thenReturn(true);
        lenient().when(unleash.isEnabled(AGGREGERING_TOGGLE)).thenReturn(true);
        lenient().when(nomStateOrganiseringListener.hasReadToEndOnStartup()).thenReturn(true);
        lenient().when(nomStateOrgEnhetListener.hasReadToEndOnStartup()).thenReturn(true);
        lenient().when(nomStateOrgEnhetLederListener.hasReadToEndOnStartup()).thenReturn(true);
        lenient().when(nomApiClient.getKostnadsstedForOrgEnhet(anyString())).thenAnswer(invocation ->
            Optional.of(new OrgEnhetKostnadsstedMappingResponse("0123400", invocation.getArgument(0), IDAG.minusYears(1), null))
        );
        // Mockito returnerer ellers null fra save, og da ender alle enhetene som FEILET
        lenient().when(sfOutboxRepository.save(any(SfOutbox.class))).thenAnswer(invocation -> {
            SfOutbox outbox = invocation.getArgument(0);
            var lagret = outbox.toBuilder().sporingsId(idTeller.incrementAndGet()).build();
            lagretIOutbox.add(lagret);
            return lagret;
        });

        service = new NomSuccessFactorsOrgEnhetOppdateringService(
            organiseringStateRepository,
            orgEnhetLederStateRepository,
            orgEnhetStateRepository,
            organisasjonHendelseRepository,
            sfOutboxRepository,
            behandlingService,
            nomApiClient,
            nomStateOrganiseringListener,
            nomStateOrgEnhetListener,
            nomStateOrgEnhetLederListener,
            leaderElection,
            jsonMapper,
            unleash
        );
    }

    @Nested
    @DisplayName("Nivå for enheten")
    class NivaaForEnheten {

        @Test
        @DisplayName("Eget nivå brukes uendret")
        void egetNivaaBrukesUendret() {
            enhet("T", NomNivaa.LINJEENHET);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("Z", "T", IDAG.minusYears(1), null);

            assertThat(synk("Z").nivaa()).isEqualTo(NomNivaa.ARBEIDSOMRAADE);
        }

        @Test
        @DisplayName("En enhet uten eget nivå får ikke nivå fra overenheten")
        void enhetUtenEgetNivaaFaarIkkeNivaaFraOverenheten() {
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("Z", null);
            oppheng("Z", "C", IDAG.minusYears(1), null);

            assertThat(synk("Z").nivaa()).isNull();
        }

        @Test
        @DisplayName("En avsluttet enhet uten eget nivå får heller ikke nivå")
        void avsluttetEnhetUtenEgetNivaaFaarIkkeNivaa() {
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("Z", null, IDAG.minusYears(3), IDAG.minusMonths(1));
            oppheng("Z", "C", IDAG.minusYears(3), IDAG.minusMonths(1));

            assertThat(synk("Z").nivaa()).isNull();
        }
    }

    @Nested
    @DisplayName("Nivå for overenhetene")
    class NivaaForOverenhetene {

        @Test
        @DisplayName("En overenhet uten eget nivå sendes uten nivå")
        void overenhetUtenEgetNivaaSendesUtenNivaa() {
            enhet("X", NomNivaa.DRIFTSENHET);
            enhet("Y", null);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("Y", "X", IDAG.minusYears(1), null);
            oppheng("Z", "Y", IDAG.minusYears(1), null);

            assertThat(synk("Z").overOrgEnheter())
                .containsExactly(new OverOrgEnhet("Y", null, IDAG.minusYears(1), null));
        }

        @Test
        @DisplayName("En fremtidig overenhet uten eget nivå sendes uten nivå og gir ikke FEILET")
        void fremtidigOverenhetUtenEgetNivaaSendesUtenNivaa() {
            enhet("A", NomNivaa.DRIFTSENHET);
            enhet("P", NomNivaa.DRIFTSENHET);
            enhet("O", null, IDAG.plusMonths(2), null);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("O", "P", IDAG.plusMonths(2), null);
            oppheng("Z", "A", IDAG.minusYears(1), IDAG.plusMonths(2).minusDays(1));
            oppheng("Z", "O", IDAG.plusMonths(2), null);

            assertThat(synk("Z").overOrgEnheter())
                .filteredOn(overOrgEnhet -> overOrgEnhet.id().equals("O"))
                .containsExactly(new OverOrgEnhet("O", null, IDAG.plusMonths(2), null));
        }
    }

    @Nested
    @DisplayName("Listen over overenheter")
    class ListenOverOverenheter {

        @Test
        @DisplayName("Periodene kommer fra organiseringen, ikke fra overenheten")
        void periodeneKommerFraOrganiseringen() {
            enhet("C", NomNivaa.DRIFTSENHET, IDAG.minusYears(10), null);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("Z", "C", IDAG.minusYears(1), null);

            assertThat(synk("Z").overOrgEnheter())
                .containsExactly(new OverOrgEnhet("C", NomNivaa.DRIFTSENHET, IDAG.minusYears(1), null));
        }

        @Test
        @DisplayName("En flytting gir to innslag")
        void flyttingGirToInnslag() {
            enhet("B", NomNivaa.DRIFTSENHET);
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("Z", "B", IDAG.minusYears(2), IDAG.minusYears(1).minusDays(1));
            oppheng("Z", "C", IDAG.minusYears(1), null);

            assertThat(synk("Z").overOrgEnheter()).containsExactlyInAnyOrder(
                new OverOrgEnhet("B", NomNivaa.DRIFTSENHET, IDAG.minusYears(2), IDAG.minusYears(1).minusDays(1)),
                new OverOrgEnhet("C", NomNivaa.DRIFTSENHET, IDAG.minusYears(1), null)
            );
        }

        @Test
        @DisplayName("Et slettet oppheng er ikke med")
        void slettetOpphengErIkkeMed() {
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("D", NomNivaa.DRIFTSENHET);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("Z", "C", IDAG.minusYears(1), null);
            // Lik FOM og TOM betyr slettet. State beholder opphenget fordi datoen ikke er i dag.
            oppheng("Z", "D", IDAG.minusMonths(1), IDAG.minusMonths(1));

            assertThat(synk("Z").overOrgEnheter())
                .extracting(OverOrgEnhet::id)
                .containsExactly("C");
        }

        @Test
        @DisplayName("En linjeenhet får tom liste")
        void linjeenhetFaarTomListe() {
            enhet("T", null);
            enhet("Z", NomNivaa.LINJEENHET);
            oppheng("Z", "T", IDAG.minusYears(1), null);

            assertThat(synk("Z").overOrgEnheter()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Øvrig")
    class Ovrig {

        @Test
        @DisplayName("erLovnode er true når enheten bare har avsluttede underenheter")
        void erLovnodeNaarEnhetenBareHarAvsluttedeUnderenheter() {
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            enhet("W", null);
            oppheng("Z", "C", IDAG.minusYears(1), null);
            oppheng("W", "Z", IDAG.minusYears(1), IDAG.minusMonths(1));

            assertThat(synk("Z").erLovnode()).isTrue();
        }

        @Test
        @DisplayName("erLovnode er false når enheten har en aktiv underenhet")
        void erIkkeLovnodeNaarEnhetenHarAktivUnderenhet() {
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            enhet("W", null);
            oppheng("Z", "C", IDAG.minusYears(1), null);
            oppheng("W", "Z", IDAG.minusYears(1), null);

            assertThat(synk("Z").erLovnode()).isFalse();
        }

        @Test
        @DisplayName("endretAv og endringshendelser hentes fra meldingene")
        void endretAvOgEndringshendelserHentesFraMeldingene() {
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("Z", "C", IDAG.minusYears(1), null);

            kjor(
                hendelse("Z", "OrganiseringOpprettetEventV1", lesMelding("evtsrcmeldinger/organisering/OrganiseringOpprettetEventV1_1.json")),
                hendelse("Z", "OrgEnhetEndretNavnEventV1", melding("A123456")),
                hendelse("Z", "OrgEnhetEndretNavnEventV1", melding("A123456"))
            );

            assertThat(lagretIOutbox).hasSize(1);
            var outbox = lagretIOutbox.getFirst();
            assertThat(outbox.getEndretAv()).containsExactlyInAnyOrder("nom-api", "A123456");
            assertThat(outbox.getEndringshendelser()).containsExactlyInAnyOrder("OrganiseringOpprettetEventV1", "OrgEnhetEndretNavnEventV1");
        }

        @Test
        @DisplayName("Mangler kostnadssted, blir det tom streng")
        void manglendeKostnadsstedGirTomStreng() {
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("Z", "C", IDAG.minusYears(1), null);
            when(nomApiClient.getKostnadsstedForOrgEnhet("Z")).thenReturn(Optional.empty());

            assertThat(synk("Z").kostnadssted()).isEmpty();
        }

        @Test
        @DisplayName("Feiler én enhet, blir bare behandlingene for den FEILET")
        void feilIEnEnhetGirFeiletBareForDenEnheten() {
            enhet("C", NomNivaa.DRIFTSENHET);
            enhet("Z", NomNivaa.ARBEIDSOMRAADE);
            oppheng("Z", "C", IDAG.minusYears(1), null);
            // Q finnes ikke i state, så byggingen av Q feiler
            var hendelseQ = hendelse("Q");
            var hendelseZ = hendelse("Z");

            kjor(hendelseQ, hendelseZ);

            verify(behandlingService).oppdaterStatusTilFeilet(List.of(hendelseQ.getBehandlingsId()));
            verify(behandlingService, never()).oppdaterStatusTilFeilet(List.of(hendelseZ.getBehandlingsId()));
            verify(behandlingService).oppdaterStatusTilSynket(eq(List.of(hendelseZ.getBehandlingsId())), anyLong());
            assertThat(lagretIOutbox).extracting(SfOutbox::getOrgEnhetId).containsExactly("Z");
        }

        @Test
        @DisplayName("Ingenting skjer når poden ikke er leder")
        void ingentingSkjerNaarPodenIkkeErLeder() {
            when(leaderElection.isLeader()).thenReturn(false);

            service.oppdaterSuccessFactors();

            verifyNoInteractions(behandlingService, organisasjonHendelseRepository, sfOutboxRepository);
        }

        @Test
        @DisplayName("Ingenting skjer når state ikke er lest inn")
        void ingentingSkjerNaarStateIkkeErLestInn() {
            when(nomStateOrganiseringListener.hasReadToEndOnStartup()).thenReturn(false);

            service.oppdaterSuccessFactors();

            verifyNoInteractions(behandlingService, organisasjonHendelseRepository, sfOutboxRepository);
        }

        @Test
        @DisplayName("Ingenting skjer når togglen er av")
        void ingentingSkjerNaarTogglenErAv() {
            when(unleash.isEnabled(AGGREGERING_TOGGLE)).thenReturn(false);

            service.oppdaterSuccessFactors();

            verifyNoInteractions(behandlingService, organisasjonHendelseRepository, sfOutboxRepository);
        }
    }

    private SfOrgEnhet synk(String orgEnhetId) {
        kjor(hendelse(orgEnhetId));
        assertThat(lagretIOutbox)
            .as("Enheten skulle vært lagt i outbox. Se loggen for feilen som ga FEILET.")
            .hasSize(1);
        return jsonMapper.readValue(lagretIOutbox.getFirst().getOrgenhet(), SfOrgEnhet.class);
    }

    private void kjor(OrganisasjonHendelse... hendelser) {
        var behandlinger = Arrays.stream(hendelser)
            .map(hendelse -> Behandling.builder()
                .behandlingsId(hendelse.getBehandlingsId())
                .status(BehandlingStatus.MOTTATT)
                .behandlingTidspunkt(LocalDateTime.now())
                .build())
            .toList();
        when(behandlingService.getLatestNonProcessedBehandlingObjects()).thenReturn(behandlinger);
        when(organisasjonHendelseRepository.findAllGroupedByOrgEnhetId(anyList()))
            .thenReturn(Arrays.stream(hendelser).collect(Collectors.groupingBy(OrganisasjonHendelse::getOrgEnhetId)));

        service.oppdaterSuccessFactors();
    }

    private void enhet(String id, NomNivaa nomNivaa) {
        enhet(id, nomNivaa, IDAG.minusYears(10), null);
    }

    private void enhet(String id, NomNivaa nomNivaa, LocalDate gyldigFom, LocalDate gyldigTom) {
        orgEnhetStateRepository.update(OrgEnhetState.builder()
            .fid("orgEnhet-" + id)
            .id(id)
            .navn("Enhet " + id)
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .nomNivaa(nomNivaa)
            .build());
    }

    private void oppheng(String id, String idOver, LocalDate gyldigFom, LocalDate gyldigTom) {
        organiseringStateRepository.update(new OrganiseringState(
            "organisering-" + idTeller.incrementAndGet(),
            id,
            "Enhet " + id,
            idOver,
            "Enhet " + idOver,
            gyldigFom,
            gyldigTom
        ));
    }

    private OrganisasjonHendelse hendelse(String orgEnhetId) {
        return hendelse(orgEnhetId, "OrgEnhetEndretNavnEventV1", melding("A123456"));
    }

    private OrganisasjonHendelse hendelse(String orgEnhetId, String eventType, String melding) {
        var id = idTeller.incrementAndGet();
        return OrganisasjonHendelse.builder()
            .id(id)
            .mottattTidspunkt(OffsetDateTime.now())
            .fid("fid-" + id)
            .eventKey("event-" + id)
            .orgEnhetId(orgEnhetId)
            .eventType(eventType)
            .melding(melding)
            .behandlingsId(id)
            .build();
    }

    private static String melding(String initiatedBy) {
        return """
            {"metadata":{"inputMetadata":{"origination":{"initiatedBy":"%s","initiatedFrom":"test"}}}}
            """.formatted(initiatedBy).strip();
    }

    private static String lesMelding(String sti) {
        try (var is = NomSuccessFactorsOrgEnhetOppdateringServiceTest.class.getClassLoader().getResourceAsStream(sti)) {
            return new String(Objects.requireNonNull(is, "Fant ikke: " + sti).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
