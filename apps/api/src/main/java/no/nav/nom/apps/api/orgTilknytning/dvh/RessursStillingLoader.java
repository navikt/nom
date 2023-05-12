package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Service
public class RessursStillingLoader {
    public static final String UNLEASH_LAST_NY_ORGTILKNYTNING = "nom.orgtilknytning.last";
    private static final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);
    private final TokenRepository tokenRepository;
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final IOrgenhetService orgenhetService;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;
    private final Unleash unleash;

    public RessursStillingLoader(TokenRepository tokenRepository, OrgtilknytningStateService orgtilknytningStateService,
                                 OrgtilknytningCommandService orgtilknytningCommandService,
                                 IOrgenhetService orgenhetService, RestTemplate restTemplate, AppProperties appProperties,
                                 @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
                                 Unleash unleash) {
        this.tokenRepository = tokenRepository;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.orgenhetService = orgenhetService;
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.unleash = unleash;
    }

    public void loadAndProcess() {
        if (unleash.isEnabled(UNLEASH_LAST_NY_ORGTILKNYTNING)) {
            List<DvhRessursStilling> ressursStillinger = load();

            Map <String, List<DvhRessursStilling>> ressursStillingerGruppert = grupperPerNavident(ressursStillinger);
            log.debug("Gruppert ressursStilligner på {} navidenter", (Integer)ressursStillingerGruppert.keySet().size());

            ressursStillingerGruppert.forEach((navident, rsList) -> {
                log.debug("Strukturer ressursStillinger for navident {} til orgtilknytninger", navident);
                var stillingerGruppert = strukturerNavidentensRessursStillinger(rsList);
                if (log.isDebugEnabled()) {
                    log.debug("Resultat:\n{}", stillingperioderToString(stillingerGruppert, true));
                    process(navident, stillingerGruppert);
                }
            });

            // TODO: Etter last - vent litt - les state og sjekk sanity?
        }
    }

    public void process(String navident, List<StillingPeriode> stillingPerioder) {
        Set<String> behandledeOrgtilknytningerFids = new HashSet<>();

        if (log.isDebugEnabled()) {
            var duplikateCurrent = stillingPerioder.stream().filter(this::isCurrentPeriode);
            if (duplikateCurrent.count() > 1) {
                log.debug(
                    "Navid {} har duplikate aktive stillinger:\n{}",
                    navident,
                    stillingperioderToString(stillingPerioder.stream().filter(this::isCurrentPeriode).toList(), true)
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
                                .filter(ot -> stillingPeriode.gyldigFom.equals(ot.gyldigFom()) && orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();
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
                                    stillingperioderToString(stillingPerioder)
                                );
                                behandledeOrgtilknytningerFids.addAll(matcherStartDatoOgAgressoId.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                            }
                        }
                    }
                }
            } else if (isCurrentPeriode(stillingPeriode)) {
                // DVH Perioden er aktiv i dag

                // Finn aktiv orgtilknytning på matchende enhet
                var aktiveOrgtilknytninger = orgtilknytninger.stream().filter(this::isCurrentPeriode);
                // Valider om noen av disse matcher på agressoId - ok om stillingperiode ikke matcher til 7 sifret, vi tar det for god fisk uansett
                var matcherAgressoId = aktiveOrgtilknytninger.filter(ot -> orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();

                // Aktiv periode kan sjekkes for primær tilkynting
                boolean erPrimaer = erStillingPeriodenPrimaer(stillingPeriode);

                if (matcherAgressoId.size() > 1) {
                    log.warn("Navident har aktiv stillingPeriode i DVH\n  {}\n som matcher flere aktive orgtilknytninger i state\n  {}\nDette kan ikke behandles", stillingPeriode, matcherAgressoId);
                    // Har ingen behandling av denne stilling perioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                } else if (matcherAgressoId.size() == 0) {
                    log.debug("Navident har aktiv stillingPeriode i DVH\n  {}\n som ikke finnes i orgtilknytning state. Behandler denne som ny.", stillingPeriode);

                    String fid = lagNyOrgtilknytning(navident, stillingPeriode.orgenhet.getId(), stillingPeriode, stillingperioderToString(stillingPerioder));
                    if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {
                        settOrgtilknytningSluttDato(fid, navident, stillingPeriode, stillingperioderToString(stillingPerioder));
                    }
                    if (erPrimaer) {
                        endreOrgtilknytningPrimaer(fid, navident, true, stillingPeriode, stillingperioderToString(stillingPerioder));
                    }
                } else {
                    log.debug("Navident har aktiv stillingPeriode i DVH\n  {}\n som matcher en aktiv orgtilknytning i state\n  {}\n Sjekker denne for dato endringer og evt. endring av primær", stillingPeriode, matcherAgressoId);
                    OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);
                    if (!orgtilknytning.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom)) {
                        endreOrgtilknytningGyldigTOM(orgtilknytning, stillingPeriode, stillingperioderToString(stillingPerioder));
                    }
                    if (!orgtilknytning.gyldigFom().equals(stillingPeriode.gyldigFom())) {
                        endreOrgtilknytningGyldigFOM(orgtilknytning, stillingPeriode, stillingperioderToString(stillingPerioder));
                    }
                    if (erPrimaer != orgtilknytning.erPrimaer()) {
                        endreOrgtilknytningPrimaer(orgtilknytning.fid(), navident, erPrimaer, stillingPeriode, stillingperioderToString(stillingPerioder));
                    }
                }

                behandledeOrgtilknytningerFids.addAll(matcherAgressoId.stream().map(OrgtilknytningState::fid).collect(Collectors.toSet()));
            } else {
                // Perioden er fremtidig
                // Hvis fremtidig DVH stillingPeriode har endret enhetId ift hva som tidligere er lest inn til Orgtilknytning state - så ikke behandle Orgtilknytning staten (da fjernes den), og DVH stillingPerioden behandles som helt ny.

                // Finn fremtidige orgtilknytninger på matchende enhet
                var aktiveOrgtilknytninger = orgtilknytninger.stream().filter(this::isFuturePeriod);
                // Valider om noen av disse matcher på agressoId - ok om stillingPeriode.enhetId ikke matcher til 7 sifret, vi tar det for god fisk uansett
                var matcherAgressoId = aktiveOrgtilknytninger.filter(ot -> orgtilknytningEnhetMatcherAgressoId(stillingPeriode, ot)).toList();

                // Fremtidig periode kan sjekkes for primær tilkynting
                boolean erPrimaer = erStillingPeriodenPrimaer(stillingPeriode);

                // Finn så i matcherAgressoId om det er match på fra-til dato.
                var matcherIdOgPeriode = matcherAgressoId.stream().filter( ot -> isMatchingPeriod(ot, stillingPeriode)).toList();
                if (matcherIdOgPeriode.size() == 0) {
                    // Vi har ingen direkte match på både start/sluttdato for agressoId

                    // Prøv så å matche på partial date - samme startdato først
                    var matcherIdOgGyldigFom =  matcherAgressoId.stream().filter( ot -> ot.gyldigFom().equals(stillingPeriode.gyldigFom)).toList();
                    if (matcherIdOgGyldigFom.size() > 0) {
                        if (matcherIdOgGyldigFom.size() > 1) {
                            // Vi har flere orgtilknytninger i state som har samme fra og med dato og agressoId som stillignPeriode.
                            log.warn("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og GyldigFom for flere fremtidige orgtilknytninger i state\n  {}\nDette kan ikke behandles", stillingPeriode, matcherIdOgGyldigFom);
                            // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                        } else {
                            log.debug("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher en fremtidig orgtilknytning i state\n  {}\n Sjekker denne for gyldig fra dato endringer og evt. endring av primær", stillingPeriode, matcherAgressoId);
                            OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                            if (!orgtilknytning.gyldigFom().equals(stillingPeriode.gyldigFom())) {
                                endreOrgtilknytningGyldigFOM(orgtilknytning, stillingPeriode, stillingperioderToString(stillingPerioder));
                            }
                            if (erPrimaer != orgtilknytning.erPrimaer()) {
                                endreOrgtilknytningPrimaer(orgtilknytning.fid(), navident, erPrimaer, stillingPeriode, stillingperioderToString(stillingPerioder));
                            }
                        }
                    } else {
                        var matcherIdOgGyldigTom =  matcherAgressoId.stream().filter( ot -> ot.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom)).toList();
                        if (matcherIdOgGyldigTom.size() > 0) {
                            if (matcherIdOgGyldigTom.size() > 1) {
                                // Vi har flere orgtilknytninger i state som har samme til og med dato og agressoId som stillignPeriode.
                                log.warn("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher enhetId og GyldigTom flere fremtidige orgtilknytninger i state\n  {}\nDette kan ikke behandles", stillingPeriode, matcherIdOgGyldigTom);
                                // Har ingen behandling av denne stillingPerioden, men vi sier at orgtilknytningene er 'behandlet' (så de ikke slettes)
                            } else {
                                log.debug("Navident har fremtidig stillingPeriode i DVH\n  {}\n som matcher en fremtidig orgtilknytning i state\n  {}\n Sjekker denne for gyldig til dato endringer og evt. endring av primær", stillingPeriode, matcherAgressoId);
                                OrgtilknytningState orgtilknytning = matcherAgressoId.get(0);

                                if (!orgtilknytning.gyldigTom().orElse(NO_END_DATE).equals(stillingPeriode.gyldigTom)) {
                                    endreOrgtilknytningGyldigTOM(orgtilknytning, stillingPeriode, stillingperioderToString(stillingPerioder));
                                }
                                if (erPrimaer != orgtilknytning.erPrimaer()) {
                                    endreOrgtilknytningPrimaer(orgtilknytning.fid(), navident, erPrimaer, stillingPeriode, stillingperioderToString(stillingPerioder));
                                }
                            }
                        } else {
                            log.debug("Navident har fremtidig stillingPeriode i DVH\n  {}\n som ikke finnes i orgtilknytning state. Behandler denne som ny.", stillingPeriode);

                            String fid = lagNyOrgtilknytning(navident, stillingPeriode.orgenhet.getId(), stillingPeriode, stillingperioderToString(stillingPerioder));
                            if (stillingPeriode.gyldigTom().isBefore(NO_END_DATE)) {
                                settOrgtilknytningSluttDato(fid, navident, stillingPeriode, stillingperioderToString(stillingPerioder));
                            }
                            if (erPrimaer) {
                                endreOrgtilknytningPrimaer(fid, navident, true, stillingPeriode, stillingperioderToString(stillingPerioder));
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
                        endreOrgtilknytningPrimaer(orgtilknytning.fid(), navident, erPrimaer, stillingPeriode, stillingperioderToString(stillingPerioder));
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

    private boolean isMatchingPeriod(OrgtilknytningState orgtilknytningState, StillingPeriode stillingPeriode) {
        return
            stillingPeriode.gyldigFom.equals(orgtilknytningState.gyldigFom())
                &&
            stillingPeriode.gyldigTom().equals(orgtilknytningState.gyldigTom().orElse(NO_END_DATE));
    }

    private boolean erStillingPeriodenPrimaer(StillingPeriode stillingPeriode) {
        // Er forventet at dette kun spørres på for en aktiv eller fremtidig stilling.
        var stillingEnhetId = stillingPeriode.stilling.agressoId;
        var ressursEnhetId = stillingPeriode.stilling.ressursAgressoId;

        if (stillingEnhetId.isEmpty() || ressursEnhetId.isEmpty()) return true; // Hvis enten enhet på stilling mangler, eller enhet på ressurs mangler - går vi ut ifra at dette er primær stillingen.

        return stillingEnhetId.get().startsWith(ressursEnhetId.get()) || ressursEnhetId.get().startsWith(stillingEnhetId.get());
    }

    private String lagNyOrgtilknytning(String navident, String orgenhetId, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling.toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        return orgtilknytningCommandService.opprettOrgtilknytning(
            new OpprettOrgtilknytning(
                navident,
                orgenhetId,
                stillingPeriode.gyldigFom,
                metadata,
                additionalData
            )
        );
    }

    private void endreOrgtilknytningGyldigFOM(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling.toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        orgtilknytningCommandService.korrigerOrgtilknytningDato(
            originalOrgtilknytning.fid(),
            new KorrigerOrgtilknytningDato(
                originalOrgtilknytning.navident(),
                originalOrgtilknytning.orgEnhetId(),
                originalOrgtilknytning.gyldigFom(),
                originalOrgtilknytning.gyldigTom(),
                metadata,
                additionalData
            )
        );
    }

    private void endreOrgtilknytningGyldigTOM(OrgtilknytningState originalOrgtilknytning, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling.toString(),
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

    private void settOrgtilknytningSluttDato(String fid, String navident, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling.toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        orgtilknytningCommandService.avvikleOrgtilknytning(
            fid,
            new AvvikleOrgtilknytning(
                navident,
                stillingPeriode.orgenhet.getId(),
                stillingPeriode.gyldigTom(),
                metadata,
                additionalData
            )
        );
    }

    private void endreOrgtilknytningPrimaer(String fid, String navident, boolean erPrimaer, StillingPeriode stillingPeriode, String stillingPerioderAsString) {
        OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            stillingPeriode.stilling.toString(),
            stillingPerioderAsString
        );

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"));

        orgtilknytningCommandService.endrePrimaerOrgtilknytning(
            fid,
            new EndrePrimaeOrgtilknytning(
                navident,
                stillingPeriode.orgenhet.getId(),
                stillingPeriode.gyldigFom,
                stillingPeriode.gyldigTom().equals(NO_END_DATE) ? Optional.empty() : Optional.of(stillingPeriode.gyldigTom),
                erPrimaer,
                metadata,
                additionalData
            )
        );
    }

    private void fjernOrgtilknytning(OrgtilknytningState originalOrgtilknytning) {
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

    /**
     * Vil matche på overnode-agressoId
     * Kan brukes på historiske rader, hvor DVH kan angi overnode og ikke løv-node (ORGENHET)
     * Altså en stillingPeriode med angivelse av f.eks. ORGNIV3 node '060200' vil da matche en orgtilknytning i nom for ORGENHET '0602002'
     */
    private boolean orgtilknytningEnhetMatcherAgressoId(StillingPeriode stillingPeriode, OrgtilknytningState ot) {
        //noinspection OptionalGetWithoutIsPresent
        return orgenhetService.getById(ot.orgEnhetId()).get().getAgressoId().startsWith(stillingPeriode.stilling.agressoId.orElse("NULL"));
    }

    private boolean isFuturePeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigFom().isAfter(LocalDate.now());
    }

    private boolean isCurrentPeriode(StillingPeriode stillingPeriode) {
        return isCurrentOrFuturePeriod(stillingPeriode) && stillingPeriode.gyldigFom().isBefore(tomorrow());
    }

    private boolean isCurrentPeriode(OrgtilknytningState orgtilknytning) {
        return isCurrentOrFuturePeriod(orgtilknytning) && orgtilknytning.gyldigFom().isBefore(tomorrow());
    }

    private boolean isHistoricPeriod(StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigTom.isBefore(tomorrow());
    }

    private boolean isCurrentOrFuturePeriod(StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigTom.isAfter(yesterday());
    }

    private boolean isCurrentOrFuturePeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigTom().isEmpty() || orgtilknytning.gyldigTom().get().isAfter(yesterday());
    }

    private LocalDate yesterday() { return LocalDate.now().minusDays(1); }

    private LocalDate tomorrow() { return LocalDate.now().plusDays(1); }

    protected List<DvhRessursStilling> load() {
        log.info("Laster ressursStillinger fra {}", appProperties.proxy().ressursStillingerPath());
        LocalDateTime start = LocalDateTime.now();
        val jsonRessursStillinger = dvhExchange(appProperties.proxy().ressursStillingerPath());
        val listField = jsonRessursStillinger.fields().next();
        log.debug("Lastet ressursStillinger: Første field: {} med {} ant noder", listField.getKey(), (Integer)listField.getValue().size());
        List<DvhRessursStilling> ressursStillinger = StreamSupport.stream(jsonRessursStillinger.get(ressursStillingerResponseNode).spliterator(), true)
            .map(DvhRessursStilling::new)
            .collect(Collectors.toList());
        log.info("Lastet {} ressursStillinger. Tok {}", (Integer)ressursStillinger.size(), durationAsH24(start));
        return ressursStillinger;
    }

    public Map<String, List<DvhRessursStilling>> grupperPerNavident(List<DvhRessursStilling> ressursStillinger) {
        return ressursStillinger.stream()
            .filter(rs -> rs.getNavident().isPresent())
            .collect(groupingBy(rs -> rs.getNavident().get()));
    }

    public List<StillingPeriode> strukturerNavidentensRessursStillinger(List<DvhRessursStilling> ressursStillinger) {
        // grupper først på Stillingens - stilling enhet/navn +enhetNavn+ressurs (A+B)
        var grupperteStillinger = ressursStillinger.stream().collect(groupingBy( rs -> new StillingAgressoOrgIdOgNavn(rs.getStillingAgressoId(), rs.getStillingOrgenhetNavn(), rs.getRessursAgressoId(), rs.getRessursOrgenhetNiv())));
        List<StillingPeriode> perioderGruppert = new ArrayList<>();

        // Sorter og slå sammen sammenhengende perioder
        grupperteStillinger.forEach( (stillingAgressoOrgIdOgNavn, grupperteRessursStillinger)  -> {
            log.debug("Slår sammen for {}", stillingAgressoOrgIdOgNavn);
            var sorted = grupperteRessursStillinger.stream().sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom)).toList();
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("Perioder:");
                sorted.forEach( rs -> sb.append("\n\t%s - %s".formatted(rs.getStillingDatoFom(), rs.getStillingDatoTom())));
                log.debug(sb.toString());
            }
            LocalDate periodStart = sorted.get(0).getStillingDatoFom();
            LocalDate previousEnd = sorted.get(0).getStillingDatoTom();
            for (int i = 1; i < sorted.size(); i++) {
                var rs = sorted.get(i);
                var start = rs.getStillingDatoFom();
                var end =  rs.getStillingDatoTom();
                if(start.isEqual(previousEnd.plusDays(1))) {
                    //Stillings raden er  med fortsettelse på forrige stilling - slå sammen
                    previousEnd = end;
                } else if (start.isAfter(previousEnd)){
                    // Her er det brudd i stilling - Lag en orgtilknytning på forrige periode
                    perioderGruppert.add(
                        new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
                    );
                    periodStart = start;
                    previousEnd = end;
                } else {
                    // Her er det overlapp i datoer på samme stillingAgressoId, stillingOrgenhetNavn og stillingRessursAgressoId
                    // Dette håndterer vi ikke per nå
                    log.warn("Overlapp i stillingsdatoer for '{}' -> forrige periode [{} - {}] og denne stillingen [{} - {}]", stillingAgressoOrgIdOgNavn, periodStart, previousEnd, start, end);
                }
            }
            // Siste RessursStilling er slått sammen
            perioderGruppert.add(
                new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
            );
        });
        // Nå er alle stillingPeriodene gruppert på periode for agressoId.
        // Finn så NOM orgenhet for disse. Hvis gyldig periode for enhet ikke dekker hele perioden for stillingPerioden, så må stillingperioden splittes.
        List<StillingPeriode> perioderGruppertMedOrgenhet = new ArrayList<>();
        perioderGruppert.forEach(stillingPeriode -> {
            List<IOrgenhet> nomEnheter = Collections.emptyList();
            // Kan kun matche for på stilling agressoId hvis denne finnes og er presis
            if (stillingPeriode.stilling.agressoId().isPresent() && stillingPeriode.stilling.agressoId().get().length() == 7) {
                nomEnheter = finnNomEnheterForIdOgNavn(stillingPeriode.stilling.agressoId.get(), stillingPeriode.stilling.enhetNavn, stillingPeriode.gyldigFom, stillingPeriode.gyldigTom);
            } else if (
                // Hvis det er en aktiv periode, og ressurs agressoId er presis
                // så kan vi matche på ressurs agressoId, hvis denne matcher stilling eller stilling er tom
                (isCurrentPeriode(stillingPeriode) && stillingPeriode.stilling.ressursAgressoId.isPresent() && stillingPeriode.stilling.ressursAgressoId.get().length() == 7)
                &&
                (stillingPeriode.stilling.agressoId().isEmpty() || stillingPeriode.stilling.ressursAgressoId.get().startsWith(stillingPeriode.stilling.agressoId.get()))) {

                nomEnheter = finnNomEnheterForId(stillingPeriode.stilling.ressursAgressoId.get(), stillingPeriode.gyldigFom, stillingPeriode.gyldigTom);
            } else {
                // Kan ikke finne orgenhet for stillingPeriode siden agressoId tilgjengelig ikke er presis (7 lang)
                log.warn("StillingPeriode er ikke presis nok på stilling/ressurs sin agressoId til å kunne finne NOM enhet denne tilhører. Den vil ikke behandles: {}", stillingPeriode);
            }

            if (nomEnheter.size() == 0) {
                // Klarer ikke finne NOM orgenhet for stillingPeriode/agressoId
                log.warn("Klarer ikke finne NOM orgenhet for stillingPeriode/agressoId. Den vil ikke behandles: {}", stillingPeriode);
            } else if (nomEnheter.size() == 1) {
                // Exact one match
                perioderGruppertMedOrgenhet.add(new StillingPeriode(stillingPeriode, nomEnheter.get(0)));
            } else {
                // Match på flere orgenheter i NOM. Må splitte stillingPerioden...
                log.debug("StillingPerioden {} matcher flere NOM Orgenheter\n  {}", stillingPeriode, orgenheterToString(nomEnheter));
                List<StillingPeriode> nyeStillingPerioder = new ArrayList<>();
                AtomicReference<LocalDate> nextGyldigFom = new AtomicReference<>(stillingPeriode.gyldigFom);
                nomEnheter.stream()
                    .sorted(Comparator.comparing(IOrgenhet::getGyldigFom))
                    .forEach( oe -> {
                        nyeStillingPerioder.add(new StillingPeriode(stillingPeriode.stilling, nextGyldigFom.get(), oe.getGyldigTom().orElse(NO_END_DATE), oe));
                        nextGyldigFom.set(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1)));
                    });
                log.debug("Splittet opp i {}", nyeStillingPerioder);
                perioderGruppertMedOrgenhet.addAll(nyeStillingPerioder);
            }
        });

        return perioderGruppertMedOrgenhet;
    }

    // Alle ORGENHET enheter med agressoId som overlapper i perioden
    private List<IOrgenhet> finnNomEnheterForId(String agressoId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.matchByAgressoId(agressoId).stream()
            .filter( oe ->
                        "ORGENHET".equals(oe.getOrgNiv()) &&
                        (gyldigTom.isAfter(oe.getGyldigFom().minusDays(1)) && gyldigFom.isBefore(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1))))
                ).toList();
    }


    // Alle ORGENHET enheter med agressoId og navn som overlapper i perioden
    private List<IOrgenhet> finnNomEnheterForIdOgNavn(String agressoId, String enhetNavn, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.matchByAgressoId(agressoId).stream()
            .filter( oe ->
                    "ORGENHET".equals(oe.getOrgNiv()) &&
                    enhetNavn.equals(oe.getNavn()) &&
                    (gyldigTom.isAfter(oe.getGyldigFom().minusDays(1)) && gyldigFom.isBefore(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1))))
            ).toList();
    }

    record StillingAgressoOrgIdOgNavn(Optional<String> agressoId, String enhetNavn, Optional<String> ressursAgressoId, Optional<String> ressursAgressoOrgNiv) {
        @Override
        public String toString() {
            return "HRRES_STILLING[" + agressoId.orElse("NULL") + "-" + enhetNavn + "]/HRRES_RESSURS[" + ressursAgressoId.orElse("NULL") + "-" + ressursAgressoOrgNiv.orElse("NULL") + "]";
        }
    }

    record StillingPeriode(StillingAgressoOrgIdOgNavn stilling, LocalDate gyldigFom, LocalDate gyldigTom, IOrgenhet orgenhet) {
        public StillingPeriode(StillingPeriode copy, IOrgenhet orgenhet) {
            this(copy.stilling, copy.gyldigFom, copy.gyldigTom, orgenhet);
        }

        @Override
        public String toString() {
            return stilling.toString() + ": " + gyldigFom + " - " + gyldigTom + " for nom orgenhet [" + (orgenhet != null ? orgenhet.getId() + "-" + orgenhet.getNavn() : " - " ) + "]";
        }
    }

    // Shameless duplication from DvhRestConsumer
    private JsonNode dvhExchange(String urlPath) {
        val headers = new HttpHeaders();
        headers.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.proxy().aadClientId()).serialize());

        return restTemplate.exchange(appProperties.proxy().baseUrl() + urlPath, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return  "%02d:%02d:%02d".formatted((Integer)duration.toHoursPart(), (Integer)duration.toMinutesPart(), (Integer)duration.toSecondsPart());
    }

    private String stillingperioderToString(List<StillingPeriode> stillingPerioder) {
        return stillingperioderToString(stillingPerioder, false);
    }
    private String stillingperioderToString(List<StillingPeriode> stillingPerioder, boolean indent) {
        StringBuffer str = new StringBuffer();
        stillingPerioder.forEach( s -> str.append(indent ? "  " : "").append(s).append("\n"));
        return str.toString();
    }

    private String orgenheterToString(List<IOrgenhet> orgenheter) {
        StringBuffer str = new StringBuffer();
        orgenheter.forEach( oe ->
            str.append("  ")
                .append(oe.getId())
                .append("-")
                .append(oe.getNavn())
                .append(" [")
                .append(oe.getGyldigFom())
                .append(" - ")
                .append(oe.getGyldigTom().orElse(NO_END_DATE))
                .append("]")
        );
        return str.toString();
    }
}
