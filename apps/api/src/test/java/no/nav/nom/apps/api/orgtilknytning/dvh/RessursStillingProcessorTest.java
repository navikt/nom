package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Tester Utility metoder for bruk i tolking og generering av events for RessursStillinger fra DVH")
public class RessursStillingProcessorTest extends OrgtilknytningCmdServiceTestBase {
    private static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);

    @Mock
    OrgtilknytningService orgtilknytningService;
    @Mock
    RessursStateRepository ressursStateRepository;
    @Mock
    RessursStillingLoadStatus ressursStillingLoadStatus;
    RessursStillingProcessor ressursStillingProcessor;
    OrgenhetService orgenhetService;
    OrgEnhetStateRepository orgEnhetStateRepository;
    @Mock
    OrgEnhetCommandService orgEnhetCommandService;
    @Mock
    KafkaTopicReader kafkaTopicReader;
    @Mock
    NomMastered nomMastered;
    @Mock
    Unleash unleash;
    @Mock
    OrganiseringStateRepository organiseringStateRepository;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        orgEnhetStateRepository = new OrgEnhetStateRepository();
        orgenhetService = new OrgenhetService(orgEnhetStateRepository, orgEnhetCommandService, new DvhRepository(), kafkaTopicReader,
            unleash,
            organiseringStateRepository
        );
        ressursStillingProcessor = new RessursStillingProcessor(orgenhetService, orgtilknytningService, orgtilknytningCommandService,
            ressursStillingLoadStatus,
            ressursStateRepository,
            nomMastered,
            unleash
        );

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn("FID_1");

        // Default mock svar på nomMastered ressurs og Unleash flag
        when(nomMastered.isMastered(any())).thenReturn(false);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(false);

        settOppStandardEnheter();
    }

    @Test
    @DisplayName("Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende orgtilknytning")
    public void test_001() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.of(2023, 1, 5);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());

        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(navident)
                .tilEnhet("a001bc")
                .gyldigFom(startDate)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET, NEVER_INVOKED);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende enhet med fremtidig startdato")
    public void test_002() {
        String navident = "A000001";

        LocalDate start = LocalDate.now().plusMonths(3);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                start,                          // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen eksitsterende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());
        // Alle er sttatlig ansatte
        svarAtAlleNavidenterErStatlig(navident);

        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(navident)
                .tilEnhet("a001bc")
                .gyldigFom(start)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET, NEVER_INVOKED);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .erDagligOppfolging(true);
    }

    private void svarAtAlleNavidenterErStatlig(String navident) {
        when(ressursStateRepository.findByNavident(any())).thenReturn(Optional.of(new RessursState("FID_1",
            "FNR00000",
            navident,
            Sektor.NAV_STATLIG,
            null,
            Optional.empty(),
            Optional.empty(),
            null,
            Collections.emptyList()
        )));
    }

    @Test
    @DisplayName("Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende enhet med sluttdato")
    public void test_003() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate endDate = LocalDate.now().plusMonths(2);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                          // Gyldig Fom for den strukturerte stillingen
                endDate,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());

        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(navident)
                .tilEnhet("a001bc")
                .gyldigFom(startDate)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(endDate);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Sjekk at OrgtilknytningOpprett CMD blir kalt med ressursens sluttDato hvis orgtilknyting er open ended")
    public void test_004() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate ressursSluttDato = LocalDate.now().plusMonths(2);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());
        when(ressursStateRepository.findByNavident(any())).thenReturn(
            Optional.of(new RessursState(
                "FID_1",
                "FNR000",
                navident,
                Sektor.NAV_STATLIG,
                new Visningsnavn("Ola", Optional.empty(), "Normann"),
                Optional.of(ressursSluttDato),
                Optional.empty(),
                null,
                Collections.emptyList()
            ))
        );

        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(navident)
                .tilEnhet("a001bc")
                .gyldigFom(startDate)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET).gyldigTom(ressursSluttDato);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Sjekk at FjernOrgtilknytning CMD blir kalt på duplikat tilknytning når det har vært navnebytte på enhetsnavn")
    public void test_005() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.of(2023, 1, 1);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_3"),     // DVH stillingens enhets agressoId
                    "ENHET_3_NYTT_NAVN",        // DVH stillingens enhets navn
                    Optional.of("AGRID_3"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_4").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // To eksisterende orgtilknytninger
        OrgtilknytningState eksisterendeGammelEnhet =  orgtilknytningState().toBuilder()
            .fid("OT_FID_1")
            .navident(navident)
            .orgEnhetId("a003bc")
            .gyldigFom(startDate).build();

        OrgtilknytningState eksisterendeNyEnhet = orgtilknytningState().toBuilder()
            .fid("OT_FID_2")
            .navident(navident)
            .orgEnhetId("a004bc")
            .gyldigFom(startDate).build();

        when(orgtilknytningService.getByNavIdent(any())).thenReturn(List.of(eksisterendeGammelEnhet, eksisterendeNyEnhet));
        when(ressursStateRepository.findByNavident(any())).thenReturn(
            Optional.of(new RessursState(
                "FID_1",
                "FNR000",
                navident,
                Sektor.NAV_STATLIG,
                new Visningsnavn("Ola", Optional.empty(), "Normann"),
                Optional.empty(),
                Optional.empty(),
                null,
                Collections.emptyList()
            ))
        );
        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid = assertOrgtilknytning(CMD.FJERNET).getFid();
        assertEquals(fid, "OT_FID_1");

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid("OT_FID_2")
            .forNavident(navident)
            .tilEnhet("a004bc")
            .erDagligOppfolging(true);
    }

    /*************************************************************************
     * Tester for NomMastered og ulike settinger av unleash flag
     ************************************************************************/

    @Test
    @DisplayName("Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende orgtilknytning - når NomMasterd unleash flag er satt og navident er markert")
    public void test_101a() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.of(2023, 1, 5);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());

        //Unleash flagg satt og ident er mastret
        when(nomMastered.isMastered(any())).thenReturn(true);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(true);

        ressursStillingProcessor.process(navident, stillingPerioder);

        assertIngenEvtsrcCmdsInvokert();
    }

    @Test
    @DisplayName(
        "Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende orgtilknytning - når NomMasterd unleash flag ikke er satt og navident er markert som mastret"
    )
    public void test_101b() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.of(2023, 1, 5);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());

        //Unleash flagg ikke satt og ident er mastret
        when(nomMastered.isMastered(any())).thenReturn(true);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(false);

        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(navident)
                .tilEnhet("a001bc")
                .gyldigFom(startDate)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET, NEVER_INVOKED);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName(
        "Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende orgtilknytning - når NomMasterd unleash flag er satt og navident ikke er markert som mastret"
    )
    public void test_101c() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.of(2023, 1, 5);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());

        //Unleash flagg satt og ident ikke er mastret
        when(nomMastered.isMastered(any())).thenReturn(false);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(true);

        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .gyldigFom(startDate)
            .getFid();

        assertOrgtilknytning(CMD.AVVIKLET, NEVER_INVOKED);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende enhet med sluttdato- når NomMasterd unleash flag er satt og navident er markert")
    public void test_103a() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate endDate = LocalDate.now().plusMonths(2);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                          // Gyldig Fom for den strukturerte stillingen
                endDate,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());

        //Unleash flagg satt og ident ikke er mastret
        when(nomMastered.isMastered(any())).thenReturn(true);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(true);

        ressursStillingProcessor.process(navident, stillingPerioder);

        assertIngenEvtsrcCmdsInvokert();
    }

    @Test
    @DisplayName(
        "Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende enhet med sluttdato- når NomMasterd unleash flag ikke er satt og navident er markert"
    )
    public void test_103b() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate endDate = LocalDate.now().plusMonths(2);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                          // Gyldig Fom for den strukturerte stillingen
                endDate,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());


        //Unleash flagg ikke satt og ident er mastret
        when(nomMastered.isMastered(any())).thenReturn(true);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(false);

        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(navident)
                .tilEnhet("a001bc")
                .gyldigFom(startDate)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(endDate);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName(
        "Sjekk at OrgtilknytningOpprett CMD blir kalt for manglende enhet med sluttdato- når NomMasterd unleash flag er satt og navident ikke er markert"
    )
    public void test_103c() {
        String navident = "A000001";

        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate endDate = LocalDate.now().plusMonths(2);

        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    Optional.of("AGRID_1"),     // DVH stillingens enhets agressoId
                    "ENHET_1",                  // DVH stillingens enhets navn
                    Optional.of("AGRID_1"),     // DVH ressursens enhets agressoId
                    Optional.of("ORGENHET")
                ),   // DVH ressursens agresso orgenhets nivå
                startDate,                          // Gyldig Fom for den strukturerte stillingen
                endDate,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningService.getAll()).thenReturn(Collections.emptyList());

        //Unleash flagg satt og ident ikke er mastret
        when(nomMastered.isMastered(any())).thenReturn(false);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(true);

        ressursStillingProcessor.process(navident, stillingPerioder);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(navident)
                .tilEnhet("a001bc")
                .gyldigFom(startDate)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(endDate);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(navident)
            .tilEnhet("a001bc")
            .erDagligOppfolging(true);
    }

    private void assertIngenEvtsrcCmdsInvokert() {
        assertOrgtilknytning(CMD.OPPRETTET, NEVER_INVOKED);
        assertOrgtilknytning(CMD.AVVIKLET, NEVER_INVOKED);
        assertOrgtilknytning(CMD.FJERNET, NEVER_INVOKED);
        assertOrgtilknytning(CMD.KORRIGERT, NEVER_INVOKED);
        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING, NEVER_INVOKED);
    }

    private void settOppStandardEnheter() {
        final Optional<LocalDate> EMPTY_DATE = Optional.empty();
        orgEnhetStateRepository.clear();
        orgEnhetStateRepository.putEventAggregate(lagEnhet("FID_1", "a001bc", "ENHET_1", LocalDate.of(2020, 1, 1), EMPTY_DATE, "AGRID_1"));
        orgEnhetStateRepository.putEventAggregate(lagEnhet("FID_2", "a002bc", "ENHET_2", LocalDate.of(2020, 1, 1), EMPTY_DATE, "AGRID_2"));
        orgEnhetStateRepository.putEventAggregate(lagEnhet(
            "FID_3",
            "a003bc",
            "ENHET_3_AVSLUTTET",
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2022, 12, 31),
            "AGRID_3"
        ));
        orgEnhetStateRepository.putEventAggregate(lagEnhet(
            "FID_4",
            "a004bc",
            "ENHET_3_NYTT_NAVN",
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2022, 12, 31),
            "AGRID_3"
        ));
    }

    private OrgEnhetState lagEnhet(String fid, String id, String navn, LocalDate gyldigFom, LocalDate gyldigTom, String agressoId) {
        return lagEnhet(fid, id, navn, gyldigFom, Optional.ofNullable(gyldigTom), agressoId);
    }

    private OrgEnhetState lagEnhet(String fid, String id, String navn, LocalDate gyldigFom, Optional<LocalDate> gyldigTom, String agressoId) {
        return new OrgEnhetState(fid, id, navn, gyldigFom, gyldigTom, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(agressoId), Optional.of("ORGENHET"), Optional.empty()
        );
    }
}
