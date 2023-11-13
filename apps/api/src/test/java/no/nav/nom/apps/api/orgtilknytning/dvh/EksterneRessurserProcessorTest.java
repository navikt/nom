package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserProcessor.UNLEASH_LAG_EVENTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Tester prosessering av ekstern for endring/legge til orgtilkknytninger på last fra DVH")
public class EksterneRessurserProcessorTest extends OrgtilknytningCmdServiceTestBase {
    EksterneRessurserProcessor eksterneRessurserProcessor;
    @Mock
    Unleash unleash;
    @Mock
    RemedyEnhetMapperService remedyEnhetMapperService;
    @Mock
    OrgtilknytningService orgtilknytningService;
    @Mock
    OrgenhetService orgenhetService;

    final LocalDate yesterday = LocalDate.now().minusDays(1);
    final LocalDate today = LocalDate.now();
    final LocalDate tomorrow = LocalDate.now().plusDays(1);
    final LocalDate inTwoMonths = LocalDate.now().plusMonths(2);

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        eksterneRessurserProcessor = new EksterneRessurserProcessor(
            unleash,
            remedyEnhetMapperService,
                orgtilknytningService,
            orgenhetService,
            orgtilknytningCommandService
        );

        when(unleash.isEnabled(UNLEASH_LAG_EVENTS)).thenReturn(true);

        setupOrgenhetServiceDefaultResponses();
        setupRemedyEnhetMapperServiceDefaultResponses();
        setupOrgtilknytningStateServiceDefaultResponses();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn("FID_1");
    }

    private void setupOrgenhetServiceDefaultResponses() {
        when(orgenhetService.getById(ORGENHET_A.getId())).thenReturn(Optional.of(ORGENHET_A));
        when(orgenhetService.getByAgressoIdForEnheterMedOrganisering(ORGENHET_A.getAgressoId())).thenReturn(List.of(ORGENHET_A));
    }

    private void setupRemedyEnhetMapperServiceDefaultResponses() {
        when(remedyEnhetMapperService.findByNomId(REMEDY_ENHET_1234.nomId())).thenReturn(Optional.of(REMEDY_ENHET_1234));
        when(remedyEnhetMapperService.findByRemedyEnhetId(REMEDY_ENHET_1234.remedyEnhetId())).thenReturn(Optional.of(REMEDY_ENHET_1234));
    }

    private void setupOrgtilknytningStateServiceDefaultResponses() {

    }

    @Test
    @DisplayName("Skip ikke aktive eksterne")
    public void test_001() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "8000100", "ORGENHET", yesterday, tomorrow, false);

        eksterneRessurserProcessor.process(ekstern);

        verifyNoInteractions(orgtilknytningCommandService);
        verifyNoInteractions(remedyEnhetMapperService);
        verifyNoInteractions(orgenhetService);
    }

    @Test
    @DisplayName("Ekstern med remedy_start i går slutter i morgen, og basic oppslag på orgenhet fra orgenhet_paa_resssurs")
    public void test_002() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "8000100", "ORGENHET", yesterday, tomorrow, true);

        eksterneRessurserProcessor.process(ekstern);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(ekstern.navident())
                .tilEnhet(ORGENHET_A.getId())
                .gyldigFom(yesterday)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(tomorrow);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Ekstern med remedy_start i går slutter i morgen, og basic oppslag på orgenhet fra orgenhet_paa_resssurs, men mappet remedyEnhet er ulik")
    public void test_003() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "5678", "8000100", "ORGENHET", yesterday, tomorrow, true);

        eksterneRessurserProcessor.process(ekstern);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(ekstern.navident())
                .tilEnhet(ORGENHET_A.getId())
                .gyldigFom(yesterday)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(tomorrow);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .erDagligOppfolging(true);

        // Merk at dette ikke funksjonelt gir annet utfall - men vi får en WARN melding i loggen ala:
        //      Ekstern ressurs A000001 er i DVH angitt remedyEnhet 5678 og agressoId/Nivå 8000100/ORGENHET er i NOM matchet til i orgenhet ORGENHET_A som mapper til en annen remedy enhet 1234.
    }

    @Test
    @DisplayName("Ekstern med remedy_start i går slutter i morgen, og oppslag på orgenhet hvor DVH ikke har orgenhet_paa_ressurs, fallback til DVH remedyEnhet")
    public void test_004() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "-4", "ORGENHET", yesterday, tomorrow, true);

        eksterneRessurserProcessor.process(ekstern);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(ekstern.navident())
                .tilEnhet(ORGENHET_A.getId())
                .gyldigFom(yesterday)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(tomorrow);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName(
        "Ekstern med remedy_start i går slutter i morgen, og oppslag på orgenhet hvor DVH ikke har orgenhet_paa_ressurs agressoId vi ikke får mappet, fallback til DVH remedyEnhet"
    )
    public void test_005() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "UKJENT_AGRESSO_ID", "ORGENHET", yesterday, tomorrow, true);

        eksterneRessurserProcessor.process(ekstern);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(ekstern.navident())
                .tilEnhet(ORGENHET_A.getId())
                .gyldigFom(yesterday)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(tomorrow);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Ekstern hvor vi ikke klarer å finne orgenhet i det hele tatt")
    public void test_006() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "----", "UKJENT_AGRESSO_ID", "ORGENHET", yesterday, tomorrow, true);

        eksterneRessurserProcessor.process(ekstern);

        verifyNoInteractions(orgtilknytningCommandService);
    }

    @Test
    @DisplayName("Ekstern med eksisterende knytning hvor vi ikke klarer å finne orgenhet i det hele tatt")
    public void test_007() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "----", "UKJENT_AGRESSO_ID", "ORGENHET", yesterday, tomorrow, true);

        when(orgtilknytningService.getByNavIdent(ekstern.navident())).thenReturn(List.of(
            new OrgtilknytningState("FID_A", "A000001", "ORGENHET_A", true, yesterday, Optional.of(tomorrow))
        ));

        eksterneRessurserProcessor.process(ekstern);

        verifyNoInteractions(orgtilknytningCommandService);
    }

    @Test
    @DisplayName("Ekstern med eksisterende knytning hvor vi ikke trenger å endre noe")
    public void test_008() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "8000100", "ORGENHET", yesterday, tomorrow, true);

        when(orgtilknytningService.getByNavIdent(ekstern.navident())).thenReturn(List.of(
            new OrgtilknytningState("FID_A", "A000001", "ORGENHET_A", true, yesterday, Optional.of(tomorrow))
        ));

        eksterneRessurserProcessor.process(ekstern);

        verifyNoInteractions(orgtilknytningCommandService);
    }

    @Test
    @DisplayName("Ekstern med eksisterende knytning hvor sluttdato er endret på men orgenhet er lik")
    public void test_009() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "8000100", "ORGENHET", yesterday, tomorrow, true);

        when(orgtilknytningService.getByNavIdent(ekstern.navident())).thenReturn(List.of(
            new OrgtilknytningState("FID_A", "A000001", "ORGENHET_A", true, yesterday, Optional.of(today))
        ));

        eksterneRessurserProcessor.process(ekstern);

        assertOrgtilknytning(CMD.KORRIGERT)
            .forFid("FID_A")
            .gyldigFom(yesterday)
            .gyldigTom(tomorrow);
    }

    @Test
    @DisplayName("Ekstern med eksisterende knytning hvor orgenhet er endret, sluttdato i fremtiden")
    public void test_010() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "8000100", "ORGENHET", yesterday, tomorrow, true);

        when(orgtilknytningService.getByNavIdent(ekstern.navident())).thenReturn(List.of(
            new OrgtilknytningState("FID_A", "A000001", "ORGENHET_B", true, yesterday.minusMonths(1), Optional.of(tomorrow))
        ));

        eksterneRessurserProcessor.process(ekstern);

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid("FID_A")
            .gyldigTom(yesterday);

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(ekstern.navident())
                .tilEnhet(ORGENHET_A.getId())
                .gyldigFom(today)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(tomorrow);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Ekstern med eksisterende knytning hvor orgenhet er endret, startdato i fremtiden")
    public void test_011() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "8000100", "ORGENHET", tomorrow, inTwoMonths, true);

        when(orgtilknytningService.getByNavIdent(ekstern.navident())).thenReturn(List.of(
            new OrgtilknytningState("FID_A", "A000001", "ORGENHET_B", true, tomorrow, Optional.of(inTwoMonths))
        ));

        eksterneRessurserProcessor.process(ekstern);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_A");

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(ekstern.navident())
                .tilEnhet(ORGENHET_A.getId())
                .gyldigFom(tomorrow)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(inTwoMonths);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Ekstern med eksisterende knytning hvor orgenhet er endret, startdato i fremtiden")
    public void test_012() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "8000100", "ORGENHET", tomorrow, inTwoMonths, true);

        when(orgtilknytningService.getByNavIdent(ekstern.navident())).thenReturn(List.of(
            new OrgtilknytningState("FID_A", "A000001", "ORGENHET_B", true, tomorrow, Optional.of(tomorrow.plusDays(10)))
        ));

        eksterneRessurserProcessor.process(ekstern);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_A");

        String fid =
            assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(ekstern.navident())
                .tilEnhet(ORGENHET_A.getId())
                .gyldigFom(tomorrow)
                .getFid();

        assertOrgtilknytning(CMD.AVVIKLET)
            .forFid(fid)
            .gyldigTom(inTwoMonths);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .erDagligOppfolging(true);
    }

    @Test
    @DisplayName("Ikke lag events hvis Unleash flag ikke er satt")
    public void test_999() {
        DvhEksternRessurs ekstern =
            new DvhEksternRessurs("payload", "A000001", "1234", "8000100", "ORGENHET", yesterday, tomorrow, true);

        when(unleash.isEnabled(UNLEASH_LAG_EVENTS)).thenReturn(false);

        eksterneRessurserProcessor.process(ekstern);

        verifyNoInteractions(orgtilknytningCommandService);
    }

    static final IOrgenhet ORGENHET_A = new OrgEnhet(
        new OrgEnhetState("FID_ENHET_A", "ORGENHET_A", "ENHET A", LocalDate.of(2022, 1, 1),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("8000100-ENHET A-ORGENHET"), Optional.of("8000100"), Optional.of("ORGENHET"), Optional.empty()
        )
    );

    static final IOrgenhet ORGENHET_B = new OrgEnhet(
        new OrgEnhetState("FID_ENHET_B", "ORGENHET_B", "ENHET B", LocalDate.of(2022, 1, 1),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("1000800-ENHET B-ORGENHET"), Optional.of("1000800"), Optional.of("ORGENHET"), Optional.empty()
        )
    );

    static final RemedyEnhet REMEDY_ENHET_1234 = new RemedyEnhet("1234", "8000100", "ORGENHET", "ORGENHET_A");
    static final RemedyEnhet REMEDY_ENHET_5678 = new RemedyEnhet("5678", "1000800", "ORGENHET", "ORGENHET_B");
}