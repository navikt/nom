package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningSektorCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.verification.VerificationMode;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
public class OrgtilknytningCmdServiceTestBase {
    static final VerificationMode NEVER_INVOKED = times(0);

    @Mock
    public OrgtilknytningCommandService orgtilknytningCommandService;

    @Captor
    ArgumentCaptor<OpprettOrgtilknytning> opprettOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<EndreOrgtilknytningDato> endreDatoOrgtilknytningDatoArgumentCaptor;
    @Captor
    ArgumentCaptor<AvvikleOrgtilknytning> avvikleOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<FjernOrgtilknytning> fjernOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<EndreDagligOppfolgingOrgtilknytning> endreDagligOppfolgingOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<EndreOrgtilknytningSektorCommand> endreOrgtilknytningSektorCommandArgumentCaptor;
    @Captor
    ArgumentCaptor<EndreTilknytningtypeCommand> endreTilknytningtypeCommandArgumentCaptor;

    @Captor
    ArgumentCaptor<String> endretDatoFidArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> avvikletFidArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> fjernetFidArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> endretDagligOppfolgingFidArgumentCaptor = ArgumentCaptor.forClass(String.class);

    int opprettetTimes;
    int endretDatoTimes;
    int avvikleTimes;
    int fjernetTimes;
    int endretDagligOppfolgingTimes;
    int endretSektorTimes;
    int endretTilknytningtypeTimes;

    @BeforeEach
    void setup() {
        opprettetTimes = 0;
        endretDatoTimes = 0;
        avvikleTimes = 0;
        fjernetTimes = 0;
        endretDagligOppfolgingTimes = 0;
        endretSektorTimes = 0;
        endretTilknytningtypeTimes = 0;
    }

    protected enum CMD {
        OPPRETTET,
        ENDRET_DATO,
        AVVIKLET,
        FJERNET,
        ENDRE_DAGLIG_OPPFOLGING,
        ENDRE_SEKTOR,
        ENDRE_TILKNYTNINGTYPE
    }

    protected OrgtilknytningAssert assertOrgtilknytning(CMD cmd) {
        return assertOrgtilknytning(cmd, null);
    }

    OrgtilknytningAssert assertOrgtilknytning(CMD cmd, VerificationMode verificationMode) {
        if (verificationMode != NEVER_INVOKED) {
            switch (cmd) {
                case OPPRETTET -> verificationMode = atLeast(++opprettetTimes);
                case FJERNET -> verificationMode = atLeast(++fjernetTimes);
                case ENDRET_DATO -> verificationMode = atLeast(++endretDatoTimes);
                case AVVIKLET -> verificationMode = atLeast(++avvikleTimes);
                case ENDRE_DAGLIG_OPPFOLGING -> verificationMode = atLeast(++endretDagligOppfolgingTimes);
                case ENDRE_SEKTOR -> verificationMode = atLeast(++endretSektorTimes);
                case ENDRE_TILKNYTNINGTYPE -> verificationMode = atLeast(++endretTilknytningtypeTimes);
            }
        }

        OrgtilknytningCommandService mock = verify(orgtilknytningCommandService, verificationMode);

        switch (cmd) {
            case OPPRETTET -> {
                mock.opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());
                log.debug("OPPRETTET ({})", opprettetTimes);
                if (verificationMode != NEVER_INVOKED) {
                    return new OpprettOrgtilknytningAssert("FID_%s".formatted(opprettetTimes), opprettOrgtilknytningArgumentCaptor.getAllValues().get(opprettetTimes - 1));
                } else {
                    return null;
                }
            }
            case FJERNET -> {
                mock.fjernOrgtilknytning(fjernetFidArgumentCaptor.capture(), fjernOrgtilknytningArgumentCaptor.capture());
                log.debug("FJERNET ({})- FID argument: {}", fjernetTimes, fjernetFidArgumentCaptor.getAllValues());
                if (verificationMode != NEVER_INVOKED) {
                    return new FjernOrgtilknytningAssert(fjernetFidArgumentCaptor.getAllValues().get(fjernetTimes -1), fjernOrgtilknytningArgumentCaptor.getAllValues().get(fjernetTimes - 1));
                } else {
                    return null;
                }
            }
            case ENDRET_DATO -> {
                mock.endreOrgtilknytningDato(endretDatoFidArgumentCaptor.capture(), endreDatoOrgtilknytningDatoArgumentCaptor.capture());
                log.debug("ENDRET DATO ({})- FID argument: {}", endretDatoTimes, endretDatoFidArgumentCaptor.getAllValues());
                if (verificationMode != NEVER_INVOKED) {
                    return new EndretOrgtilknytningDatoAssert(endretDatoFidArgumentCaptor.getAllValues().get(endretDatoTimes -1), endreDatoOrgtilknytningDatoArgumentCaptor.getAllValues().get(
                        endretDatoTimes - 1));
                } else {
                    return null;
                }
            }
            case AVVIKLET -> {
                mock.avvikleOrgtilknytning(avvikletFidArgumentCaptor.capture(), avvikleOrgtilknytningArgumentCaptor.capture());
                log.debug("AVVIKLET ({})- FID argument: {}", avvikleTimes, avvikletFidArgumentCaptor.getAllValues());
                if (verificationMode != NEVER_INVOKED) {
                    return new AvvikleOrgtilknytningAssert(avvikletFidArgumentCaptor.getAllValues().get(avvikleTimes -1), avvikleOrgtilknytningArgumentCaptor.getAllValues().get(avvikleTimes - 1));
                } else {
                    return null;
                }
            }
            case ENDRE_DAGLIG_OPPFOLGING -> {
                mock.endreDagligOppfolgingOrgtilknytning(endretDagligOppfolgingFidArgumentCaptor.capture(), endreDagligOppfolgingOrgtilknytningArgumentCaptor.capture());
                log.debug("ENDRE_DAGLIG_OPPFOLGING ({})- FID argument: {}", endretDagligOppfolgingTimes, endretDagligOppfolgingFidArgumentCaptor.getAllValues());
                if (verificationMode != NEVER_INVOKED) {
                    return new EndreDagligOppfolgingOrgtilknytningAssert(
                        endretDagligOppfolgingFidArgumentCaptor.getAllValues().get(endretDagligOppfolgingTimes -1),
                        endreDagligOppfolgingOrgtilknytningArgumentCaptor.getAllValues().get(endretDagligOppfolgingTimes - 1)
                    );
                } else {
                    return null;
                }
            }
            case ENDRE_SEKTOR -> {
                mock.endreOrgtilknytningSektor(endreOrgtilknytningSektorCommandArgumentCaptor.capture());
                log.debug("ENDRE_SEKTOR ({})- FID argument: {}", endretSektorTimes, endreOrgtilknytningSektorCommandArgumentCaptor.getAllValues());
                if (verificationMode != NEVER_INVOKED) {
                    return new EndreOrgtilknytningSektorAssert(
                        endreOrgtilknytningSektorCommandArgumentCaptor.getAllValues().get(endretSektorTimes - 1).fid(),
                        endreOrgtilknytningSektorCommandArgumentCaptor.getAllValues().get(endretSektorTimes - 1)
                    );
                } else {
                    return null;
                }
            }

            case ENDRE_TILKNYTNINGTYPE -> {
                mock.endreTilknytningtype(endreTilknytningtypeCommandArgumentCaptor.capture());
                log.debug("ENDRET_TILKNYTNINGTYPE ({})- FID argument: {}", endretTilknytningtypeTimes, endreTilknytningtypeCommandArgumentCaptor.getAllValues());
                if (verificationMode != NEVER_INVOKED) {
                    return new EndreTilknytningtypeAssert(
                        endreTilknytningtypeCommandArgumentCaptor.getAllValues().get(endretTilknytningtypeTimes - 1).fid(),
                        endreTilknytningtypeCommandArgumentCaptor.getAllValues().get(endretTilknytningtypeTimes - 1)
                    );
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    protected class OrgtilknytningAssert {

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

        public OrgtilknytningAssert forNavident(String navident) {
            fail("Orgtilknytning CMD objektet har ikke Navident, kan ikke teste på det");
            return this;
        }

        public OrgtilknytningAssert tilEnhet(String orgenhetId) {
            fail("Orgtilknytning CMD objektet har ikke OrgenhetId, kan ikke teste på det");
            return this;
        }

        public OrgtilknytningAssert gyldigFom(LocalDate gyldigFom) {
            fail("Orgtilknytning CMD objektet har ikke GyldigFOM, kan ikke teste på det");
            return this;
        }

        public OrgtilknytningAssert gyldigTom(LocalDate gyldigTom) {
            fail("Orgtilknytning CMD objektet har ikke GyldigTOM, kan ikke teste på det");
            return this;
        }

        public OrgtilknytningAssert erUtenGyldigTom() {
            fail("Orgtilknytning CMD objektet har ikke GyldigTOM, kan ikke teste på det");
            return this;
        }

        public OrgtilknytningAssert erDagligOppfolging(boolean erDagligOppfolging) {
            fail("Orgtilknytning CMD objektet har ikke dagligOppfolging flagg, kan ikke teste på det");
            return this;
        }

        public OrgtilknytningAssert tilSektor(Sektor sektor) {
            fail("Orgtilknytning CMD objektet har ikke sektor, kan ikke teste på det");
            return this;
        }

        public OrgtilknytningAssert tilTilknytningtype(Tilknytningtype tilknytningtype) {
            fail("Orgtilknytning CMD objektet har ikke tilknytningtype, kan ikke teste på det");
            return this;
        }
    }

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

        @Override
        public OrgtilknytningAssert tilSektor(Sektor sektor) {
            assertEquals(sektor, opprettOrgtilknytning.sektor(), "Sektor er ikke korrekt for OpprettOrgtilknytning");
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

    class EndretOrgtilknytningDatoAssert extends OrgtilknytningAssert {
        private final EndreOrgtilknytningDato endreOrgtilknytningDato;

        EndretOrgtilknytningDatoAssert(String fid, EndreOrgtilknytningDato endreOrgtilknytningDato) {
            super(fid);
            this.endreOrgtilknytningDato = endreOrgtilknytningDato;
        }

        @Override
        public OrgtilknytningAssert forFid(String fid) {
            assertEquals(fid, this.fid, "FID er ikke korrekt for EndreOrgtilknytningDato");
            return this;
        }

        @Override
        public OrgtilknytningAssert gyldigFom(LocalDate gyldigFom) {
            assertEquals(gyldigFom, endreOrgtilknytningDato.gyldigFom(), "GyldigFOM dato er ikke korrekt for EndreOrgtilknytningDato");
            return this;
        }

        @Override
        public OrgtilknytningAssert gyldigTom(LocalDate gyldigTom) {
            assertEquals(gyldigTom, endreOrgtilknytningDato.gyldigTom().orElse(null), "GyldigTOM dato er ikke korrekt for EndreOrgtilknytningDato");
            return this;
        }
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
            assertEquals(gyldigTom, avvikleOrgtilknytning.gyldigTom(), "GyldigTOM dato er ikke korrekt for AvvikleOrgtilknytning");
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
            assertEquals(gyldigTom, endreDagligOppfolgingOrgtilknytning.gyldigTom().orElse(null), "GyldigTOM dato er ikke korrekt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }

        @Override
        public OrgtilknytningAssert erUtenGyldigTom() {
            assertTrue(endreDagligOppfolgingOrgtilknytning.gyldigTom().isEmpty(), "GyldigTOM er satt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }

        public OrgtilknytningAssert erDagligOppfolging(boolean erDagligOppfolging) {
            assertEquals(erDagligOppfolging, endreDagligOppfolgingOrgtilknytning.dagligOppfolging(), "ErDagligOppfolging er ikke korrekt for EndreDagligOppfolgingOrgtilknytning");
            return this;
        }
    }

    class EndreOrgtilknytningSektorAssert extends OrgtilknytningAssert {
        private final EndreOrgtilknytningSektorCommand endreOrgtilknytningSektorCommand;

        EndreOrgtilknytningSektorAssert(String fid, EndreOrgtilknytningSektorCommand endreOrgtilknytningSektorCommand) {
            super(fid);
            this.endreOrgtilknytningSektorCommand = endreOrgtilknytningSektorCommand;
        }

        @Override
        public OrgtilknytningAssert forFid(String fid) {
            assertEquals(fid, this.fid, "FID er ikke korrekt for EndreOrgtilknytningSektorCommand");
            return this;
        }

        @Override
        public OrgtilknytningAssert forNavident(String navident) {
            assertEquals(navident, endreOrgtilknytningSektorCommand.navident(), "Navident er ikke korrekt for EndreOrgtilknytningSektorCommand");
            return this;
        }

        @Override
        public OrgtilknytningAssert tilEnhet(String orgEnhetId) {
            assertEquals(orgEnhetId, endreOrgtilknytningSektorCommand.orgenhetId(), "OrgenhetId er ikke korrekt for EndreOrgtilknytningSektorCommand");
            return this;
        }

        @Override
        public OrgtilknytningAssert tilSektor(Sektor sektor) {
            assertEquals(sektor, endreOrgtilknytningSektorCommand.sektor(), "Sektor er ikke korrekt for EndreOrgtilknytningSektorCommand");
            return this;
        }
    }

    class EndreTilknytningtypeAssert extends OrgtilknytningAssert {
        private final EndreTilknytningtypeCommand endreTilknytningtypeCommand;

        EndreTilknytningtypeAssert(String fid, EndreTilknytningtypeCommand endreTilknytningtypeCommand) {
            super(fid);
            this.endreTilknytningtypeCommand = endreTilknytningtypeCommand;
        }

        @Override
        public OrgtilknytningAssert forFid(String fid) {
            assertEquals(fid, this.fid, "FID er ikke korrekt for EndreTilknytningtypeCommand");
            return this;
        }

        @Override
        public OrgtilknytningAssert tilTilknytningtype(Tilknytningtype tilknytningtype) {
            assertEquals(Optional.of(tilknytningtype), endreTilknytningtypeCommand.tilknytningtype(), "Tilknytningtype er ikke korrekt for EndreTilknytningtypeCommand");
            return this;
        }
    }
}