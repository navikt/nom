package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.KorrigerOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isCurrentPeriode;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isHistoricPeriod;

@Component
@Slf4j
public class RessursStillingProcessor {
    private final IOrgenhetService orgenhetService;
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final IRessursService ressursService;
    private ProcessCmdResult processCmdResult;
    private HashMap<String, List<String>> evtsrcCommandsPerNavident = new HashMap<>();

    public RessursStillingProcessor(IOrgenhetService orgenhetService, OrgtilknytningStateService orgtilknytningStateService, OrgtilknytningCommandService orgtilknytningCommandService,
                                    IRessursService ressursService
    ) {
        this.orgenhetService = orgenhetService;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.ressursService = ressursService;
    }

    public void process(Map<String, List<StillingPeriode>> stillingPerioderPerNavident) {
        processCmdResult = new ProcessCmdResult();
        stillingPerioderPerNavident.forEach(this::process);

        log.debug("Ferdig prossesert stillingPerioder for {} navidenter. Laget følgene evtSrc commands:{}", stillingPerioderPerNavident.size(), processCmdResult.summary());
    }

    // Prosesserer ett sett med stillingPerioder fra DVH som allerede er slått sammen/splittet på enhet/periode
    public void process(String navident, List<StillingPeriode> stillingPerioder) {
        if (processCmdResult == null) {
            processCmdResult = new ProcessCmdResult();
        }
        Set<String> behandledeOrgtilknytningerFids = new HashSet<>();
        evtsrcCommandsPerNavident.put(navident, new ArrayList<>());

        if (log.isDebugEnabled()) {
            var duplikateCurrent = stillingPerioder.stream().filter(PeriodeUtil::isCurrentPeriode);
            if (duplikateCurrent.count() > 1) {
                log.debug(
                    "Navid {} har duplikate aktive stillinger:\n{}",
                    navident,
                    StillingPeriode.stillingperioderToString(stillingPerioder.stream().filter(PeriodeUtil::isCurrentPeriode).toList(), true)
                );
            }
        }

        // Enn sålenge skal vi ikke behandle eksterne
        RessursState ressurs = ressursService.findByNavident(navident).orElse(null);
        if (ressurs == null) {
            log.error("Navident {} finnes ikke. Behandler ikke DVH ressursStillinger", navident);
        } else if (ressurs.sektor() == Sektor.EKSTERN) {
            log.debug("Navident {} er ekstern. Enn sålenge behandler vi ikke disse", navident);
            return;
        }

        // Hent orgtilknytninger fra State
        var orgtilknytninger = orgtilknytningStateService.getOrgtilknytningerByNavIdent(navident);
        stillingPerioder.forEach( stillingPeriode -> {
            if (isHistoricPeriod(stillingPeriode)) {  // Historisk stillingPeriode fra DVH
                if (orgtilknytninger.size() == 0) {
                    log.debug("Navident {} har historisk stillingPeriode fra DVH ({}) men NOM har ingen orgtilknytninger for ident {}", navident, stillingPeriode, navident);
                } else {
                    // Finn orgtilknytninger på matchende datoperiode
                    var sammePeriode = orgtilknytninger.stream().filter(ot -> isMatchingPeriod(ot, stillingPeriode)).toList();
                    // Valider om noen av disse matcher på agressoId - ok om stillingperiode ikke matcher til 7 sifret, vi tar det for god fisk uansett
                    var matcherAgressoId = sammePeriode.stream().filter(ot -> orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();
                    if (matcherAgressoId.size() == 1) {
                        // Historikken stemmer - move on
                        behandledeOrgtilknytningerFids.add(matcherAgressoId.stream().findFirst().get().fid());
                    } else if (matcherAgressoId.size() > 0) {
                        log.warn("Navident {} har historisk stillingPeriode fra DVH ({}) matcher flere orgtilknytninger {}", navident, stillingPeriode, matcherAgressoId);
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
                                .filter(ot -> stillingPeriode.gyldigFom().equals(ot.gyldigFom()) && orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();
                            if (matcherStartDatoOgAgressoId.size() == 0) {
                                // Matcher ingen i NOM, DVH historisk finnes ikke i NOM, så da behandler vi den ikke.
                                // TODO: Har vi kommet til ett sted hvor gyldigFOM på historisk periode kan ha endret seg i DVH?
                                log.debug("Navident {} har historisk stillingPeriode fra DVH ({}) matcher ingen orgtilknytninger {}", navident, stillingPeriode, matcherAgressoId);
                            } else if (matcherStartDatoOgAgressoId.size() > 1) {
                                // Matcher flere i NOM - dette må være feil!....
                                log.warn("Navident {} har historisk stillingPeriode fra DVH ({}) matcher flere orgtilknytninger {}", navident, stillingPeriode, matcherAgressoId);
                                // Burde ikke vært her - men vi markerer alle disse tidligere orgtilknytningene som behandlet
                                behandledeOrgtilknytningerFids.addAll(matcherStartDatoOgAgressoId.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                            } else {
                                // Vi matcher 1. Og trenger å endre GyldigTOM for denne -> Enten korriger, eller avvikle
                                endreOrgtilknytningGyldigTOM(
                                    matcherStartDatoOgAgressoId.get(0),
                                    stillingPeriode,
                                    StillingPeriode.stillingperioderToString(stillingPerioder)
                                );
                                behandledeOrgtilknytningerFids.addAll(matcherStartDatoOgAgressoId.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                            }
                        }
                    }
                }
            } else if (isCurrentPeriode(stillingPeriode)) {
                // DVH Perioden er aktiv i dag

                // Finn aktiv orgtilknytning på matchende enhet
                var aktiveOrgtilknytninger = orgtilknytninger.stream().filter(PeriodeUtil::isCurrentPeriode);
                // Valider om noen av disse matcher på agressoId - ok om stillingperiode ikke matcher til 7 sifret, vi tar det for god fisk uansett
                var matcherAgressoId = aktiveOrgtilknytninger.filter(ot -> orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();

                // Aktiv periode kan sjekkes for primær tilkynting
                boolean erDagligOppfolging = erStillingPeriodenDagligOppfolging(stillingPeriode);

                if (matcherAgressoId.size() > 1) {
                    log.error(
                        "Navident {} har aktiv stillingPeriode i DVH\n  {}\n som matcher flere aktive orgtilknytninger i state\n  {}",
                        navident,
                        stillingPeriode,
                        matcherAgressoId
                    );
                    matcherAgressoId = fjernDuplikater(matcherAgressoId);
                }
                if (matcherAgressoId.size() > 1) {
                    log.error(
                        "Selv etter forsøk på å fjerne duplikat tilknytninger så har Navident {} har stillingPeriode i DVH\n  {}\n som matcher flere aktive orgtilknytninger i state\n  {}",
                        navident,
                        stillingPeriode,
                        matcherAgressoId
                    );
                    log.error("Dette kan ikke behandles (det er forsøkt i fjernDuplikater over)");
                } else if (matcherAgressoId.size() == 0) {
                    log.debug("Navident {} har aktiv stillingPeriode i DVH\n  {}\n som ikke finnes i orgtilknytning state. Behandler denne som ny.", navident, stillingPeriode);

                    String fid = lagNyOrgtilknytning(navident, stillingPeriode.orgenhet().getId(), stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {
                        settOrgtilknytningSluttDato(fid, navident, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                    if (erDagligOppfolging) {
                        endreOrgtilknytningDagligOppfolging(fid, navident, true, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                } else {
                    log.debug("Navident {} har aktiv stillingPeriode i DVH\n  {}\n som matcher en aktiv orgtilknytning i state\n  {}\n Sjekker denne for dato endringer og evt. endring av primær", navident, stillingPeriode, matcherAgressoId);
                    OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);
                    if (!orgtilknytning.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())) {
                        endreOrgtilknytningGyldigTOM(orgtilknytning, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                    if (!orgtilknytning.gyldigFom().equals(stillingPeriode.gyldigFom())) {
                        endreOrgtilknytningGyldigFOM(orgtilknytning, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                    if (erDagligOppfolging != orgtilknytning.erDagligOppfolging()) {
                        endreOrgtilknytningDagligOppfolging(orgtilknytning.fid(), navident, erDagligOppfolging, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
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
                boolean erDagligOppfolging = erStillingPeriodenDagligOppfolging(stillingPeriode);

                // Finn så i matcherAgressoId om det er match på fra-til dato.
                var matcherIdOgPeriode = matcherAgressoId.stream().filter( ot -> isMatchingPeriod(ot, stillingPeriode)).toList();
                if (matcherIdOgPeriode.size() == 0) {
                    // Vi har ingen direkte match på både start/sluttdato for agressoId

                    // Prøv så å matche på partial date - samme startdato først
                    var matcherIdOgGyldigFom =  matcherAgressoId.stream().filter( ot -> ot.gyldigFom().equals(stillingPeriode.gyldigFom())).toList();
                    if (matcherIdOgGyldigFom.size() > 0) {
                        if (matcherIdOgGyldigFom.size() > 1) {
                            // Vi har flere orgtilknytninger i state som har samme fra og med dato og agressoId som stillignPeriode.
                            log.error("Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og GyldigFom for flere fremtidige orgtilknytninger i state\n  {}\nDette kan ikke behandles", navident, stillingPeriode, matcherIdOgGyldigFom);
                            // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                        } else {
                            log.debug("Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher en fremtidig orgtilknytning i state\n  {}\n Sjekker denne for gyldig fra dato endringer og evt. endring av primær", navident, stillingPeriode, matcherAgressoId);
                            OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                            if (!orgtilknytning.gyldigFom().equals(stillingPeriode.gyldigFom())) {
                                endreOrgtilknytningGyldigFOM(orgtilknytning, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            }
                            if (erDagligOppfolging != orgtilknytning.erDagligOppfolging()) {
                                endreOrgtilknytningDagligOppfolging(orgtilknytning.fid(), navident, erDagligOppfolging, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            }
                        }
                    } else {
                        var matcherIdOgGyldigTom =  matcherAgressoId.stream().filter( ot -> ot.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())).toList();
                        if (matcherIdOgGyldigTom.size() > 0) {
                            if (matcherIdOgGyldigTom.size() > 1) {
                                // Vi har flere orgtilknytninger i state som har samme til og med dato og agressoId som stillignPeriode.
                                log.error("Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og GyldigTom flere fremtidige orgtilknytninger i state\n  {}\nDette kan ikke behandles", navident, stillingPeriode, matcherIdOgGyldigTom);
                                // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                            } else {
                                log.debug("Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher en fremtidig orgtilknytning i state\n  {}\n Sjekker denne for gyldig til dato endringer og evt. endring av primær", navident, stillingPeriode, matcherAgressoId);
                                OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                                if (!orgtilknytning.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())) {
                                    endreOrgtilknytningGyldigTOM(orgtilknytning, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                                }
                                if (erDagligOppfolging != orgtilknytning.erDagligOppfolging()) {
                                    endreOrgtilknytningDagligOppfolging(orgtilknytning.fid(), navident, erDagligOppfolging, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                                }
                            }
                        } else {
                            log.debug("Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som ikke finnes i orgtilknytning state. Behandler denne som ny.",navident, stillingPeriode);

                            String fid = lagNyOrgtilknytning(navident, stillingPeriode.orgenhet().getId(), stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {
                                settOrgtilknytningSluttDato(fid, navident, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            }
                            if (erDagligOppfolging) {
                                endreOrgtilknytningDagligOppfolging(fid, navident, true, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            }
                        }
                    }
                } else if (matcherIdOgPeriode.size() > 1) {
                    // Vi har flere orgtilknytninger i state som har samme dato periode og agressoId som stillingPeriode.
                    log.warn("Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og periode for flere fremtidige orgtilknytninger i state\n  {}\nDette kan ikke behandles", navident, stillingPeriode, matcherIdOgPeriode);
                    // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                } else {
                    // Vi har en presis match på stillingPeriode og Orgtilknytning state
                    log.debug("Navident {} har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og periode for en fremtidige orgtilknytninger i state\n  {}\nSjekker evt endring på primær", navident, stillingPeriode, matcherIdOgPeriode);
                    OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);
                    if (orgtilknytning.erDagligOppfolging() != erDagligOppfolging) {
                        endreOrgtilknytningDagligOppfolging(orgtilknytning.fid(), navident, erDagligOppfolging, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
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

        // TODO: Hva med primær flag som evt ikke har klart å bli satt, selv om det kun er en knytning for periode

        loggOppsummering(navident, stillingPerioder, orgtilknytninger);
    }

    private List<OrgtilknytningState> fjernDuplikater(List<OrgtilknytningState> matcherAgressoId) {
        OrgtilknytningState toKeep = matcherAgressoId.get(0);
        List<OrgtilknytningState> statesToKeep = new ArrayList<>(List.of(toKeep));
        matcherAgressoId.stream().skip(1).forEach( ot -> {
            // Make sure they are indeed duplicates
            if (ot.navident().equals(toKeep.navident())
                && ot.orgEnhetId().equals(toKeep.orgEnhetId())
                && ot.gyldigFom().equals(toKeep.gyldigFom())
                && (ot.gyldigTom().isPresent() ? ot.gyldigTom().get().equals(toKeep.gyldigTom().orElse(null)) : toKeep.gyldigTom().isEmpty())) {
                    // Plain duplicate - remove
                    fjernOrgtilknytning(ot);
            } else {
                // There is a difference? Cannot remove possible overlapping dates, but not equal?
                log.info("Fjerner duplikat orgtilknytning for Navident {}: {}", ot.navident(), ot);
                statesToKeep.add(ot);
            }
        });
        return statesToKeep;
    }

    public boolean isMatchingPeriod(OrgtilknytningState orgtilknytningState, StillingPeriode stillingPeriode) {
        return
            stillingPeriode.gyldigFom().equals(orgtilknytningState.gyldigFom())
                &&
                stillingPeriode.gyldigTom().equals(orgtilknytningState.gyldigTom().orElse(NO_END_DATE));
    }

    public boolean erStillingPeriodenDagligOppfolging(StillingPeriode stillingPeriode) {
        // Er forventet at dette kun spørres på for en aktiv eller fremtidig stilling. Men sjekkes ikke
        var stillingEnhetId = stillingPeriode.stilling().agressoId();
        var ressursEnhetId = stillingPeriode.stilling().ressursAgressoId();

        if (stillingEnhetId.isEmpty() || ressursEnhetId.isEmpty()) return true; // Hvis enten enhet på stilling mangler, eller enhet på ressurs mangler - går vi ut ifra at dette er primær stillingen.

        return stillingEnhetId.get().startsWith(ressursEnhetId.get()) || ressursEnhetId.get().startsWith(stillingEnhetId.get());
    }


    /**
     * Vil matche på overnode-agressoId
     * Kan brukes på historiske rader, hvor DVH kan angi overnode og ikke løv-node (ORGENHET)
     * Altså en stillingPeriode med angivelse av f.eks. ORGNIV3 node '060200' vil da matche en orgtilknytning i nom for ORGENHET '0602002'
     */
    public boolean orgtilknytningEnhetMatcherAgressoId(StillingPeriode stillingPeriode, OrgtilknytningState ot) {
        if (isCurrentPeriode(stillingPeriode)) {
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
        return orgtilknytningCommandService.opprettOrgtilknytning(command);
    }

    public void endreOrgtilknytningGyldigFOM(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        var command = new KorrigerOrgtilknytningDato(
            originalOrgtilknytning.navident(),
            originalOrgtilknytning.orgEnhetId(),
            stillingPeriode.gyldigFom(),
            originalOrgtilknytning.gyldigTom(),
            metadata,
            additionalData
        );

        loggCommand(originalOrgtilknytning.navident(), originalOrgtilknytning.fid(), command);

        orgtilknytningCommandService.korrigerOrgtilknytningDato(originalOrgtilknytning.fid(), command);
    }

    public void endreOrgtilknytningGyldigTOM(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        if (originalOrgtilknytning.gyldigTom().isPresent()) {
            var command = new KorrigerOrgtilknytningDato(
                originalOrgtilknytning.navident(),
                originalOrgtilknytning.orgEnhetId(),
                originalOrgtilknytning.gyldigFom(),
                stillingPeriode.gyldigTom().isEqual(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom()),
                metadata,
                additionalData
            );

            loggCommand(originalOrgtilknytning.navident(), originalOrgtilknytning.fid(), command);

            orgtilknytningCommandService.korrigerOrgtilknytningDato(originalOrgtilknytning.fid(), command);
        } else {
            var command = new AvvikleOrgtilknytning(
                originalOrgtilknytning.navident(),
                originalOrgtilknytning.orgEnhetId(),
                stillingPeriode.gyldigTom(),
                metadata,
                additionalData
            );

            loggCommand(originalOrgtilknytning.navident(), originalOrgtilknytning.fid(), command);

            orgtilknytningCommandService.avvikleOrgtilknytning(originalOrgtilknytning.fid(),command);
        }
    }

    public void settOrgtilknytningSluttDato(String fid, String navident, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
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

        loggCommand(navident, fid, command);

        orgtilknytningCommandService.avvikleOrgtilknytning(fid,command);
    }

    private void endreOrgtilknytningDagligOppfolging(String fid, String navident, boolean erDagligOppfolging, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
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

        orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(fid,command);
    }

    public void fjernOrgtilknytning(OrgtilknytningState originalOrgtilknytning) {
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

    private void loggOppsummering(String navident, List<StillingPeriode> stillingPerioder, Collection<OrgtilknytningState> orgtilknytninger) {
        if (log.isDebugEnabled()) {
            StringBuffer strbuf = new StringBuffer();
            strbuf.append("Oppsummering for navident %s".formatted(navident));
            strbuf.append("\n\tAntall DVH ressursStillinger: %s".formatted(stillingPerioder.size()));
            strbuf.append("\n\t\tHistoriske: %s".formatted(stillingPerioder.stream().filter(PeriodeUtil::isHistoricPeriod).count()));
            strbuf.append("\n\t\tAktive: %s".formatted(stillingPerioder.stream().filter(PeriodeUtil::isCurrentPeriode).count()));
            strbuf.append("\n\t\tFremtidige: %s".formatted(stillingPerioder.stream().filter(PeriodeUtil::isFuturePeriod).count()));
            strbuf.append("\n\tAntall eksisterende orgtilknytninger: %s".formatted(orgtilknytninger.size()));
            strbuf.append("\n\t\tHistoriske: %s".formatted(orgtilknytninger.stream().filter(PeriodeUtil::isHistoricPeriod).count()));
            strbuf.append("\n\t\tAktive: %s".formatted(orgtilknytninger.stream().filter(PeriodeUtil::isCurrentPeriode).count()));
            strbuf.append("\n\t\tFremtidige: %s".formatted(orgtilknytninger.stream().filter(PeriodeUtil::isFuturePeriod).count()));

            strbuf.append("\n\t===> Har laget følgende evtsrc hendelser:");
            if (evtsrcCommandsPerNavident.get(navident).size() == 0) {
                strbuf.append("\n\t\tIngen");
            } else {
                evtsrcCommandsPerNavident.get(navident).forEach(strbuf::append);
            }
            log.debug(strbuf.toString());
        }
        evtsrcCommandsPerNavident = new HashMap<>();
    }

    private void loggCommand(String navident, OpprettOrgtilknytning cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident).add("\n\t\tOpprettOrgtilknytning for enhet %s gyldigFom %s".formatted(cmd.orgenhetId(), cmd.gyldigFom()));
            processCmdResult.numOpprett++;
        }
    }
    private void loggCommand(String navident, String fid, AvvikleOrgtilknytning cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident).add("\n\t\tAvvikleOrgtilknytning for fid %s enhet %s gyldigTom %s".formatted(fid, cmd.orgenhetId(), cmd.gyldigTom()));
            processCmdResult.numAvviklet++;
        }
    }
    private void loggCommand(String navident, String fid, KorrigerOrgtilknytningDato cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident).add("\n\t\tAvvikleOrgtilknytning for fid %s enhet %s gyldigFom %s - gyldigTom %s".formatted(fid, cmd.orgenhetId(), cmd.gyldigFom(), cmd.gyldigTom()));
            processCmdResult.numKorrigertDato++;
        }
    }
    private void loggCommand(String navident, String fid, EndreDagligOppfolgingOrgtilknytning cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident).add("\n\t\tEndreDagligOppfolgingOrgtilknytning for fid %s enhet %s gyldigFom %s - gyldigTom %s => %s".formatted(fid, cmd.orgenhetId(), cmd.gyldigFom(), cmd.gyldigTom(), cmd.dagligOppfolging()));
            processCmdResult.numEndretDagligOppfolging++;
        }
    }
    private void loggCommand(String navident, String fid, FjernOrgtilknytning cmd) {
        if (log.isDebugEnabled() && evtsrcCommandsPerNavident.containsKey(navident)) {
            evtsrcCommandsPerNavident.get(navident).add("\n\t\tFjernOrgtilknytning for fid %s enhet %s gyldigFom %s - gyldigTom %s".formatted(fid, cmd.orgenhetId(), cmd.gyldigFom(), cmd.gyldigTom()));
            processCmdResult.numFjernet++;
        }
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
