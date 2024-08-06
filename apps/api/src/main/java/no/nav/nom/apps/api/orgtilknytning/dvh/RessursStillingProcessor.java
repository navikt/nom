package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isCurrentPeriod;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isHistoricPeriod;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging.JA;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging.UKJENT;

@Component
@Slf4j
public class RessursStillingProcessor {
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final RessursStillingLoadStatus ressursStillingLoadStatus;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;
    private ProcessCmdResult processCmdResult = new ProcessCmdResult();
    private HashMap<String, List<String>> evtsrcCommandsPerNavident = new HashMap<>();
    private List<String> manglerSammeEnhetPaStillingOgRessurs = new ArrayList<>();
    private final Unleash unleash;

    public RessursStillingProcessor(
        OrgenhetService orgenhetService, OrgtilknytningService orgtilknytningService, OrgtilknytningCommandService orgtilknytningCommandService,
        RessursStillingLoadStatus ressursStillingLoadStatus,
        RessursStateRepository ressursStateRepository,
        NomMastered nomMastered,
        Unleash unleash
    ) {
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.ressursStillingLoadStatus = ressursStillingLoadStatus;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
        this.unleash = unleash;
    }

    public void process(Map<String, List<StillingPeriode>> stillingPerioderPerNavident) {

        log.debug("Prosesserer stillingPerioder for navidents");

        processCmdResult = new ProcessCmdResult();
        manglerSammeEnhetPaStillingOgRessurs.clear();
        stillingPerioderPerNavident.forEach(this::process);

        log.info(
            "Ferdig prossesert stillingPerioder for {} navidenter. Laget følgene evtSrc commands:{}",
            stillingPerioderPerNavident.size(),
            processCmdResult.summary()
        );

        if (!manglerSammeEnhetPaStillingOgRessurs.isEmpty()) {
            log.error("Disse navidentene har ingen aktiv stilling for samme enhet som på ressurs i Unit4.\n{}", manglerSammeEnhetPaStillingOgRessurs);
        }
    }

    // Prosesserer ett sett med stillingPerioder fra DVH som allerede er slått sammen/splittet på enhet/periode
    public void process(String navident, List<StillingPeriode> stillingPerioder) {
        log.debug(SECURE_LOGS, "Prosesserer stillingPerioder for navident {}, stillingPerioder: {}", navident, stillingPerioder.stream().map(StillingPeriode::toString).collect(Collectors.joining("\n")));

        if (processCmdResult == null) {
            processCmdResult = new ProcessCmdResult();
        }
        Set<String> behandledeOrgtilknytningerFids = new HashSet<>();
        evtsrcCommandsPerNavident.put(navident, new ArrayList<>());

        if (log.isDebugEnabled()) {
            var duplikateCurrent = stillingPerioder.stream().filter(PeriodeUtil::isCurrentPeriod);
            if (duplikateCurrent.count() > 1) {
                log.debug(SECURE_LOGS,
                    "Navid {} har duplikate aktive stillinger:\n{}",
                    navident,
                    StillingPeriode.toString(stillingPerioder.stream().filter(PeriodeUtil::isCurrentPeriod).toList(), true)
                );
            }
        }

        // Enn sålenge skal vi ikke behandle eksterne
        RessursState ressurs = ressursStateRepository.findByNavident(navident).orElse(null);
        if (ressurs == null) {
            String msg = "Navident %s finnes ikke. Behandler ikke DVH ressursStillinger\n%s".formatted(navident, stillingPerioder);
            log.error(msg);
            ressursStillingLoadStatus.addError(navident, msg);
        } else if (ressurs.sektor() == Sektor.EKSTERN) {
            log.debug(SECURE_LOGS, "Navident {} er ekstern. Enn sålenge behandler vi ikke disse", navident);
            return;
        }

        // Hent orgtilknytninger fra State
        var orgtilknytninger = orgtilknytningService.getByNavIdent(navident);
        stillingPerioder.forEach(midlStillingPeriode -> {

            // Ikke behandle stillingperioder som starter etter Ressurs sluttdato.
            // Og bruk Ressurs sluttdato som maks på stillingsperioden sin datoTOM
            StillingPeriode nyStillingPeriode = null;
            if (ressurs != null && ressurs.sluttdato().isPresent()) {
                var ressursSluttdato = ressurs.sluttdato().get();
                if (midlStillingPeriode.gyldigFom().isBefore(ressursSluttdato)) {
                    if (midlStillingPeriode.gyldigTom().isAfter(ressursSluttdato)) {
                        nyStillingPeriode = new StillingPeriode(
                            midlStillingPeriode.stilling(),
                            midlStillingPeriode.gyldigFom(),
                            ressursSluttdato,
                            midlStillingPeriode.orgenhet(),
                            midlStillingPeriode.dagligOppfolging()
                        );
                        log.info(
                            "For navident {} setter vi stillingsperioden\n\t{}\nsin sluttdato til ressursens sluttdato {}.\nNy stillingsPeriode: \n\t{}",
                            navident,
                            midlStillingPeriode,
                            ressursSluttdato,
                            nyStillingPeriode
                        );
                    }
                } else {
                    log.info(
                        "For navident {} så behandler vi ikke stillingsperioden\n{}\n siden denne kommmer etter ressursens sluttdato {}",
                        navident,
                        midlStillingPeriode,
                        ressursSluttdato
                    );
                    return; // Ikke behandle denne stillingperioden. Den starter etter ressurs sin sluttdato
                }
            }
            final var stillingPeriode = nyStillingPeriode != null ? nyStillingPeriode : midlStillingPeriode;

            if (isHistoricPeriod(stillingPeriode)) {  // Historisk stillingPeriode fra DVH - vi lager ingen historiske tilkoblinger. Vi bare evt. avslutter.
                if (orgtilknytninger.size() == 0) {
                    log.debug(SECURE_LOGS,
                        "Navident {} har historisk stillingPeriode fra DVH ({}) men NOM har ingen orgtilknytninger for ident {}",
                        navident,
                        stillingPeriode,
                        navident
                    );
                } else {
                    // Finn orgtilknytninger på matchende datoperiode
                    var sammePeriode = orgtilknytninger.stream().filter(ot -> isMatchingPeriod(ot, stillingPeriode)).toList();
                    // Valider om noen av disse matcher på agressoId - ok om stillingperiode ikke matcher til 7 sifret, vi tar det for god fisk uansett
                    var matcherAgressoId = sammePeriode.stream().filter(ot -> orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();
                    if (matcherAgressoId.size() == 1) {
                        // Historikken stemmer - Sjekk dagligOppfølging og move on
                        behandledeOrgtilknytningerFids.add(matcherAgressoId.stream().findFirst().get().fid());
                    } else if (matcherAgressoId.size() > 0) {
                        String msg = "Navident %s har historisk stillingPeriode fra DVH (%s) matcher flere orgtilknytninger %s".formatted(
                            navident,
                            stillingPeriode,
                            matcherAgressoId
                        );
                        log.warn(msg);
                        ressursStillingLoadStatus.addError(navident, msg);
                        // Burde ikke vært her - men vi markerer alle disse tidligere orgtilknytningene som behandlet
                        behandledeOrgtilknytningerFids.addAll(matcherAgressoId.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                    } else {
                        // Har en historisk stillingPeriode fra DVH, men finner ikke tilsvarende historisk orgtilknytning i state på både periode og agressoId
                        if (sammePeriode.size() > 0) {
                            // Vi finner minst en orgtilknytning for samme historisk periode
                            // Da tar vi State historikken for god fisk.
                            behandledeOrgtilknytningerFids.addAll(sammePeriode.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                        } else {
                            // Nå derimot kan det hende vi må avslutte en aktiv orgtilknytning, eller endre gyldigTOM
                            var matcherStartDatoOgAgressoId = orgtilknytninger.stream()
                                .filter(ot -> stillingPeriode.gyldigFom().equals(ot.gyldigFom()) && orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot))
                                .toList();
                            if (matcherStartDatoOgAgressoId.size() == 0) {
                                // Matcher ingen i NOM, DVH historisk finnes ikke i NOM, så da behandler vi den ikke.
                                // TODO: Har vi kommet til ett sted hvor gyldigFOM på historisk periode kan ha endret seg i DVH?
                                log.debug(SECURE_LOGS,
                                    "Navident {} har historisk stillingPeriode fra DVH ({}) matcher ingen orgtilknytninger {}",
                                    navident,
                                    stillingPeriode,
                                    matcherAgressoId
                                );
                                // Løsn forslag:
                                // opprettOrgtilknytning does not set gyldigTom
                                // settOrgtilknytningSluttDato sets gyldigTom per stillingPeriode
                                String fid = lagNyOrgtilknytning(navident, stillingPeriode.orgenhet().getId(), stillingPeriode, StillingPeriode.toString(stillingPerioder));

                                // testing tracking
                                log.info("Calling endreOrgtilknytningGyldigTOM from process 10 " + fid);
                                settOrgtilknytningSluttDato(fid, navident, stillingPeriode, StillingPeriode.toString(stillingPerioder));

                                // historisk og -ve OT, fjern
                                if(fid !=null && !fid.isBlank() && stillingPeriode.gyldigTom().isBefore(stillingPeriode.gyldigFom())){
                                    //fjernNegativeOrgtilknytning(fid, stillingPeriode);
                                    var orgtilknytning = orgtilknytningService.findByFid(fid);
                                    log.info("Fjerner negativ orgtilknytning {} {} {}", fid, orgtilknytning, stillingPeriode);
                                    //fjernNegativeOrgtilknytning(fid, navident, stillingPeriode.orgenhet().getId(), stillingPeriode.gyldigFom(), Optional.of(stillingPeriode.gyldigTom()));
                                }
                            } else if (matcherStartDatoOgAgressoId.size() > 1) {
                                // Matcher flere i NOM - dette må være feil!....
                                String msg = "Navident %s har historisk stillingPeriode fra DVH (%s) matcher flere orgtilknytninger %s".formatted(
                                    navident,
                                    stillingPeriode,
                                    matcherAgressoId
                                );
                                log.warn(msg);
                                ressursStillingLoadStatus.addError(navident, msg);
                                // Burde ikke vært her - men vi markerer alle disse tidligere orgtilknytningene som behandlet
                                behandledeOrgtilknytningerFids.addAll(matcherStartDatoOgAgressoId.stream()
                                    .map(OrgtilknytningState::fid)
                                    .collect(Collectors.toSet()));
                            } else {
                                // testing tracking
                                log.info("Calling endreOrgtilknytningGyldigTOM from process 1");
                                // Vi matcher 1. Og trenger å endre GyldigTOM for denne -> Enten korriger, eller avvikle
                                endreOrgtilknytningGyldigTOM(
                                    matcherStartDatoOgAgressoId.get(0),
                                    stillingPeriode,
                                    StillingPeriode.toString(stillingPerioder)
                                );
                                behandledeOrgtilknytningerFids.addAll(matcherStartDatoOgAgressoId.stream()
                                    .map(OrgtilknytningState::fid)
                                    .collect(Collectors.toSet()));
                            }
                        }
                    }
                }
            } else if (isCurrentPeriod(stillingPeriode)) {
                // DVH Perioden er aktiv i dag

                // Finn aktiv orgtilknytning på matchende enhet
                var aktiveOrgtilknytninger = orgtilknytninger.stream().filter(PeriodeUtil::isCurrentPeriod);
                // Valider om noen av disse matcher på agressoId - ok om stillingperiode ikke matcher til 7 sifret, vi tar det for god fisk uansett
                var matcherAgressoId = aktiveOrgtilknytninger.filter(ot -> orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();

                // Aktiv periode kan sjekkes for primær tilkynting
                boolean erDagligOppfolging = stillingPeriode.dagligOppfolging() == JA;

                if (matcherAgressoId.size() > 1) {
                    String msg = "Navident %s har aktiv stillingPeriode i DVH\n  %s\n som matcher flere aktive orgtilknytninger i state\n  %s"
                        .formatted(navident, stillingPeriode, matcherAgressoId);
                    log.error(msg);
                    ressursStillingLoadStatus.addError(navident, msg);
                    matcherAgressoId = fjernDuplikater(matcherAgressoId);
                    if (matcherAgressoId.size() > 1) {
                        // Er kanskje ren bytte av enhet på dato - ta vekk den som ikke matcher orgenhet id, men som har samme datoperiode
                        // Vil etter det ikke inngå i behandledeOrgtilknytningerFids - og skal da følgelig fjernes
                        var fjerne = matcherAgressoId.stream().filter(ot ->
                            !ot.orgEnhetId().equals(stillingPeriode.orgenhet().getId()) &&
                                ot.gyldigFom().equals(stillingPeriode.gyldigFom()) &&
                                ot.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())
                        ).toList();
                        log.debug(SECURE_LOGS,
                            "For navident {} fant vi følgende orgtilknytning(er)\n {}\nsom er duplikat på aktiv periode, men for annen enhet enn DVH stillingPeriode\n {}\n. Denne/disse vil fjernes",
                            navident,
                            fjerne,
                            stillingPeriode
                        );
                        matcherAgressoId.removeAll(fjerne);
                    }
                }
                if (matcherAgressoId.size() > 1) {
                    String msg = "Selv etter forsøk på å fjerne duplikat tilknytninger så har Navident %s har stillingPeriode i DVH\n  %s\n som matcher flere aktive orgtilknytninger i state\n  %s"
                        .formatted(navident, stillingPeriode, matcherAgressoId);
                    log.error(msg);
                    log.error("Dette kan ikke behandles (det er forsøkt i fjernDuplikater over)");
                    ressursStillingLoadStatus.addError(navident, msg);
                } else if (matcherAgressoId.size() == 0) {
                    log.debug(SECURE_LOGS,
                        "Navident {} har aktiv stillingPeriode i DVH\n  {}\n som ikke finnes i orgtilknytning state. Behandler denne som ny.",
                        navident,
                        stillingPeriode
                    );

                    String fid = lagNyOrgtilknytning(navident, stillingPeriode.orgenhet().getId(), stillingPeriode, StillingPeriode.toString(stillingPerioder));
                    if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {
                        // testing tracking
                        log.info("Calling endreOrgtilknytningGyldigTOM from process 11 " + fid);
                        settOrgtilknytningSluttDato(fid, navident, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                    }
                    if (erDagligOppfolging) {
                        endreOrgtilknytningDagligOppfolging(fid, navident, true, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                    }
                } else {
                    OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                    if (!orgtilknytning.orgEnhetId().equals(stillingPeriode.orgenhet().getId())) {
                        log.debug(SECURE_LOGS,
                            "Navident {} har aktiv stillingPeriode i DVH\n  {}\n som matcher en aktiv orgtilknytning i state\n  {}\n  på agressoId, men ikke på enhetsId",
                            navident,
                            stillingPeriode,
                            matcherAgressoId
                        );
                        // Behandle denne som ny tilkobling - og la evt. opprydding fjerne eller endre datoer på den gamle
                        String fid = lagNyOrgtilknytning(
                            navident,
                            stillingPeriode.orgenhet().getId(),
                            stillingPeriode,
                            StillingPeriode.toString(stillingPerioder)
                        );
                        if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {

                            // testing tracking
                            log.info("Calling endreOrgtilknytningGyldigTOM from process 12 ");
                            settOrgtilknytningSluttDato(fid, navident, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                        }
                    } else {
                        log.debug(SECURE_LOGS,
                            "Navident {} har aktiv stillingPeriode i DVH\n  {}\n som matcher en aktiv orgtilknytning i state\n  {}\n Sjekker denne for dato endringer og evt. endring av primær",
                            navident,
                            stillingPeriode,
                            matcherAgressoId
                        );
                        if (!orgtilknytning.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())) {
                            // testing tracking
                            log.info("Calling endreOrgtilknytningGyldigTOM from process 2 " + orgtilknytning.fid());
                            endreOrgtilknytningGyldigTOM(orgtilknytning, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                        }
                        if (!orgtilknytning.gyldigFom().equals(stillingPeriode.gyldigFom())) {
                            endreOrgtilknytningGyldigFOM(orgtilknytning, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                        }
                    }
                    if (erDagligOppfolging != orgtilknytning.erDagligOppfolging() && stillingPeriode.dagligOppfolging() != UKJENT) {
                        endreOrgtilknytningDagligOppfolging(
                            orgtilknytning.fid(),
                            navident,
                            erDagligOppfolging,
                            stillingPeriode,
                            StillingPeriode.toString(stillingPerioder)
                        );
                    }
                }

                behandledeOrgtilknytningerFids.addAll(matcherAgressoId.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));
            } else {
                // Perioden er fremtidig
                // Hvis fremtidig DVH stillingPeriode har endret enhetId ift hva som tidligere er lest inn til Orgtilknytning state - så ikke behandle Orgtilknytning staten (da fjernes den), og DVH stillingPerioden behandles som helt ny.

                // Finn fremtidige orgtilknytninger på matchende enhet
                var aktiveOrgtilknytninger = orgtilknytninger.stream().filter(PeriodeUtil::isFuturePeriod);
                // Valider om noen av disse matcher på agressoId - ok om stillingPeriode.enhetId ikke matcher til 7 sifret, vi tar det for god fisk uansett
                var matcherAgressoId = aktiveOrgtilknytninger.filter(ot -> orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();

                // Fremtidig periode kan sjekkes for primær tilkynting
                boolean erDagligOppfolging = stillingPeriode.dagligOppfolging() == JA;

                // Finn så i matcherAgressoId om det er match på fra-til dato.
                var matcherIdOgPeriode = matcherAgressoId.stream().filter(ot -> isMatchingPeriod(ot, stillingPeriode)).toList();
                if (matcherIdOgPeriode.size() == 0) {
                    // Vi har ingen direkte match på både start/sluttdato for agressoId

                    // Prøv så å matche på partial date - samme startdato først
                    var matcherIdOgGyldigFom = matcherAgressoId.stream().filter(ot -> ot.gyldigFom().equals(stillingPeriode.gyldigFom())).toList();
                    if (matcherIdOgGyldigFom.size() > 0) {
                        if (matcherIdOgGyldigFom.size() > 1) {
                            // Vi har flere orgtilknytninger i state som har samme fra og med dato og agressoId som stillignPeriode.
                            String msg = "Navident %s har fremtidig stillingPeriode i DVH\n  %s\n som matcher enhetId og GyldigFom for flere fremtidige orgtilknytninger i state\n  %s\nDette kan ikke behandles"
                                .formatted(navident, stillingPeriode, matcherIdOgGyldigFom);
                            log.error(msg);
                            // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                            ressursStillingLoadStatus.addError(navident, msg);
                        } else {
                            log.debug(SECURE_LOGS,
                                "Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher en fremtidig orgtilknytning i state\n  {}\n Sjekker denne for gyldig fra dato endringer og evt. endring av primær",
                                navident,
                                stillingPeriode,
                                matcherAgressoId
                            );
                            OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                            if (!orgtilknytning.gyldigFom().equals(stillingPeriode.gyldigFom())) {
                                endreOrgtilknytningGyldigFOM(orgtilknytning, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                            }
                            if (erDagligOppfolging != orgtilknytning.erDagligOppfolging() && stillingPeriode.dagligOppfolging() != UKJENT) {
                                endreOrgtilknytningDagligOppfolging(
                                    orgtilknytning.fid(),
                                    navident,
                                    erDagligOppfolging,
                                    stillingPeriode,
                                    StillingPeriode.toString(stillingPerioder)
                                );
                            }
                        }
                    } else {
                        var matcherIdOgGyldigTom = matcherAgressoId.stream()
                            .filter(ot -> ot.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom()))
                            .toList();
                        if (matcherIdOgGyldigTom.size() > 0) {
                            if (matcherIdOgGyldigTom.size() > 1) {
                                String msg = "Navident %s har fremtidig stillingPeriode i DVH\n  %s\n som matcher enhetId og GyldigTom flere fremtidige orgtilknytninger i state\n  %s\nDette kan ikke behandles"
                                    .formatted(navident, stillingPeriode, matcherIdOgGyldigTom);
                                // Vi har flere orgtilknytninger i state som har samme til og med dato og agressoId som stillignPeriode.
                                log.error(msg);
                                // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                                ressursStillingLoadStatus.addError(navident, msg);
                            } else {
                                log.debug(SECURE_LOGS,
                                    "Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher en fremtidig orgtilknytning i state\n  {}\n Sjekker denne for gyldig til dato endringer og evt. endring av primær",
                                    navident,
                                    stillingPeriode,
                                    matcherAgressoId
                                );
                                OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                                if (!orgtilknytning.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())) {
                                    // testing tracking
                                    log.info("Calling endreOrgtilknytningGyldigTOM from process 3");
                                    endreOrgtilknytningGyldigTOM(orgtilknytning, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                                }
                                if (erDagligOppfolging != orgtilknytning.erDagligOppfolging() && stillingPeriode.dagligOppfolging() != UKJENT) {
                                    endreOrgtilknytningDagligOppfolging(
                                        orgtilknytning.fid(),
                                        navident,
                                        erDagligOppfolging,
                                        stillingPeriode,
                                        StillingPeriode.toString(stillingPerioder)
                                    );
                                }
                            }
                        } else {
                            log.debug(SECURE_LOGS,
                                "Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som ikke finnes i orgtilknytning state. Behandler denne som ny.",
                                navident,
                                stillingPeriode
                            );

                            String fid = lagNyOrgtilknytning(
                                navident,
                                stillingPeriode.orgenhet().getId(),
                                stillingPeriode,
                                StillingPeriode.toString(stillingPerioder)
                            );
                            if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {

                                // testing tracking
                                log.info("Calling endreOrgtilknytningGyldigTOM from process 13 " + fid);
                                settOrgtilknytningSluttDato(fid, navident, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                            }
                            if (erDagligOppfolging) {
                                endreOrgtilknytningDagligOppfolging(fid, navident, true, stillingPeriode, StillingPeriode.toString(stillingPerioder));
                            }
                        }
                    }
                } else if (matcherIdOgPeriode.size() > 1) {
                    // Vi har flere orgtilknytninger i state som har samme dato periode og agressoId som stillingPeriode.
                    String msg = "Navident %s har fremtidig stillingPeriode i DVH\n  %s\n som matcher enhetId og periode for flere fremtidige orgtilknytninger i state\n  %s\nDette kan ikke behandles".formatted(
                        navident,
                        stillingPeriode,
                        matcherIdOgPeriode
                    );
                    log.warn(msg);
                    ressursStillingLoadStatus.addError(navident, msg);
                    // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                } else {
                    // Vi har en presis match på stillingPeriode og Orgtilknytning state
                    log.debug(SECURE_LOGS,
                        "Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og periode for en fremtidige orgtilknytninger i state\n  {}\nSjekker evt endring på primær",
                        navident,
                        stillingPeriode,
                        matcherIdOgPeriode
                    );
                    OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);
                    if (orgtilknytning.erDagligOppfolging() != erDagligOppfolging && stillingPeriode.dagligOppfolging() != UKJENT) {
                        endreOrgtilknytningDagligOppfolging(
                            orgtilknytning.fid(),
                            navident,
                            erDagligOppfolging,
                            stillingPeriode,
                            StillingPeriode.toString(stillingPerioder)
                        );
                    }
                }
                behandledeOrgtilknytningerFids.addAll(matcherAgressoId.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));
            }
        });
        // Sjekk til slutt om det er noen orgtilknytninger i state som ikke er 'behandlet'. De finnes da ikke i DVH mer, og må fjernes
        orgtilknytninger.stream()
            .filter(ot -> !behandledeOrgtilknytningerFids.contains(ot.fid()))
            .forEach(ot -> {
                log.info("DVH har ikke denne orgtilknytningen mer, så vi fjerner den: {}", ot);
                fjernOrgtilknytning(ot);
            });

        loggOppsummering(navident, stillingPerioder, orgtilknytninger);

        // Sjekk om det finnes en current stillingPeriode hvor stilling og ressurs har samme enhet
        if (!validAgrIdStillingVsRessurs(stillingPerioder)) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "Navident {} har ingen gjeldende stilling med samme agressoId som ressursen:\n{}",
                    navident,
                    StillingPeriode.toString(stillingPerioder.stream().filter(PeriodeUtil::isCurrentPeriod).toList())
                );
            }
            manglerSammeEnhetPaStillingOgRessurs.add(navident);
        }
    }

    private boolean validAgrIdStillingVsRessurs(List<StillingPeriode> stillingPerioder) {
        if (stillingPerioder.stream().anyMatch(PeriodeUtil::isCurrentPeriod)) {
            return stillingPerioder.stream()
                .filter(PeriodeUtil::isCurrentPeriod)
                .anyMatch(StillingPeriode::harSammeAgressoIdPaStillingOgRessurs);
        }
        return true;
    }

    private List<OrgtilknytningState> fjernDuplikater(List<OrgtilknytningState> matcherAgressoId) {
        OrgtilknytningState toKeep = matcherAgressoId.get(0);
        List<OrgtilknytningState> statesToKeep = new ArrayList<>(List.of(toKeep));
        matcherAgressoId.stream().skip(1).forEach(ot -> {
            // Make sure they are indeed duplicates
            if (ot.navident().equals(toKeep.navident())
                && ot.orgEnhetId().equals(toKeep.orgEnhetId())
                && ot.gyldigFom().equals(toKeep.gyldigFom())
                && (ot.gyldigTom().isPresent() ? ot.gyldigTom().get().equals(toKeep.gyldigTom().orElse(null)) : toKeep.gyldigTom().isEmpty())) {
                // Plain duplicate - remove
                fjernOrgtilknytning(ot);
                log.info("Fjerner duplikat orgtilknytning for Navident {}: {}", ot.navident(), ot);
            } else {
                // There is a difference? Cannot remove possible overlapping dates, but not equal?
                statesToKeep.add(ot);
            }
        });
        return statesToKeep;
    }

    private void fjernNegativeOrgtilknytning(String fid, StillingPeriode stillingPeriode) {
        var orgtilknytning = orgtilknytningService.findByFid(fid);
        log.info("Fjerner negativ orgtilknytning {} {} {}", fid, orgtilknytning, stillingPeriode);
        orgtilknytning.ifPresent(ot -> {
            log.info("Fjerner negativ orgtilknytning finnes for {}: {}", fid, ot);
            if(ot.gyldigTom().isPresent() && ot.gyldigTom().get().isBefore(ot.gyldigFom())) {
                log.info("Fjerner negativ orgtilknytning for Navident {}: {}", ot.navident(), ot);
                fjernOrgtilknytning(ot);
            }
        });
    }

    public boolean isMatchingPeriod(OrgtilknytningState orgtilknytningState, StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigFom().equals(orgtilknytningState.gyldigFom())
            && stillingPeriode.gyldigTom().equals(orgtilknytningState.gyldigTom().orElse(NO_END_DATE));
    }

    public boolean erStillingPeriodenDagligOppfolging(StillingPeriode stillingPeriode) {
        // Er forventet at dette kun spørres på for en aktiv eller fremtidig stilling. Men sjekkes ikke
        var stillingEnhetId = stillingPeriode.stilling().agressoId();
        var ressursEnhetId = stillingPeriode.stilling().ressursAgressoId();

        if (stillingEnhetId.isEmpty() || ressursEnhetId.isEmpty()) {
            return true; // Hvis enten enhet på stilling mangler, eller enhet på ressurs mangler - går vi ut ifra at dette er primær stillingen.
        }

        return stillingEnhetId.get().startsWith(ressursEnhetId.get()) || ressursEnhetId.get().startsWith(stillingEnhetId.get());
    }


    /**
     * Vil matche på overnode-agressoId
     * Kan brukes på historiske rader, hvor DVH kan angi overnode og ikke løv-node (ORGENHET)
     * Altså en stillingPeriode med angivelse av f.eks. ORGNIV3 node '060200' vil da matche en orgtilknytning i nom for ORGENHET '0602002'
     */
    public boolean orgtilknytningEnhetMatcherAgressoId(StillingPeriode stillingPeriode, OrgtilknytningState ot) {
        if (stillingPeriode.orgenhet() != null) { // Hvis vi har en ORGENHET på stillingPeriode, så matcher vi mot denne
            log.debug(SECURE_LOGS, "Navident {} har orgenhet på stillingPeriode fra stillingStrukturerer og bruker denne. {}", ot.navident(), stillingPeriode);
            return ot.orgEnhetId().equals(stillingPeriode.orgenhet().getId());
        }
        log.debug(SECURE_LOGS, "Navident {} har ikke orgenhet på stillingPeriode og må slå opp på agressoid", ot.navident(), stillingPeriode);
        if (isCurrentPeriod(stillingPeriode)) {
            // For aktiv periode, match mot stilling sin AgressoId, men hvis denne er null - match mot ressursens agressso ID.
            return orgenhetService.getById(
                ot.orgEnhetId()).get().getAgressoId().startsWith(
                stillingPeriode.stilling().agressoId().orElse(
                    stillingPeriode.stilling().ressursAgressoId().orElse("NULL")
                )
            );
        } else {
            return orgenhetService.getById(ot.orgEnhetId()).get().getAgressoId().startsWith(stillingPeriode.stilling().agressoId().orElse("NULL"));
        }
    }

    public String lagNyOrgtilknytning(String navident, String orgenhetId, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        if (nomMastered.isMastered(navident)) {
            log.info("Navident {} er mastered. Vi lager ikke orgtilknytning for denne", navident);
            return null;
        }

        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        var command = new OpprettOrgtilknytning(
            navident,
            orgenhetId,
            stillingPeriode.gyldigFom(),
            metadata,
            additionalData
        );

        loggCommand(navident, command);

        var orgtilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(command);

        if (ressursStateRepository.findByNavident(navident).get().isKommunal()) {
            orgtilknytningCommandService.endreTilknytningtype(
                EndreTilknytningtypeCommand.builder()
                    .fid(orgtilknytningFid)
                    .tilknytningtype(Optional.of(Tilknytningtype.INGEN))
                    .inputMetadata(metadata)
                    .build()
            );
        }

        return orgtilknytningFid;
    }

    public void endreOrgtilknytningGyldigFOM(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        if (nomMastered.isMastered(originalOrgtilknytning.navident())) {
            log.info("Navident {} er mastered. Vi endrer ikke orgtilknytning for denne", originalOrgtilknytning.navident());
            return;
        }

        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata(
            "nom-api BatchJob",
            "Datavarehus",
            Map.of(
                "navident", originalOrgtilknytning.navident(),
                "orgEnhetId", originalOrgtilknytning.orgEnhetId()
            ),
            new Origination("nom-api", "nom-api")
        );

        var command = new EndreOrgtilknytningDato(
            originalOrgtilknytning.navident(),
            originalOrgtilknytning.orgEnhetId(),
            stillingPeriode.gyldigFom(),
            originalOrgtilknytning.gyldigTom(),
            metadata,
            additionalData
        );

        loggCommand(originalOrgtilknytning.navident(), originalOrgtilknytning.fid(), command);

        orgtilknytningCommandService.endreOrgtilknytningDato(originalOrgtilknytning.fid(), command);
    }

    public void endreOrgtilknytningGyldigTOM(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        if (nomMastered.isMastered(originalOrgtilknytning.navident())) {
            log.info("Navident {} er mastered. Vi endrer ikke orgtilknytning for denne", originalOrgtilknytning.navident());
            return;
        }

        if (originalOrgtilknytning.gyldigFom().plusDays(1).isAfter(stillingPeriode.gyldigTom())) {
            log.error(
                "Endring av sluttdato på orgtilknytning for Navident {} er ikke gyldig. Sluttdato må være etter startdato",
                originalOrgtilknytning.navident()
            );
            return;
        }

        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata(
            "nom-api BatchJob",
            "Datavarehus",
            Map.of(
                "navident", originalOrgtilknytning.navident(),
                "orgEnhetId", originalOrgtilknytning.orgEnhetId()
            ),
            new Origination("nom-api", "nom-api")
        );

        if (originalOrgtilknytning.gyldigTom().isPresent()) {
            var command = new EndreOrgtilknytningDato(
                originalOrgtilknytning.navident(),
                originalOrgtilknytning.orgEnhetId(),
                originalOrgtilknytning.gyldigFom(),
                stillingPeriode.gyldigTom().isEqual(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom()),
                metadata,
                additionalData
            );

            loggCommand(originalOrgtilknytning.navident(), originalOrgtilknytning.fid(), command);

            orgtilknytningCommandService.endreOrgtilknytningDato(originalOrgtilknytning.fid(), command);
        } else {
            var command = new AvvikleOrgtilknytning(
                originalOrgtilknytning.navident(),
                originalOrgtilknytning.orgEnhetId(),
                stillingPeriode.gyldigTom(),
                metadata,
                additionalData
            );

            // testing tracking
            log.info("Calling loggCommand from endreOrgtilknytningGyldigTOM");
            loggCommand(originalOrgtilknytning.navident(), originalOrgtilknytning.fid(), command);

            orgtilknytningCommandService.avvikleOrgtilknytning(originalOrgtilknytning.fid(), command);
        }
    }

    public void settOrgtilknytningSluttDato(String fid, String navident, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        if (nomMastered.isMastered(navident)) {
            log.info("Navident {} er mastered. Vi endrer ikke orgtilknytning for denne", navident);
            return;
        }

        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        var command = new AvvikleOrgtilknytning(
            navident,
            stillingPeriode.orgenhet().getId(),
            stillingPeriode.gyldigTom(),
            metadata,
            additionalData
        );

        // testing tracking
        log.info("Calling loggCommand from settOrgtilknytningSluttDato");
        loggCommand(navident, fid, command);

        orgtilknytningCommandService.avvikleOrgtilknytning(fid, command);
    }

    private void endreOrgtilknytningDagligOppfolging(
        String fid,
        String navident,
        boolean erDagligOppfolging,
        StillingPeriode stillingPeriode,
        String stillingPerioderAsString
    ) {
        if (nomMastered.isMastered(navident)) {
            log.info("Navident {} er mastered. Vi endrer ikke Daglig oppfølging for denne", navident);
            return;
        }

        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        var command = new EndreDagligOppfolgingOrgtilknytning(
            navident,
            stillingPeriode.orgenhet().getId(),
            stillingPeriode.gyldigFom(),
            stillingPeriode.gyldigTom().equals(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom()),
            erDagligOppfolging,
            metadata,
            additionalData
        );

        loggCommand(navident, fid, command);

        orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(fid, command);
    }

    public void fjernOrgtilknytning(OrgtilknytningState originalOrgtilknytning) {
        if (nomMastered.isMastered(originalOrgtilknytning.navident())) {
            log.info("Navident {} er mastered. Vi endrer ikke orgtilknytning for denne", originalOrgtilknytning.navident());
            return;
        }

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        var command = new FjernOrgtilknytning(
            originalOrgtilknytning.navident(),
            originalOrgtilknytning.orgEnhetId(),
            originalOrgtilknytning.gyldigFom(),
            originalOrgtilknytning.gyldigTom(),
            metadata,
            null
        );

        loggCommand(originalOrgtilknytning.navident(), originalOrgtilknytning.fid(), command);

        orgtilknytningCommandService.fjernOrgtilknytning(originalOrgtilknytning.fid(), command);
    }

    public void fjernNegativeOrgtilknytning(String fid, String navident, String orgenhetId, LocalDate gyldigFom, Optional<LocalDate> gyldigTom) {
        /*
        if (nomMastered.isMastered(navident)) {
            log.info("Navident {} er mastered. Vi endrer ikke orgtilknytning for denne", navident);
            return;
        }
        */

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        var command = new FjernOrgtilknytning(
            navident,
            orgenhetId,
            gyldigFom,
            gyldigTom,
            metadata,
            null
        );

        //loggCommand(navident, fid, command);

        log.warn("Fjerner negativ orgtilknytning fid: {}, navident: {}, orgenhet: {}, gyldigFom: {}, gyldigTom: {}", fid, navident, orgenhetId, gyldigFom, gyldigTom.orElse(null));
        //orgtilknytningCommandService.fjernOrgtilknytning(fid, command);
    }

    private void loggOppsummering(String navident, List<StillingPeriode> stillingPerioder, Collection<OrgtilknytningState> orgtilknytninger) {
        if (log.isDebugEnabled())
        {
            StringBuffer strbuf = new StringBuffer();
            strbuf.append("Oppsummering for navident %s".formatted(navident));
            strbuf.append("\n\tAntall DVH ressursStillinger: %s".formatted(stillingPerioder.size()));
            strbuf.append("\n\t\tHistoriske: %s".formatted(stillingPerioder.stream().filter(PeriodeUtil::isHistoricPeriod).count()));
            strbuf.append("\n\t\tAktive: %s".formatted(stillingPerioder.stream().filter(PeriodeUtil::isCurrentPeriod).count()));
            strbuf.append("\n\t\tFremtidige: %s".formatted(stillingPerioder.stream().filter(PeriodeUtil::isFuturePeriod).count()));
            strbuf.append("\n\tAntall eksisterende orgtilknytninger: %s".formatted(orgtilknytninger.size()));
            strbuf.append("\n\t\tHistoriske: %s".formatted(orgtilknytninger.stream().filter(PeriodeUtil::isHistoricPeriod).count()));
            strbuf.append("\n\t\tAktive: %s".formatted(orgtilknytninger.stream().filter(PeriodeUtil::isCurrentPeriod).count()));
            strbuf.append("\n\t\tFremtidige: %s".formatted(orgtilknytninger.stream().filter(PeriodeUtil::isFuturePeriod).count()));

            strbuf.append("\n\t===> Har laget følgende evtsrc hendelser:");
            if (evtsrcCommandsPerNavident.get(navident).size() == 0) {
                strbuf.append("\n\t\tIngen");
            } else {
                evtsrcCommandsPerNavident.get(navident).forEach(strbuf::append);
            }
            log.debug(
                strbuf.toString());
        }

        evtsrcCommandsPerNavident = new HashMap<>();
    }

    private void loggCommand(String navident, OpprettOrgtilknytning cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident).add("\n\t\tOpprettOrgtilknytning for enhet %s gyldigFom %s".formatted(cmd.orgenhetId(), cmd.gyldigFom()));
        }
        processCmdResult.numOpprett++;
    }

    private void loggCommand(String navident, String fid, AvvikleOrgtilknytning cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident)
                .add("\n\t\tAvvikleOrgtilknytning for fid %s enhet %s gyldigTom %s".formatted(fid, cmd.orgenhetId(), cmd.gyldigTom()));
        }
        processCmdResult.numAvviklet++;
    }

    private void loggCommand(String navident, String fid, EndreOrgtilknytningDato cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident)
                .add("\n\t\tAvvikleOrgtilknytning for fid %s enhet %s gyldigFom %s - gyldigTom %s".formatted(fid,
                    cmd.orgenhetId(),
                    cmd.gyldigFom(),
                    cmd.gyldigTom()
                ));
        }
        processCmdResult.numKorrigertDato++;
    }

    private void loggCommand(String navident, String fid, EndreDagligOppfolgingOrgtilknytning cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident)
                .add("\n\t\tEndreDagligOppfolgingOrgtilknytning for fid %s enhet %s gyldigFom %s - gyldigTom %s => %s".formatted(fid,
                    cmd.orgenhetId(),
                    cmd.gyldigFom(),
                    cmd.gyldigTom(),
                    cmd.dagligOppfolging()
                ));
        }
        processCmdResult.numEndretDagligOppfolging++;
    }

    private void loggCommand(String navident, String fid, FjernOrgtilknytning cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident)
                .add("\n\t\tFjernOrgtilknytning for fid %s enhet %s gyldigFom %s - gyldigTom %s".formatted(fid,
                    cmd.orgenhetId(),
                    cmd.gyldigFom(),
                    cmd.gyldigTom()
                ));
        }
        processCmdResult.numFjernet++;
    }

    public class ProcessCmdResult {
        int numOpprett;
        int numAvviklet;
        int numEndretDagligOppfolging;
        int numKorrigertDato;
        int numFjernet;

        public String summary() {
            StringBuffer strbuf = new StringBuffer();
            strbuf.append("\n\tOpprettOrgtilknytning:               %6s".formatted(numOpprett));
            strbuf.append("\n\tAvvikletOrgtilknytning:              %6s".formatted(numAvviklet));
            strbuf.append("\n\tEndreDagligOppfolgingOrgtilknytning: %6s".formatted(numEndretDagligOppfolging));
            strbuf.append("\n\tKorrigerOrgtilknytningDato:          %6s".formatted(numKorrigertDato));
            strbuf.append("\n\tFjernOrgtilknytning:                 %6s".formatted(numFjernet));
            return strbuf.toString();
        }
    }
}
