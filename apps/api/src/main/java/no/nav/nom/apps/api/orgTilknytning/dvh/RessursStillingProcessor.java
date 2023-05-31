package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndrePrimaeOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.KorrigerOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isCurrentPeriode;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isHistoricPeriod;

@Component
@Slf4j
public class RessursStillingProcessor {
    public static final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);

    private final IOrgenhetService orgenhetService;
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;


    public RessursStillingProcessor(IOrgenhetService orgenhetService, OrgtilknytningStateService orgtilknytningStateService, OrgtilknytningCommandService orgtilknytningCommandService) {
        this.orgenhetService = orgenhetService;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
    }

    public void process(Map<String, List<StillingPeriode>> stillingPerioderPerNavident) {
        stillingPerioderPerNavident.forEach(this::process);
    }

    // Prosesserer ett sett med stillingPerioder fra DVH som allerede er slått sammen/splittet på enhet/periode
    public void process(String navident, List<StillingPeriode> stillingPerioder) {
        Set<String> behandledeOrgtilknytningerFids = new HashSet<>();

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

        // Hent orgtilknytninger fra State
        var orgtilknytninger = orgtilknytningStateService.getKoblingerByNavIdent(navident);
        stillingPerioder.forEach( stillingPeriode -> {
            if (isHistoricPeriod(stillingPeriode)) {  // Historisk stillingPeriode fra DVH
                if (orgtilknytninger.size() == 0) {
                    log.debug("Historisk stillingPeriode fra DVH ({}) men NOM har ingen orgtilknytninger for ident {}", stillingPeriode, navident);
                } else {
                    // Finn orgtilknytninger på matchende datoperiode
                    var sammePeriode = orgtilknytninger.stream().filter(ot -> isMatchingPeriod(ot, stillingPeriode)).toList();
                    // Valider om noen av disse matcher på agressoId - ok om stillingperiode ikke matcher til 7 sifret, vi tar det for god fisk uansett
                    var matcherAgressoId = sammePeriode.stream().filter(ot -> orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();
                    if (matcherAgressoId.size() == 1) {
                        // Historikken stemmer - move on
                        behandledeOrgtilknytningerFids.add(matcherAgressoId.stream().findFirst().get().fid());
                    } else if (matcherAgressoId.size() > 0) {
                        log.warn("Historisk stillingPeriode fra DVH ({}) matcher flere orgtilknytninger {}", stillingPeriode, matcherAgressoId);
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
                                log.debug("Historisk stillingPeriode fra DVH ({}) matcher ingen orgtilknytninger {}", stillingPeriode, matcherAgressoId);
                            } else if (matcherStartDatoOgAgressoId.size() > 1) {
                                // Matcher flere i NOM - dette må være feil!....
                                log.warn("Historisk stillingPeriode fra DVH ({}) matcher flere orgtilknytninger {}", stillingPeriode, matcherAgressoId);
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
                boolean erPrimaer = erStillingPeriodenPrimaer(stillingPeriode);

                if (matcherAgressoId.size() > 1) {
                    log.warn("Navident har aktiv stillingPeriode i DVH\n  {}\n som matcher flere aktive orgtilknytninger i state\n  {}\nDette kan ikke behandles", stillingPeriode, matcherAgressoId);
                    // Har ingen behandling av denne stilling perioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                } else if (matcherAgressoId.size() == 0) {
                    log.debug("Navident har aktiv stillingPeriode i DVH\n  {}\n som ikke finnes i orgtilknytning state. Behandler denne som ny.", stillingPeriode);

                    String fid = lagNyOrgtilknytning(navident, stillingPeriode.orgenhet().getId(), stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {
                        settOrgtilknytningSluttDato(fid, navident, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                    if (erPrimaer) {
                        endreOrgtilknytningPrimaer(fid, navident, true, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                } else {
                    log.debug("Navident har aktiv stillingPeriode i DVH\n  {}\n som matcher en aktiv orgtilknytning i state\n  {}\n Sjekker denne for dato endringer og evt. endring av primær", stillingPeriode, matcherAgressoId);
                    OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);
                    if (!orgtilknytning.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())) {
                        endreOrgtilknytningGyldigTOM(orgtilknytning, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                    if (!orgtilknytning.gyldigFom().equals(stillingPeriode.gyldigFom())) {
                        endreOrgtilknytningGyldigFOM(orgtilknytning, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                    if (erPrimaer != orgtilknytning.erPrimaer()) {
                        endreOrgtilknytningPrimaer(orgtilknytning.fid(), navident, erPrimaer, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
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
                boolean erPrimaer = erStillingPeriodenPrimaer(stillingPeriode);

                // Finn så i matcherAgressoId om det er match på fra-til dato.
                var matcherIdOgPeriode = matcherAgressoId.stream().filter( ot -> isMatchingPeriod(ot, stillingPeriode)).toList();
                if (matcherIdOgPeriode.size() == 0) {
                    // Vi har ingen direkte match på både start/sluttdato for agressoId

                    // Prøv så å matche på partial date - samme startdato først
                    var matcherIdOgGyldigFom =  matcherAgressoId.stream().filter( ot -> ot.gyldigFom().equals(stillingPeriode.gyldigFom())).toList();
                    if (matcherIdOgGyldigFom.size() > 0) {
                        if (matcherIdOgGyldigFom.size() > 1) {
                            // Vi har flere orgtilknytninger i state som har samme fra og med dato og agressoId som stillignPeriode.
                            log.warn("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og GyldigFom for flere fremtidige orgtilknytninger i state\n  {}\nDette kan ikke behandles", stillingPeriode, matcherIdOgGyldigFom);
                            // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                        } else {
                            log.debug("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher en fremtidig orgtilknytning i state\n  {}\n Sjekker denne for gyldig fra dato endringer og evt. endring av primær", stillingPeriode, matcherAgressoId);
                            OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                            if (!orgtilknytning.gyldigFom().equals(stillingPeriode.gyldigFom())) {
                                endreOrgtilknytningGyldigFOM(orgtilknytning, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            }
                            if (erPrimaer != orgtilknytning.erPrimaer()) {
                                endreOrgtilknytningPrimaer(orgtilknytning.fid(), navident, erPrimaer, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            }
                        }
                    } else {
                        var matcherIdOgGyldigTom =  matcherAgressoId.stream().filter( ot -> ot.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())).toList();
                        if (matcherIdOgGyldigTom.size() > 0) {
                            if (matcherIdOgGyldigTom.size() > 1) {
                                // Vi har flere orgtilknytninger i state som har samme til og med dato og agressoId som stillignPeriode.
                                log.warn("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og GyldigTom flere fremtidige orgtilknytninger i state\n  {}\nDette kan ikke behandles", stillingPeriode, matcherIdOgGyldigTom);
                                // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                            } else {
                                log.debug("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher en fremtidig orgtilknytning i state\n  {}\n Sjekker denne for gyldig til dato endringer og evt. endring av primær", stillingPeriode, matcherAgressoId);
                                OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                                if (!orgtilknytning.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom())) {
                                    endreOrgtilknytningGyldigTOM(orgtilknytning, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                                }
                                if (erPrimaer != orgtilknytning.erPrimaer()) {
                                    endreOrgtilknytningPrimaer(orgtilknytning.fid(), navident, erPrimaer, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                                }
                            }
                        } else {
                            log.debug("Navident har fremtidig stillingPeriode i DVH\n  {}\n som ikke finnes i orgtilknytning state. Behandler denne som ny.", stillingPeriode);

                            String fid = lagNyOrgtilknytning(navident, stillingPeriode.orgenhet().getId(), stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {
                                settOrgtilknytningSluttDato(fid, navident, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            }
                            if (erPrimaer) {
                                endreOrgtilknytningPrimaer(fid, navident, true, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                            }
                        }
                    }
                } else if (matcherIdOgPeriode.size() > 1) {
                    // Vi har flere orgtilknytninger i state som har samme dato periode og agressoId som stillignPeriode.
                    log.warn("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og periode for flere fremtidige orgtilknytninger i state\n  {}\nDette kan ikke behandles", stillingPeriode, matcherIdOgPeriode);
                    // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                } else {
                    // Vi har en presis match på stillingPeriode og Orgtilknytning state
                    log.warn("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og periode for en fremtidige orgtilknytninger i state\n  {}\nSjekker evt endring på primær", stillingPeriode, matcherIdOgPeriode);
                    OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);
                    if (orgtilknytning.erPrimaer() != erPrimaer) {
                        endreOrgtilknytningPrimaer(orgtilknytning.fid(), navident, erPrimaer, stillingPeriode, StillingPeriode.stillingperioderToString(stillingPerioder));
                    }
                }
                behandledeOrgtilknytningerFids.addAll(matcherAgressoId.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));

            }
        });
        // Sjekk til slutt om det er noen orgtilknytninger i state som ikke er 'behandlet'. De finnes da ikke i DVH mer, og må fjernes
        orgtilknytninger.stream()
            .filter(ot -> !behandledeOrgtilknytningerFids.contains(ot.fid()))
            .forEach(ot -> {
                log.debug("DVH har ikke denne orgtilknytningen mer, så vi fjerner den: {}", ot);
                fjernOrgtilknytning(ot);
            });

        // TODO: Hva med primær flag som evt ikke har klart å bli satt, selv om det kun er en knytning for periode

        // TODO: Gjør en oppsummering på endringer - state er eventually, så vi kan ikke behandle state og sjekk konsistens enda.
    }


    public boolean isMatchingPeriod(OrgtilknytningState orgtilknytningState, StillingPeriode stillingPeriode) {
        return
            stillingPeriode.gyldigFom().equals(orgtilknytningState.gyldigFom())
                &&
                stillingPeriode.gyldigTom().equals(orgtilknytningState.gyldigTom().orElse(NO_END_DATE));
    }

    public boolean erStillingPeriodenPrimaer(StillingPeriode stillingPeriode) {
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
        return orgenhetService.getById(ot.orgEnhetId()).get().getAgressoId().startsWith(stillingPeriode.stilling().agressoId().orElse("NULL"));
    }

    public String lagNyOrgtilknytning(String navident, String orgenhetId, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        return orgtilknytningCommandService.opprettOrgtilknytning(
            new OpprettOrgtilknytning(
                navident,
                orgenhetId,
                stillingPeriode.gyldigFom(),
                metadata,
                additionalData
            )
        );
    }

    public void endreOrgtilknytningGyldigFOM(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        orgtilknytningCommandService.korrigerOrgtilknytningDato(
            originalOrgtilknytning.fid(),
            new KorrigerOrgtilknytningDato(
                originalOrgtilknytning.navident(),
                originalOrgtilknytning.orgEnhetId(),
                stillingPeriode.gyldigFom(),
                originalOrgtilknytning.gyldigTom(),
                metadata,
                additionalData
            )
        );
    }

    public void endreOrgtilknytningGyldigTOM(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        if (originalOrgtilknytning.gyldigTom().isPresent()) {
            orgtilknytningCommandService.korrigerOrgtilknytningDato(
                originalOrgtilknytning.fid(),
                new KorrigerOrgtilknytningDato(
                    originalOrgtilknytning.navident(),
                    originalOrgtilknytning.orgEnhetId(),
                    originalOrgtilknytning.gyldigFom(),
                    stillingPeriode.gyldigTom().isEqual(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom()),
                    metadata,
                    additionalData
                )
            );
        } else {
            orgtilknytningCommandService.avvikleOrgtilknytning(
                originalOrgtilknytning.fid(),
                new AvvikleOrgtilknytning(
                    originalOrgtilknytning.navident(),
                    originalOrgtilknytning.orgEnhetId(),
                    stillingPeriode.gyldigTom(),
                    metadata,
                    additionalData
                )
            );
        }
    }

    public void settOrgtilknytningSluttDato(String fid, String navident, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        orgtilknytningCommandService.avvikleOrgtilknytning(
            fid,
            new AvvikleOrgtilknytning(
                navident,
                stillingPeriode.orgenhet().getId(),
                stillingPeriode.gyldigTom(),
                metadata,
                additionalData
            )
        );
    }

    private void endreOrgtilknytningPrimaer(String fid, String navident, boolean erPrimaer, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling().toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        orgtilknytningCommandService.endrePrimaerOrgtilknytning(
            fid,
            new EndrePrimaeOrgtilknytning(
                navident,
                stillingPeriode.orgenhet().getId(),
                stillingPeriode.gyldigFom(),
                stillingPeriode.gyldigTom().equals(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom()),
                erPrimaer,
                metadata,
                additionalData
            )
        );
    }

    public void fjernOrgtilknytning(OrgtilknytningState originalOrgtilknytning) {
        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        orgtilknytningCommandService.fjernOrgtilknytning(
            originalOrgtilknytning.fid(),
            new FjernOrgtilknytning(
                originalOrgtilknytning.navident(),
                originalOrgtilknytning.orgEnhetId(),
                originalOrgtilknytning.gyldigFom(),
                originalOrgtilknytning.gyldigTom(),
                metadata,
                null
            )
        );
    }
}
