package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningSektorCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OrgtilknytningCommand;
import no.nav.nom.apps.api.u4diff.U4DiffPaaMastrede;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.AbstractMap.SimpleEntry;
import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isCurrentOrFuture;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.lik;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.overlapsInterval;

@Component
@Slf4j
public class NyeRessursStillingProcessor {
    private final OrgtilknytningService orgtilknytningService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;
    private final U4DiffPaaMastrede u4DiffPaaMastrede;
    private final Unleash unleash;

    public NyeRessursStillingProcessor(
        OrgtilknytningService orgtilknytningService, OrgtilknytningCommandService orgtilknytningCommandService, RessursStateRepository ressursStateRepository,
        NomMastered nomMastered,
        U4DiffPaaMastrede u4DiffPaaMastrede,
        Unleash unleash
    ) {
        this.orgtilknytningService = orgtilknytningService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
        this.u4DiffPaaMastrede = u4DiffPaaMastrede;
        this.unleash = unleash;
    }

    public Map<String, StrukturerteStillingPerioderForNavident>  process(Map<String, StrukturerteStillingPerioderForNavident> stillingPerioderPerNavident) {
        Map<String, StrukturerteStillingPerioderForNavident> stillingPerioderPerNavidentResultat = new HashMap<>();
        stillingPerioderPerNavident.forEach( (navident, sspForNavident) -> {
            var resultat = process(navident, sspForNavident);
            if (resultat != null) {
                stillingPerioderPerNavidentResultat.put(navident, resultat);
            }
        });
        u4DiffPaaMastrede.rapporterU4NavidenterBehandlet(stillingPerioderPerNavident.keySet().stream().toList());
        return stillingPerioderPerNavidentResultat;
    }

    private StrukturerteStillingPerioderForNavident process(String navident, StrukturerteStillingPerioderForNavident sspForNavident) {
        var ressurs = ressursStateRepository.findByNavident(navident).orElseThrow(() -> new RuntimeException("Ressurs ikke funnet for navident " + navident));
        if (!ressurs.isStatlig()) {
            log.error("Ressurs med navident {} er ikke statlig", navident);
            return null;
        }
        var eksisterendeOTer = new ArrayList<>(orgtilknytningService.getByNavIdent(navident));

        Map<StillingPeriode, OrgtilknytningState> matchedeOTer = new HashMap<>();
        List<StillingPeriode> stillingPerioderAaOpprette = new ArrayList<>();

        // For hver stillingperiode, finn eksisterende OT med samme enhet og gyldigFom
        // Disse skal endres
        // Stillingperioder uten eksisterende OT skal opprettes
        // Eksisterende OT uten tilsvarende stillingperiode skal fjernes
        if (unleash.isEnabled("nom.ny-tilknytning-mapping")) {
            matchEksisterendeMotStillingPerioderNew(sspForNavident.stillingPerioder(), eksisterendeOTer, matchedeOTer, stillingPerioderAaOpprette);
        } else {
            matchEksisterendeMotStillingPerioderOld(sspForNavident.stillingPerioder(), eksisterendeOTer, matchedeOTer, stillingPerioderAaOpprette);
        }


        var eksisterendeAaFjerne = getOTerAaFjerne(eksisterendeOTer, matchedeOTer);

        List<SimpleEntry<String, OrgtilknytningCommand>> endringer = new ArrayList<>();

        endringer.addAll(endreEksisterende(matchedeOTer, sspForNavident));
        endringer.addAll(opprettNye(stillingPerioderAaOpprette, sspForNavident));
        endringer.addAll(fjerngamle(eksisterendeAaFjerne));

        if (!endringer.isEmpty()) {
            if (nomMastered.isMastered(navident)) {
                log.debug("Navident {} er mastered. Vi endrer ikke orgtilknytning(er) fra DVH for denne", navident);
                u4DiffPaaMastrede.rapporterU4Diff(navident, matchedeOTer, eksisterendeAaFjerne, stillingPerioderAaOpprette);
            } else {
                endringer = utfoerEndringer(endringer);
            }
        } else if (nomMastered.isMastered(navident)) {
            u4DiffPaaMastrede.rapporterIngenU4Diff(navident);
        }

        return new StrukturerteStillingPerioderForNavident(sspForNavident, eksisterendeOTer, endringer);
    }

    private List<SimpleEntry<String, OrgtilknytningCommand>> endreEksisterende(Map<StillingPeriode, OrgtilknytningState> matchedeOTer, StrukturerteStillingPerioderForNavident sspForNavident) {
        var endringer = new ArrayList<SimpleEntry<String, OrgtilknytningCommand>>();

        matchedeOTer.forEach( (sp, ot) -> {
            boolean gyldigTomErUlik = false;
            if (!lik(ot.gyldigTom().orElse(null), sp.gyldigTom())) {
                gyldigTomErUlik = true;
                if (ot.gyldigTom().isEmpty()) {
                    endringer.add(lagAvvikleEventCmd(ot, sp, sspForNavident));
                } else {
                    endringer.add(lagEndreGyldigTomCmd(ot, sp, sspForNavident));
                }
            }
            if (ot.erDagligOppfolging() != sp.erDagligOppfolging()) {
                endringer.add(lagEndreDOLCmd(ot, sp, sspForNavident));
            }

            if (unleash.isEnabled("nom.ny-tilknytning-mapping")) {
                if (!lik(ot.gyldigFom(), sp.gyldigFom())) {
                    var cmd = lagEndreGyldigFomCmd(ot, sp, sspForNavident, gyldigTomErUlik);
                    if (cmd != null) {
                        endringer.add(cmd);
                    }
                }

                if (isCurrentOrFuture(sp.gyldigTom())) {
                    if (sp.sektor() != null) {
                        if (isNull(ot.sektor()) || ot.sektor() != sp.sektor()) {
                            endringer.add(lagSektorEndringCmd(ot, sp, sspForNavident));
                        }
                    }
                    if (sp.tilknytningtype().isPresent()) {
                        if (ot.orgtilknytningType().isEmpty() || ot.orgtilknytningType().get() != sp.tilknytningtype().get()) {
                            endringer.add(lagEndreTilknytningtypeCmd(ot, sp));
                        }
                    }
                }
            } else {
                if (sp.erDagligOppfolging() && isCurrentOrFuture(sp.gyldigTom())) {
                    if (sp.sektor() != null) {
                        if (isNull(ot.sektor()) || ot.sektor() != sp.sektor()) {
                            endringer.add(lagSektorEndringCmd(ot, sp, sspForNavident));
                        }
                    }
                    if (sp.tilknytningtype().isPresent()) {
                        if (ot.orgtilknytningType().isEmpty() || ot.orgtilknytningType().get() != sp.tilknytningtype().get()) {
                            endringer.add(lagEndreTilknytningtypeCmd(ot, sp));
                        }
                    }
                }
            }
        });

        return endringer;
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagEndreTilknytningtypeCmd(
        OrgtilknytningState ot,
        StillingPeriode sp
    ) {
        var navident = ot.navident();
        var orgenhetId = ot.orgEnhetId();

        InputMetadata inputMetadata = getMetadata(navident, orgenhetId);

        var cmd = new EndreTilknytningtypeCommand(
            ot.fid(),
            sp.tilknytningtype(),
            inputMetadata
        );

        return new SimpleEntry<>(ot.fid(), cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagEndreTilknytningtypeCmd(
        String fiktivFID,
        StillingPeriode sp,
        StrukturerteStillingPerioderForNavident sspForNavident
    ) {
        var navident = sspForNavident.navident();
        var orgenhetId = sp.orgenhet().getId();

        InputMetadata inputMetadata = getMetadata(navident, orgenhetId);

        var cmd = new EndreTilknytningtypeCommand(
            fiktivFID,
            sp.tilknytningtype(),
            inputMetadata
        );

        return new SimpleEntry<>(fiktivFID, cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagSektorEndringCmd(String fiktivFID, StillingPeriode sp, StrukturerteStillingPerioderForNavident sspForNavident) {
        var navident = sspForNavident.navident();
        var orgenhetId = sp.orgenhet().getId();

        InputMetadata inputMetadata = getMetadata(navident, orgenhetId);

        var cmd = new EndreOrgtilknytningSektorCommand(
            fiktivFID,
            navident,
            orgenhetId,
            sp.sektor(),
            inputMetadata,
            new OrgtilknytningEvent.AdditionalAgressoData(
                sp.stilling().toString(),
                StillingPeriode.toString(sspForNavident.stillingPerioder())
            )
        );

        return new SimpleEntry<>(fiktivFID, cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagSektorEndringCmd(OrgtilknytningState ot, StillingPeriode sp, StrukturerteStillingPerioderForNavident sspForNavident) {
        var navident = ot.navident();
        var orgenhetId = ot.orgEnhetId();

        InputMetadata inputMetadata = getMetadata(navident, orgenhetId);

        var cmd = new EndreOrgtilknytningSektorCommand(
            ot.fid(),
            navident,
            orgenhetId,
            sp.sektor(),
            inputMetadata,
            new OrgtilknytningEvent.AdditionalAgressoData(
                sp.stilling().toString(),
                StillingPeriode.toString(sspForNavident.stillingPerioder())
            )
        );

        return new SimpleEntry<>(ot.fid(), cmd);
    }

    private List<SimpleEntry<String, OrgtilknytningCommand>> fjerngamle(List<OrgtilknytningState> eksisterendeAaFjerne) {
        var endringer = new ArrayList<SimpleEntry<String, OrgtilknytningCommand>>();
        eksisterendeAaFjerne.forEach(ot -> endringer.add(lagFjernEventCmd(ot)));
        return endringer;
    }

    private List<SimpleEntry<String, OrgtilknytningCommand>> utfoerEndringer(List<SimpleEntry<String, OrgtilknytningCommand>> endringer) {
        var endringerMedFid = new ArrayList<SimpleEntry<String, OrgtilknytningCommand>>();
        Map<String, String> fiktivFidTilFaktiskFid = new HashMap<>();

        endringer.forEach(entry -> {
            var endring = entry.getValue();
            var endringFID = entry.getKey();
            if (Objects.requireNonNull(endring) instanceof OpprettOrgtilknytning cmd) {
                var fiktivFID = endringFID;
                endringFID = orgtilknytningCommandService.opprettOrgtilknytning(cmd);
                fiktivFidTilFaktiskFid.put(fiktivFID, endringFID);
            } else {
                endringFID = hentFaktiskFIDHvisFiktiv(endringFID, fiktivFidTilFaktiskFid);
                switch (endring) {
                    case EndreDagligOppfolgingOrgtilknytning cmd ->
                            orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(endringFID, cmd);
                    case EndreOrgtilknytningDato cmd ->
                            orgtilknytningCommandService.endreOrgtilknytningDato(endringFID, cmd);
                    case AvvikleOrgtilknytning cmd ->
                            orgtilknytningCommandService.avvikleOrgtilknytning(endringFID, cmd);
                    case FjernOrgtilknytning cmd ->
                            orgtilknytningCommandService.fjernOrgtilknytning(endringFID, cmd);
                    case EndreTilknytningtypeCommand cmd ->
                            orgtilknytningCommandService.endreTilknytningtype(cmd.toBuilder().fid(endringFID).build());
                    case EndreOrgtilknytningSektorCommand cmd ->
                            orgtilknytningCommandService.endreOrgtilknytningSektor(cmd.toBuilder().fid(endringFID).build());
                    default ->
                            throw new IllegalStateException("Unexpected OrgtilknytningCommand object: " + entry.getValue().getClass().getName());
                }
            }
            endringerMedFid.add(new SimpleEntry<>(endringFID, entry.getValue()));
        });
        return endringerMedFid;
    }

    private String hentFaktiskFIDHvisFiktiv(String endringFID, Map<String, String> fiktivFidTilFaktiskFid) {
        if (endringFID.startsWith("NY_FID_")) {
             var faktiskFID = fiktivFidTilFaktiskFid.get(endringFID);
             if (faktiskFID == null) {
                 throw new RuntimeException("Faktisk FID ikke funnet for fiktiv FID " + endringFID);
             }
             return faktiskFID;
        }
        return endringFID;
    }

    private List<SimpleEntry<String, OrgtilknytningCommand>> opprettNye(List<StillingPeriode> stillingPerioderAaOpprette, StrukturerteStillingPerioderForNavident sspForNavident) {
        var nye = new ArrayList<SimpleEntry<String, OrgtilknytningCommand>>();
        AtomicInteger nyFidId = new AtomicInteger(0);
        stillingPerioderAaOpprette.forEach(sp -> {
            var fiktivFID = "NY_FID_" + (nyFidId.incrementAndGet());
            nye.add(lagOpprettEventCmd(fiktivFID, sp, sspForNavident));
            if (sp.erDagligOppfolging()) {
                nye.add(lagEndreDOLCmd(fiktivFID, sp, sspForNavident));
                if (!unleash.isEnabled("nom.ny-tilknytning-mapping")) {
                    if (sp.sektor() != null && sp.tilknytningtype().isPresent() && isCurrentOrFuture(sp.gyldigTom())) {
                        nye.add(lagSektorEndringCmd(fiktivFID, sp, sspForNavident));
                        nye.add(lagEndreTilknytningtypeCmd(fiktivFID, sp, sspForNavident));
                    }
                }
            }

            if (unleash.isEnabled("nom.ny-tilknytning-mapping")) {
                if (isCurrentOrFuture(sp.gyldigTom())) {
                    if (sp.sektor() != null) {
                        nye.add(lagSektorEndringCmd(fiktivFID, sp, sspForNavident));
                    }
                    if (sp.tilknytningtype().isPresent()) {
                        nye.add(lagEndreTilknytningtypeCmd(fiktivFID, sp, sspForNavident));
                    }
                }
            }

            if (sp.gyldigTom().isBefore(NO_END_DATE)) {
                nye.add(lagEndreGyldigTomCmd(fiktivFID, sp, sspForNavident));
            }
        });
        return nye;
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagOpprettEventCmd(String fiktivFID, StillingPeriode stillingPeriode, StrukturerteStillingPerioderForNavident sspForNavident) {
        String navident = sspForNavident.navident();
        String orgenhetId = stillingPeriode.orgenhet().getId();

        OrgtilknytningEvent.AdditionalAgressoData additionalData = getAdditionalData(stillingPeriode, sspForNavident);

        InputMetadata metadata = getMetadata(navident, orgenhetId);

        var cmd = new OpprettOrgtilknytning(
            navident,
            orgenhetId,
            null,
            stillingPeriode.gyldigFom(),
            null,
            metadata,
            additionalData
        );
        return new SimpleEntry<>(fiktivFID, cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagEndreDOLCmd(String fiktivFID, StillingPeriode stillingPeriode, StrukturerteStillingPerioderForNavident sspForNavident) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = getAdditionalData(stillingPeriode, sspForNavident);

        InputMetadata metadata = getMetadata(sspForNavident.navident(), stillingPeriode.orgenhet().getId());

        var cmd = new EndreDagligOppfolgingOrgtilknytning(
            sspForNavident.navident(),
            stillingPeriode.orgenhet().getId(),
            stillingPeriode.gyldigFom(),
            stillingPeriode.gyldigTom().equals(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom()),
            stillingPeriode.erDagligOppfolging(),
            metadata,
            additionalData
        );
        return new SimpleEntry<>(fiktivFID, cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagEndreGyldigTomCmd(String fiktivFID, StillingPeriode stillingPeriode, StrukturerteStillingPerioderForNavident sspForNavident) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = getAdditionalData(stillingPeriode, sspForNavident);

        InputMetadata metadata = getMetadata(sspForNavident.navident(), stillingPeriode.orgenhet().getId());

        var cmd = new EndreOrgtilknytningDato(
            sspForNavident.navident(),
            stillingPeriode.orgenhet().getId(),
            stillingPeriode.gyldigFom(),
            stillingPeriode.gyldigTom().equals(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom()),
            metadata,
            additionalData
        );
        return new SimpleEntry<>(fiktivFID, cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagAvvikleEventCmd(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, StrukturerteStillingPerioderForNavident sspForNavident) {
        if (stillingPeriode.gyldigTom().equals(NO_END_DATE)) {
            throw new RuntimeException("StillingPeriode kan ikke være gyldigTom = NO_END_DATE i lagAvvikleEventCmd(..)");
        }

        OrgtilknytningEvent.AdditionalAgressoData additionalData = getAdditionalData(stillingPeriode, sspForNavident);

        InputMetadata metadata = getMetadata(originalOrgtilknytning);

        var cmd = new AvvikleOrgtilknytning(
            originalOrgtilknytning.navident(),
            originalOrgtilknytning.orgEnhetId(),
            stillingPeriode.gyldigTom(),
            metadata,
            additionalData
        );
        return new SimpleEntry<>(originalOrgtilknytning.fid(), cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagEndreGyldigTomCmd(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, StrukturerteStillingPerioderForNavident sspForNavident) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = getAdditionalData(stillingPeriode, sspForNavident);

        InputMetadata metadata = getMetadata(originalOrgtilknytning);

        var cmd = new EndreOrgtilknytningDato(
            originalOrgtilknytning.navident(),
            originalOrgtilknytning.orgEnhetId(),
            originalOrgtilknytning.gyldigFom(),
            stillingPeriode.gyldigTom().equals(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom()),
            metadata,
            additionalData
        );
        return new SimpleEntry<>(originalOrgtilknytning.fid(), cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagEndreGyldigFomCmd(
        OrgtilknytningState originalOrgtilknytning,
        StillingPeriode stillingPeriode,
        StrukturerteStillingPerioderForNavident sspForNavident,
        boolean gyldigTomErUlik
    ) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = getAdditionalData(stillingPeriode, sspForNavident);
        InputMetadata metadata = getMetadata(originalOrgtilknytning);

        LocalDate threeYearsAgo = LocalDate.now().minusYears(3).plusMonths(1);
        Optional<LocalDate> gyldigTom = gyldigTomErUlik ? Optional.of(stillingPeriode.gyldigTom()) : originalOrgtilknytning.gyldigTom();

        // Allow the update if the original gyldigFom is within the last three years or the new gyldigFom is within the last three years
        if (originalOrgtilknytning.gyldigFom().isAfter(threeYearsAgo) || stillingPeriode.gyldigFom().isAfter(threeYearsAgo)) {
            var cmd = new EndreOrgtilknytningDato(
                originalOrgtilknytning.navident(),
                originalOrgtilknytning.orgEnhetId(),
                stillingPeriode.gyldigFom(),
                gyldigTom.equals(Optional.of(NO_END_DATE)) ? Optional.empty() : gyldigTom,
                metadata,
                additionalData
            );
            return new SimpleEntry<>(originalOrgtilknytning.fid(), cmd);
        }

        return null;
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagEndreDOLCmd(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, StrukturerteStillingPerioderForNavident sspForNavident) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = getAdditionalData(stillingPeriode, sspForNavident);

        InputMetadata metadata = getMetadata(originalOrgtilknytning);

        var cmd = new EndreDagligOppfolgingOrgtilknytning(
            originalOrgtilknytning.navident(),
            originalOrgtilknytning.orgEnhetId(),
            stillingPeriode.gyldigFom(),
            Optional.ofNullable(stillingPeriode.gyldigTom()),
            stillingPeriode.erDagligOppfolging(),
            metadata,
            additionalData
        );
        return new SimpleEntry<>(originalOrgtilknytning.fid(), cmd);
    }

    private SimpleEntry<String, OrgtilknytningCommand> lagFjernEventCmd(OrgtilknytningState originalOrgtilknytning) {
        InputMetadata metadata = new InputMetadata(
            "nom-api BatchJob ny statlig prosessering",
            "Datavarehus",
            null,
            new Origination("nom-api", "nom-api")
        );

        var cmd = new FjernOrgtilknytning(
            originalOrgtilknytning.navident(),
            originalOrgtilknytning.orgEnhetId(),
            originalOrgtilknytning.gyldigFom(),
            originalOrgtilknytning.gyldigTom(),
            metadata,
            null
        );
        return new SimpleEntry<>(originalOrgtilknytning.fid(), cmd);
    }

    private static InputMetadata getMetadata(OrgtilknytningState originalOrgtilknytning) {
        return getMetadata(originalOrgtilknytning.navident(), originalOrgtilknytning.orgEnhetId());
    }

    private static InputMetadata getMetadata(String navident, String orgenhetId) {
        return new InputMetadata(
            "nom-api BatchJob ny statlig prosessering",
            "Datavarehus",
            Map.of(
                "navident", navident,
                "orgEnhetId", orgenhetId
            ),
            new Origination("nom-api", "nom-api")
        );
    }

    private static OrgtilknytningEvent.AdditionalAgressoData getAdditionalData(
        StillingPeriode stillingPeriode,
        StrukturerteStillingPerioderForNavident sspForNavident
    ) {
        return new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            StillingPeriode.toString(sspForNavident.stillingPerioder())
        );
    }

    private static void matchEksisterendeMotStillingPerioderOld(
        List<StillingPeriode> stillingPerioder,
        List<OrgtilknytningState> eksisterendeOTer,
        Map<StillingPeriode, OrgtilknytningState> matchedeOTer,
        List<StillingPeriode> stillingPerioderAaOpprette
    ) {
        var nestenTreAarSiden = LocalDate.now().minusYears(3).plusMonths(1);
        stillingPerioder.forEach(sp -> {
            var match = eksisterendeOTer.stream()
                .filter(ot -> ot.gyldigFom().equals(sp.gyldigFom()) && ot.orgEnhetId().equals(sp.orgenhet().getId()))
                .findFirst();

            if (match.isEmpty()) {
                // Sjekk om dette er gammel historisk hvor data last med 3 års historikk ikke er tilgjengelig, men NOM har akkumulert denne gamle historikken.
                match = eksisterendeOTer.stream()
                    .filter(ot -> ot.gyldigFom().isBefore(nestenTreAarSiden)
                        && lik(ot.gyldigTom(), sp.gyldigTom())
                        && ot.orgEnhetId().equals(sp.orgenhet().getId())
                    )
                    .findFirst();

                match.ifPresent(eksisterendeOT ->
                    log.info("Fant gammel (3år+) historisk OT for navident {} med gyldigFom {} og gyldigTom {}." +
                            " Vi mathcher mot den selv om gyldigFom ikke matchet StillingPeriode GyldigFom {}",
                        eksisterendeOT.navident(), eksisterendeOT.gyldigFom(), eksisterendeOT.gyldigTom(), sp.gyldigFom()
                ));
            }

            match.ifPresent(ot -> matchedeOTer.put(sp, ot));
            if (match.isEmpty()) {
                stillingPerioderAaOpprette.add(sp);
            }
        });
    }

    private static void matchEksisterendeMotStillingPerioderNew(
        List<StillingPeriode> stillingPerioder,
        List<OrgtilknytningState> eksisterendeOTer,
        Map<StillingPeriode, OrgtilknytningState> matchedeOTer,
        List<StillingPeriode> stillingPerioderAaOpprette
    ) {
        var nestenTreAarSiden = LocalDate.now().minusYears(3).plusMonths(1);
        stillingPerioder.forEach(sp -> {
            var match = eksisterendeOTer.stream()
                .filter(ot -> ot.gyldigFom().equals(sp.gyldigFom())
                    && ot.orgEnhetId().equals(sp.orgenhet().getId())
                    && ot.orgtilknytningType().equals(sp.tilknytningtype()))
                .findFirst();

            var overlappendeOTerPaaSP = harFlereOTerSomOverlapperMedSp(eksisterendeOTer, sp);
            // Håndterer tilfeller hvor flere OT overlapper på SP
            if (overlappendeOTerPaaSP.size() > 1) {
                if (!sp.erDagligOppfolging()) {
                    match = overlappendeOTerPaaSP.stream()
                        .filter(ot -> ot.gyldigFom().equals(sp.gyldigFom())
                            && ot.gyldigTom().orElse(NO_END_DATE).equals(sp.gyldigTom()))
                        .findFirst();
                } else {
                    match = Optional.ofNullable(overlappendeOTerPaaSP.getFirst());
                }
            }

            if (match.isEmpty()) {
                match = eksisterendeOTer.stream()
                    .filter(ot -> ot.gyldigFom().isBefore(nestenTreAarSiden)
                        && ot.gyldigTom().orElse(NO_END_DATE).isAfter(sp.gyldigTom())
                        && ot.erDagligOppfolging() == sp.erDagligOppfolging()
                        && ot.orgtilknytningType().equals(sp.tilknytningtype())
                        && ot.orgEnhetId().equals(sp.orgenhet().getId()))
                    .findFirst();
            }

            if (match.isEmpty()) {
                // Sjekk om dette er gammel historisk hvor data last med 3 års historikk ikke er tilgjengelig, men NOM har akkumulert denne gamle historikken.
                match = eksisterendeOTer.stream()
                    .filter(ot -> ot.gyldigFom().isBefore(nestenTreAarSiden)
                        && lik(ot.gyldigTom(), sp.gyldigTom())
                        && ot.orgEnhetId().equals(sp.orgenhet().getId())
                        && ot.orgtilknytningType().equals(sp.tilknytningtype())
                    )
                    .findFirst();

                match.ifPresent(eksisterendeOT ->
                    log.info("Fant gammel (3år+) historisk OT for navident {} med gyldigFom {} og gyldigTom {}." +
                            " Vi mathcher mot den selv om gyldigFom ikke matchet StillingPeriode GyldigFom {}",
                        eksisterendeOT.navident(), eksisterendeOT.gyldigFom(), eksisterendeOT.gyldigTom(), sp.gyldigFom()
                    ));
            }

            match.ifPresentOrElse(
                ot -> {
                    if (matchedeOTer.values().stream().noneMatch(matchedOT -> matchedOT.fid().equals(ot.fid()))) {
                        matchedeOTer.put(sp, ot);
                    } else {
                        stillingPerioderAaOpprette.add(sp);
                    }
                },
                () -> stillingPerioderAaOpprette.add(sp)
            );
        });
    }

    private static List<OrgtilknytningState> harFlereOTerSomOverlapperMedSp(List<OrgtilknytningState> eksisterendeOTer, StillingPeriode sp) {
        return eksisterendeOTer.stream()
            .filter(ot -> ot.orgEnhetId().equals(sp.orgenhet().getId()))
            .filter(ot -> overlapsInterval(sp.gyldigFom(), sp.gyldigTom(), ot))
            .sorted(Comparator.comparing(OrgtilknytningState::gyldigFom))
            .toList();
    }

    private static List<OrgtilknytningState> getOTerAaFjerne(List<OrgtilknytningState> eksisterendeOTer, Map<StillingPeriode, OrgtilknytningState> matchedeOTer) {
        var nestenTreAarSiden = LocalDate.now().minusYears(3).plusMonths(1);
        return eksisterendeOTer.stream()
            .filter(eksisterende -> eksisterende.gyldigTom().isEmpty() || eksisterende.gyldigTom().get().isAfter(nestenTreAarSiden))
            .filter(eksisterende -> matchedeOTer.values().stream().map(OrgtilknytningState::fid).noneMatch( matchetFid -> eksisterende.fid().equals(matchetFid)))
            .toList();
    }
}
