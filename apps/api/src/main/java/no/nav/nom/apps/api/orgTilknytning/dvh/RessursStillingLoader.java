package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.KorrigerOrgtilknytningDato;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Service
public class RessursStillingLoader {
    public static final String UNLEASH_LAST_NY_ORGTILKNYTNING = "nom.orgtilknytning.last";
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
            log.debug("Gruppert ressursStilligner på {} navidenter", ressursStillingerGruppert.keySet().size());

            ressursStillingerGruppert.forEach((navident, rsList) -> {
                log.debug("Strukturer ressursStillinger for navident {} til orgtilknytninger", navident);
                var stillingerGruppert = strukturerNavidentensRessursStillinger(rsList);
                if (log.isDebugEnabled()) {
                    log.debug("Resultat:\n{}", stillingperioderToString(stillingerGruppert, true));
                    process(navident, stillingerGruppert);
                }
            });
        }
    }

    public void process(String navident, List<StillingPeriode> stillingPerioder) {
        Set<String> behandledeOrgtilknytningerFids = new HashSet<>();

        var duplikateCurrent = stillingPerioder.stream().filter( sp -> isCurrentPeriode(sp));
        if (duplikateCurrent.count() > 1) {
            log.debug("Navid {} har duplikate aktive stillinger:\n{}", navident, stillingperioderToString(stillingPerioder.stream().filter( sp -> isCurrentPeriode(sp)).toList(), true));
        }

        // get original orgtilknytninger
        var orgtilknytninger = orgtilknytningStateService.getKoblingerByNavIdent(navident);
        stillingPerioder.forEach( stillingPeriode -> {
            if (isHistoricPeriod(stillingPeriode)) {  // Historisk stillingPeriode fra DVH
                if (orgtilknytninger == null) {
                    log.debug("Historisk stillingPeriode fra DVH ({}) men NOM har ingen orgtilknytninger for ident {}", stillingPeriode, navident);
                } else {
                    // Finn orgtilknytninger på matchende datoperiode
                    var sammePeriode = orgtilknytninger.stream()
                        .filter(ot -> stillingPeriode.gyldigFom.equals(ot.gyldigFom()) && stillingPeriode.gyldigTom().equals(ot.gyldigTom().get()));
                    // Valider om noen av disse matcher på agressoId - ok om stillingperiode ikke metcher til 7 sifret, vi tar det for god fisk uanssett
                    var matcherAgressoId = sammePeriode.filter(ot -> orgtiknytningEnhetMatcherAgressoId(stillingPeriode, ot));
                    if (matcherAgressoId.count() > 0) {
                        log.warn("Historisk stillingPeriode fra DVH ({}) matcher flere orgtilknytninger {}", stillingPeriode, matcherAgressoId);
                        // Burde ikke vært her - men vi markerer alle disse tidligere orgtilknytningene som behandlet
                        behandledeOrgtilknytningerFids.addAll(matcherAgressoId.map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                    } else if (matcherAgressoId.count() == 1) {
                        // Historikken stemmer - move on
                        behandledeOrgtilknytningerFids.add(matcherAgressoId.findFirst().get().fid());
                    } else {
                        // Har en historisk stillingPeriode fra DVH, men finner ikke tilsvarende historisk orgtilknytning i state på både periode og agressoId
                        if (sammePeriode.count() > 0) {
                            // Vi finner minst en orgtilknytning for samme historisk periode
                            // Da tar vi State historikken for god fisk.
                            behandledeOrgtilknytningerFids.addAll(matcherAgressoId.map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                        } else {
                            // Nå derimot kan det hende vi må avslutte en aktiv orgtilknytning, eller endre gyldigTOM
                            var matcherStartDatoOgAgressoId = orgtilknytninger.stream()
                                .filter(ot -> stillingPeriode.gyldigFom.equals(ot.gyldigFom()) && orgtiknytningEnhetMatcherAgressoId(stillingPeriode, ot));
                            if (matcherStartDatoOgAgressoId.count() == 0) {
                                // Matcher ingen i NOM, DVH historisk finnes ikke i NOM, så da behandler vi den ikke.
                                // TODO: Har vi kommet til ett sted hvor gyldigFOM på historisk periode kan ha endret seg i DVH?
                                log.debug("Historisk stillingPeriode fra DVH ({}) matcher ingen orgtilknytninger {}", stillingPeriode, matcherAgressoId);
                            } else if (matcherStartDatoOgAgressoId.count() > 1) {
                                // Matcher flere i NOM - dette må være feil!....
                                log.warn("Historisk stillingPeriode fra DVH ({}) matcher flere orgtilknytninger {}", stillingPeriode, matcherAgressoId);
                                // Burde ikke vært her - men vi markerer alle disse tidligere orgtilknytningene som behandlet
                                behandledeOrgtilknytningerFids.addAll(matcherAgressoId.map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                            } else {
                                // Vi matcher 1. Og trenger å endre GyldigTOM for denne -> Enten korriger, eller avvikle
                                endreOrgtilknytningGyldigTOM(
                                    matcherStartDatoOgAgressoId.findFirst().get(),
                                    stillingPeriode,
                                    stillingperioderToString(stillingPerioder)
                                );
                                behandledeOrgtilknytningerFids.addAll(matcherAgressoId.map(OrgtilknytningState::fid).collect(Collectors.toSet()));
                            }
                        }
                    }
                }
            } else if (isCurrentPeriode(stillingPeriode)) {
                // DVH Perioden er aktiv i dag

                // Aktiv periode kan sjekkes for primær tilkynting
                boolean erPrimær = erStillingPeriodenPrimær(stillingPeriode);


                // TODO: IMPLEMENT!!
            } else {
                // Perioden er fremtidig
                // TODO: IMPLEMENT!!
            }
        });
    }

    private boolean erStillingPeriodenPrimær(StillingPeriode stillingPeriode) {
        return true;
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
                    originalOrgtilknytning.gyldigTom(),
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
                    originalOrgtilknytning.gyldigTom().get(),
                    metadata,
                    additionalData
                )
            );
        }
    }

    /**
     * Vil matche på overnode-agressoId
     * Kan brukes på historiske rader, hvor DVH kan angi overnode og ikke løvnode (ORGENHET)
     * Altså en stillingPeriode med angivelse av f.eks. ORGNIV3 node '060200' vil da matche en orgtilknytning i nom for ORGENHET '0602002'
     */
    private boolean orgtiknytningEnhetMatcherAgressoId(StillingPeriode stillingPeriode, OrgtilknytningState ot) {
        return orgenhetService.getById(ot.orgEnhetId()).get().getAgressoId().startsWith(stillingPeriode.stilling.agressoId.orElse("NULL"));
    }

    private boolean isCurrentPeriode(StillingPeriode stillingPeriode) {
        return isCurrentOrFuturePeriod(stillingPeriode) && stillingPeriode.gyldigFom().isBefore(tomorrow());
    }

    private boolean isHistoricPeriod(StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigTom.isBefore(tomorrow());
    }

    private boolean isCurrentOrFuturePeriod(StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigTom.isAfter(yesterday());
    }
    private LocalDate yesterday() { return LocalDate.now().minusDays(1); }

    private LocalDate tomorrow() { return LocalDate.now().plusDays(1); }

    protected List<DvhRessursStilling> load() {
        log.info("Laster ressursStillinger fra {}", appProperties.proxy().ressursStillingerPath());
        LocalDateTime start = LocalDateTime.now();
        val jsonRessursStillinger = dvhExchange(appProperties.proxy().ressursStillingerPath());
        val listField = jsonRessursStillinger.fields().next();
        log.debug("Lastet ressursStillinger: Første field: {} med {} ant noder", listField.getKey(), listField.getValue().size());
        List<DvhRessursStilling> ressursStillinger = StreamSupport.stream(jsonRessursStillinger.get(ressursStillingerResponseNode).spliterator(), true)
            .map(DvhRessursStilling::new)
            .collect(Collectors.toList());
        log.info("Lastet {} ressursStillinger. Tok {}", ressursStillinger.size(), durationAsH24(start));
        return ressursStillinger;
    }

    public Map<String, List<DvhRessursStilling>> grupperPerNavident(List<DvhRessursStilling> ressursStillinger) {
        return ressursStillinger.stream()
            .filter(rs -> rs.getNavident().isPresent())
            .collect(groupingBy(rs -> rs.getNavident().get()));
    }

    public List<StillingPeriode> strukturerNavidentensRessursStillinger(List<DvhRessursStilling> ressursStillinger) {
        // Grupper per org tilknytning
        // Altså:
        //      A. Per Stillingens - AgressoId+enhetNavn         (stillingAgressoId/stillingOrgenhetNavn)
        //      B. Splitt også på hva ressursen har nå orgenhet  (ressurs på orgenhet)
        //
        // TODO: Stemmer denne kommentaren?     Remedy enhet og ressurs orgenhet er nå-tid info, og skal kun vurderes på den stillingen som er nå

        // TODO: Hva med primærtilknytning?

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
                        new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd)
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
                new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd)
            );
        });
        return perioderGruppert;
    }

    record StillingAgressoOrgIdOgNavn(Optional<String> agressoId, String enhetNavn, Optional<String> ressursAgressoId, Optional<String> ressursAgressoOrgNiv) {
        @Override
        public String toString() {
            return "HRRES_STILLING[" + agressoId.orElse("NULL") + "-" + enhetNavn + "]/HRRES_RESSURS[" + ressursAgressoId.orElse("NULL") + "-" + ressursAgressoOrgNiv.orElse("NULL") + "]";
        }
    };

    record StillingPeriode(StillingAgressoOrgIdOgNavn stilling, LocalDate gyldigFom, LocalDate gyldigTom) {
        @Override
        public String toString() {
            return stilling.toString() + ": " + gyldigFom + " - " + gyldigTom;
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
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    private String stillingperioderToString(List<StillingPeriode> stillingPerioder) {
        return stillingperioderToString(stillingPerioder, false);
    }
    private String stillingperioderToString(List<StillingPeriode> stillingPerioder, boolean indent) {
        StringBuffer str = new StringBuffer();
        stillingPerioder.forEach( s -> str.append((indent ? "  " : "") + s + "\n"));
        return str.toString();
    }

}
