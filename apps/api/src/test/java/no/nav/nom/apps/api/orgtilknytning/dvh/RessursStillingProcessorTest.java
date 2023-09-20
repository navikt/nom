package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.KorrigerOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Tester Utility metoder for bruk i tolking og generering av events for RessursStillinger fra DVH")
//public class RessursStillingProcessorTest extends TestBase {
public class RessursStillingProcessorTest {
    private static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);
    private static final VerificationMode NEVER_INVOKED = times(0);
    @Mock
    OrgtilknytningCommandService orgtilknytningCommandService;
    @Mock
    OrgtilknytningStateService orgtilknytningStateService;
    @Mock
    IRessursService ressursService;
    @Mock
    RessursStillingLoadStatus ressursStillingLoadStatus;
    RessursStillingProcessor ressursStillingProcessor;
    OrgenhetService orgenhetService;
    OrgEnhetStateRepository orgEnhetStateRepository;

    @Captor
    ArgumentCaptor<OpprettOrgtilknytning> opprettOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<KorrigerOrgtilknytningDato> korrigerOrgtilknytningDatoArgumentCaptor;
    @Captor
    ArgumentCaptor<AvvikleOrgtilknytning> avvikleOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<FjernOrgtilknytning> fjernOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<EndreDagligOppfolgingOrgtilknytning> endreDagligOppfolgingOrgtilknytningArgumentCaptor;

    @Captor
    ArgumentCaptor<String> fidArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        orgEnhetStateRepository = new OrgEnhetStateRepository();
        orgenhetService = new OrgenhetService(orgEnhetStateRepository);
        ressursStillingProcessor = new RessursStillingProcessor(orgenhetService, orgtilknytningStateService, orgtilknytningCommandService,
            ressursStillingLoadStatus,
            ressursService);

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn("FID_X");

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
                    Optional.of("ORGENHET")),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningStateService.getAllOrgtilknytninger()).thenReturn(Collections.emptyList());

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
                    Optional.of("ORGENHET")),   // DVH ressursens agresso orgenhets nivå
                start,                          // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen eksitsterende orgtilknytninger
        when(orgtilknytningStateService.getAllOrgtilknytninger()).thenReturn(Collections.emptyList());
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
        when(ressursService.findByNavident(any())).thenReturn(Optional.of(new RessursState("FID_1", "FNR00000", navident, Sektor.NAV_STATLIG, null, Optional.empty(), Optional.empty())));
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
                    Optional.of("ORGENHET")),   // DVH ressursens agresso orgenhets nivå
                startDate,                          // Gyldig Fom for den strukturerte stillingen
                endDate,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningStateService.getAllOrgtilknytninger()).thenReturn(Collections.emptyList());

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
                    Optional.of("ORGENHET")),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_1").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // Ingen ekststerende orgtilknytninger
        when(orgtilknytningStateService.getAllOrgtilknytninger()).thenReturn(Collections.emptyList());
        when(ressursService.findByNavident(any())).thenReturn(
            Optional.of(new RessursState("FID_1", "FNR000", navident, Sektor.NAV_STATLIG, new Visningsnavn("Ola", Optional.empty(), "Normann"), Optional.of(ressursSluttDato), Optional.empty()))
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
                    Optional.of("ORGENHET")),   // DVH ressursens agresso orgenhets nivå
                startDate,                      // Gyldig Fom for den strukturerte stillingen
                MAX_DATE,                       // Gyldig Tom for den strukturerte stillingen
                orgenhetService.getByFid("FID_4").get(),
                StillingPeriode.DagligOppfolging.JA
            )
        );

        // To eksisterende orgtilknytninger
        OrgtilknytningState eksisterendeGammelEnhet = new OrgtilknytningState("OT_FID_1", navident, "a003bc", true, startDate, Optional.empty());
        OrgtilknytningState eksisterendeNyEnhet = new OrgtilknytningState("OT_FID_2", navident, "a004bc", false, startDate, Optional.empty());

        when(orgtilknytningStateService.getOrgtilknytningerByNavIdent(any())).thenReturn(List.of(eksisterendeGammelEnhet, eksisterendeNyEnhet));
        when(ressursService.findByNavident(any())).thenReturn(
            Optional.of(new RessursState("FID_1", "FNR000", navident, Sektor.NAV_STATLIG, new Visningsnavn("Ola", Optional.empty(), "Normann"), Optional.empty(), Optional.empty()))
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

    private void settOppStandardEnheter() {
        final Optional<LocalDate> EMPTY_DATE = Optional.empty();
        orgEnhetStateRepository.clear();
        orgEnhetStateRepository.putEventAggregate( lagEnhet("FID_1", "a001bc", "ENHET_1",           LocalDate.of(2020, 1, 1), EMPTY_DATE,                 "AGRID_1" ));
        orgEnhetStateRepository.putEventAggregate( lagEnhet("FID_2", "a002bc", "ENHET_2",           LocalDate.of(2020, 1, 1), EMPTY_DATE,                 "AGRID_2" ));
        orgEnhetStateRepository.putEventAggregate( lagEnhet("FID_3", "a003bc", "ENHET_3_AVSLUTTET", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 12, 31), "AGRID_3" ));
        orgEnhetStateRepository.putEventAggregate( lagEnhet("FID_4", "a004bc", "ENHET_3_NYTT_NAVN", LocalDate.of(2023, 1, 1), LocalDate.of(2022, 12, 31), "AGRID_3" ));
    }

    private OrgEnhetState lagEnhet(String fid, String id, String navn, LocalDate gyldigFom, LocalDate gyldigTom, String agressoId) {
        return lagEnhet(fid, id, navn, gyldigFom, Optional.ofNullable(gyldigTom), agressoId);
    }

    private OrgEnhetState lagEnhet(String fid, String id, String navn, LocalDate gyldigFom, Optional<LocalDate> gyldigTom, String agressoId) {
        return new OrgEnhetState(fid, id, navn, gyldigFom, gyldigTom, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(agressoId), Optional.of("ORGENHET"), Optional.empty());
    }

    // Test UTILITY:

    private enum CMD {
        OPPRETTET,
        KORRIGERT,
        AVVIKLET,
        FJERNET,
        ENDRE_DAGLIG_OPPFOLGING
    }

    private OrgtilknytningAssert assertOrgtilknytning(CMD cmd) {
        return assertOrgtilknytning(cmd, times(1));
    }

    private OrgtilknytningAssert assertOrgtilknytning(CMD cmd, VerificationMode verificationMode) {
        OrgtilknytningCommandService mock = verify(orgtilknytningCommandService, verificationMode);
        switch (cmd) {
            case OPPRETTET:
                mock.opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());
                if ( verificationMode != NEVER_INVOKED) {
                    return new OpprettOrgtilknytningAssert("FID_X", opprettOrgtilknytningArgumentCaptor.getValue());
                } else return null;
            case FJERNET:
                mock.fjernOrgtilknytning(fidArgumentCaptor.capture(), fjernOrgtilknytningArgumentCaptor.capture());
                if ( verificationMode != NEVER_INVOKED) {
                    return new FjernOrgtilknytningAssert(fidArgumentCaptor.getValue(), fjernOrgtilknytningArgumentCaptor.getValue());
                } else return null;
            case KORRIGERT:
                mock.korrigerOrgtilknytningDato(fidArgumentCaptor.capture(), korrigerOrgtilknytningDatoArgumentCaptor.capture());
                if ( verificationMode != NEVER_INVOKED) {
                    return new KorrigertOrgtilknytningDatoAssert(fidArgumentCaptor.getValue(), korrigerOrgtilknytningDatoArgumentCaptor.getValue());
                } else return null;
            case AVVIKLET:
                mock.avvikleOrgtilknytning(fidArgumentCaptor.capture(), avvikleOrgtilknytningArgumentCaptor.capture());
                if ( verificationMode != NEVER_INVOKED) {
                    return new AvvikleOrgtilknytningAssert(fidArgumentCaptor.getValue(), avvikleOrgtilknytningArgumentCaptor.getValue());
                } else return null;
            case ENDRE_DAGLIG_OPPFOLGING:
                mock.endreDagligOppfolgingOrgtilknytning(fidArgumentCaptor.capture(), endreDagligOppfolgingOrgtilknytningArgumentCaptor.capture());
                if ( verificationMode != NEVER_INVOKED) {
                    return new EndreDagligOppfolgingOrgtilknytningAssert(fidArgumentCaptor.getValue(), endreDagligOppfolgingOrgtilknytningArgumentCaptor.getValue());
                } else return null;
        }
        return null;
    }

    class OrgtilknytningAssert {

        final String fid;

        OrgtilknytningAssert(String fid) {
            this.fid = fid;
        }

        public String getFid() {
            return fid;
        }

        public OrgtilknytningAssert forFid(String fid) {
            fail("Orgtilknytning CMD objektet har ikke FID, kan ikke teste på det");
            return this;
        }
        public OrgtilknytningAssert forNavident(String navident){
            fail("Orgtilknytning CMD objektet har ikke Navident, kan ikke teste på det");
            return this;
        }
        public OrgtilknytningAssert tilEnhet(String orgenhetId){
            fail("Orgtilknytning CMD objektet har ikke OrgenhetId, kan ikke teste på det");
            return this;
        }
        public OrgtilknytningAssert gyldigFom(LocalDate gyldigFom){
            fail("Orgtilknytning CMD objektet har ikke GyldigFOM, kan ikke teste på det");
            return this;
        }
        public OrgtilknytningAssert gyldigTom(LocalDate gyldigTom){
            fail("Orgtilknytning CMD objektet har ikke GyldigTOM, kan ikke teste på det");
            return this;
        }
        public OrgtilknytningAssert erUtenGyldigTom(){
            fail("Orgtilknytning CMD objektet har ikke GyldigTOM, kan ikke teste på det");
            return this;
        }
        public OrgtilknytningAssert erDagligOppfolging(boolean erDagligOppfolging){
            fail("Orgtilknytning CMD objektet har ikke dagligOppfolging flagg, kan ikke teste på det");
            return this;
        }

    };

    class OpprettOrgtilknytningAssert extends OrgtilknytningAssert {
        private final OpprettOrgtilknytning opprettOrgtilknytning;

        OpprettOrgtilknytningAssert(String fid, OpprettOrgtilknytning opprettOrgtilknytning) {
            super(fid);
            this.opprettOrgtilknytning = opprettOrgtilknytning;
        }

        @Override
        public OrgtilknytningAssert forNavident(String navident) {
            assertEquals(navident, opprettOrgtilknytning.navident(), "Navident er ikke korrekt for OpprettOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert tilEnhet(String orgEnhetId) {
            assertEquals(orgEnhetId, opprettOrgtilknytning.orgenhetId(), "OrgenhetId er ikke korrekt for OpprettOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert gyldigFom(LocalDate gyldigFom) {
            assertEquals(gyldigFom, opprettOrgtilknytning.gyldigFom(), "GyldigFOM dato er ikke korrekt for OpprettOrgtilknytning");
            return this;
        }
    }

    class FjernOrgtilknytningAssert extends OrgtilknytningAssert {
        private final FjernOrgtilknytning fjernOrgtilknytning;

        FjernOrgtilknytningAssert(String fid, FjernOrgtilknytning fjernOrgtilknytning) {
            super(fid);
            this.fjernOrgtilknytning = fjernOrgtilknytning;
        }
        @Override
        public OrgtilknytningAssert forFid(String fid) {
            assertEquals(fid, this.fid, "FID er ikke korrekt for FjernOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert forNavident(String navident) {
            assertEquals(navident, fjernOrgtilknytning.navident(), "Navident er ikke korrekt for FjernOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert tilEnhet(String orgEnhetId) {
            assertEquals(orgEnhetId, fjernOrgtilknytning.orgenhetId(), "OrgenhetId er ikke korrekt for FjernOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert gyldigFom(LocalDate gyldigFom) {
            assertEquals(gyldigFom, fjernOrgtilknytning.gyldigFom(), "GyldigFOM dato er ikke korrekt for FjernOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert gyldigTom(LocalDate gyldigTom) {
            assertEquals(gyldigTom, fjernOrgtilknytning.gyldigTom().orElse(null), "GyldigFOM dato er ikke korrekt for FjernOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert erUtenGyldigTom() {
            assertTrue(fjernOrgtilknytning.gyldigTom().isEmpty(), "GyldigTOM er satt for FjernOrgtilknytning");
            return this;
        }
    }
    class KorrigertOrgtilknytningDatoAssert extends OrgtilknytningAssert {
        private final KorrigerOrgtilknytningDato korrigerOrgtilknytningDato;

        KorrigertOrgtilknytningDatoAssert(String fid, KorrigerOrgtilknytningDato korrigerOrgtilknytningDato) {
            super(fid);
            this.korrigerOrgtilknytningDato = korrigerOrgtilknytningDato;
        }
        // TODO: Implement
    }

    class AvvikleOrgtilknytningAssert extends OrgtilknytningAssert {
        private final AvvikleOrgtilknytning avvikleOrgtilknytning;

        AvvikleOrgtilknytningAssert(String fid, AvvikleOrgtilknytning avvikleOrgtilknytning) {
            super(fid);
            this.avvikleOrgtilknytning = avvikleOrgtilknytning;
        }

        @Override
        public OrgtilknytningAssert forFid(String fid) {
            assertEquals(fid, this.fid, "FID er ikke korrekt for AvvikleOrgtilknytning");
            return this;
        }
        @Override
        public OrgtilknytningAssert forNavident(String navident) {
            assertEquals(navident, avvikleOrgtilknytning.navident(), "Navident er ikke korrekt for AvvikleOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert tilEnhet(String orgEnhetId) {
            assertEquals(orgEnhetId, avvikleOrgtilknytning.orgenhetId(), "OrgenhetId er ikke korrekt for AvvikleOrgtilknytning");
            return this;
        }
        @Override
        public OrgtilknytningAssert gyldigTom(LocalDate gyldigTom) {
            assertEquals(gyldigTom, avvikleOrgtilknytning.gyldigTom(), "GyldigFOM dato er ikke korrekt for AvvikleOrgtilknytning");
            return this;
        }
    }
    class EndreDagligOppfolgingOrgtilknytningAssert extends OrgtilknytningAssert {
        private final EndreDagligOppfolgingOrgtilknytning endreDagligOppfolgingOrgtilknytning;

        EndreDagligOppfolgingOrgtilknytningAssert(String fid, EndreDagligOppfolgingOrgtilknytning endreDagligOppfolgingOrgtilknytning) {
            super(fid);
            this.endreDagligOppfolgingOrgtilknytning = endreDagligOppfolgingOrgtilknytning;
        }

        @Override
        public OrgtilknytningAssert forFid(String fid) {
            assertEquals(fid, this.fid, "FID er ikke korrekt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }
        @Override
        public OrgtilknytningAssert forNavident(String navident) {
            assertEquals(navident, endreDagligOppfolgingOrgtilknytning.navident(), "Navident er ikke korrekt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert tilEnhet(String orgEnhetId) {
            assertEquals(orgEnhetId, endreDagligOppfolgingOrgtilknytning.orgenhetId(), "OrgenhetId er ikke korrekt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert gyldigFom(LocalDate gyldigFom) {
            assertEquals(gyldigFom, endreDagligOppfolgingOrgtilknytning.gyldigFom(), "GyldigFOM dato er ikke korrekt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert gyldigTom(LocalDate gyldigTom) {
            assertEquals(gyldigTom, endreDagligOppfolgingOrgtilknytning.gyldigTom().orElse(null), "GyldigFOM dato er ikke korrekt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert erUtenGyldigTom() {
            assertTrue(endreDagligOppfolgingOrgtilknytning.gyldigTom().isEmpty(), "GyldigTOM er satt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }

        public OrgtilknytningAssert erDagligOppfolging(boolean erDagligOppfolging){
            assertEquals(erDagligOppfolging, endreDagligOppfolgingOrgtilknytning.dagligOppfolging(), "ErDagligOppfolging er ikke korrekt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }
    }

}
