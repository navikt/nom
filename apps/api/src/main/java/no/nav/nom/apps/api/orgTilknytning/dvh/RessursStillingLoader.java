package no.nav.nom.apps.api.orgTilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Service
public class RessursStillingLoader {
    public static final String UNLEASH_LAST_NY_ORGTILKNYTNING = "nom.orgtilknytning.last";
    private final TokenRepository tokenRepository;
    private final DvhRepository dvhRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;

//    @Value("${app.kafka.orgEnhet.stateTopic}") String stateTopic,



    private final Unleash unleash;

    public RessursStillingLoader(TokenRepository tokenRepository, DvhRepository dvhRepository, RestTemplate restTemplate, AppProperties appProperties,
                                 @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
                                 Unleash unleash) {
        this.tokenRepository = tokenRepository;
        this.dvhRepository = dvhRepository;
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
                    log.debug("Resultat:");
                    stillingerGruppert.forEach( s -> log.debug("\t{}", s));
                }
            });
        }
    }

    private List<DvhRessursStilling> load() {
        log.info("Laster ressursStillinger fra {}", appProperties.proxy().ressursStillingerPath());
        LocalDateTime start = LocalDateTime.now();
        val jsonRessursStillinger = dvhExchange(appProperties.proxy().ressursStillingerPath());
        val listField = jsonRessursStillinger.fields().next();
        log.debug("Lastet ressursStillinger: Første field: {} med {} ant noder", listField.getKey(), ((ArrayNode)listField.getValue()).size());
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

        // grupper først på Stillingens - stilling enhet/navn +enhetNavn+ressurs (A+B)
        var grupperteStillinger = ressursStillinger.stream().collect(groupingBy( rs -> new StillingAgressoOrgIdOgNavn(rs.getStillingAgressoId(), rs.getStillingOrgenhetNavn(), rs.getRessursAgressoId(), rs.getRessursOrgenhetNiv())));
        List<StillingPeriode> perioderGruppert = new ArrayList<>();

        // Sorter og slå sammen sammenhengende perioder
        grupperteStillinger.forEach( (stillingAgressoOrgIdOgNavn, grupperteRessursStillinger)  -> {
            log.debug("Slår sammen for {}", stillingAgressoOrgIdOgNavn);
            var sorted = grupperteRessursStillinger.stream().sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom)).toList();
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("Perioder:");
                sorted.forEach( rs -> log.debug("\n\t{} - {}", rs.getStillingDatoFom(), rs.getStillingDatoTom()));
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
}
