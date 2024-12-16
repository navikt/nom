package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.ShadowOrgtilknytningerKommunaleRepository;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Denne lager ingen faktiske orgtilknytninger
 * Den parser om kommunale ressurser mangler stillinger i Unit4 og smatidig ikke har en OT i NOM (e.g. mastret)
 * Så vil den benytte seg av DVH sin OrgenhetPaaRessurs info som er en best effort tolkning av Remedy enhet etc for å plassere i organisasjonen.
 * Og lage en liksom OT - som kun tas vare på i ShadowOrgtilknytningerKommunaleRepository
 * NOM bruker aldri denne infoen selv, men den leveres til Omada, slik at Omada får tilknytning på 'aktive' Kommmunale ressurser som (enda) ikke har fått noen Stilling i Unit4
 *
 * Når NOM blir full master for alle kommunale så kan dette fjernes
 */
@Slf4j
@Service
public class KommunaleRessurserLoader {
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private final String kommunaleRessurserResponseNode;
    private KommunaleRessurserProcessor kommunaleRessurserProcessor;
    private RessursStillingLoader ressursStillingLoader;
    private RessursStillingStrukturerer ressursStillingStrukturerer;
    private ShadowOrgtilknytningerKommunaleRepository shadowOrgtilknytningerKommunaleRepository;
    private final RessursStateRepository ressursStateRepository;
    private final DvhOrdsService dvhOrdsService;
    private final Unleash unleash;

    public KommunaleRessurserLoader(
        NomProxyClient nomProxyClient, AppProperties appProperties, KommunaleRessurserProcessor kommunaleRessurserProcessor,
        @Value("${app.dvh-load.nom-kommunale-ressurser-respons-navn}") String kommunaleRessurserResponseNode,
        RessursStillingLoader ressursStillingLoader,
        RessursStillingStrukturerer ressursStillingStrukturerer,
        ShadowOrgtilknytningerKommunaleRepository shadowOrgtilknytningerKommunaleRepository,
        RessursStateRepository ressursStateRepository, DvhOrdsService dvhOrdsService, Unleash unleash
    ) {
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.kommunaleRessurserProcessor = kommunaleRessurserProcessor;
        this.kommunaleRessurserResponseNode = kommunaleRessurserResponseNode;
        this.ressursStillingLoader = ressursStillingLoader;
        this.ressursStillingStrukturerer = ressursStillingStrukturerer;
        this.shadowOrgtilknytningerKommunaleRepository = shadowOrgtilknytningerKommunaleRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.dvhOrdsService = dvhOrdsService;
        this.unleash = unleash;
    }

    public void loadAndProcess() {
        log.info("Starter last og behandling av kommunale ressurser og orgtilknytninger");
        LocalDateTime start = LocalDateTime.now();

        var aktiveKommunaleNavidenterINom = getAktiveKommunaleNavidenter();

        List<DvhRessurs> dvhKommunaleRessurser = loadKommunaleForNavidenter(aktiveKommunaleNavidenterINom);
        List<String> kommunaleNavidenter = dvhKommunaleRessurser.stream().map(DvhRessurs::getNavIdent).toList();

        Map<String, List<StillingPeriode>> stillingPerioderPerNavident = loadStillinger(kommunaleNavidenter);

        log.info("{} kommunale ressurser med U4 stillinger", stillingPerioderPerNavident.size());

        Map<String, OrgtilknytningState> tmpKommunaleOrgtilknytninger = new ConcurrentHashMap<>();
        dvhKommunaleRessurser.forEach(r -> {
            var tmpOT = kommunaleRessurserProcessor.process(r, stillingPerioderPerNavident.get(r.getNavIdent()));
            if (tmpOT.isPresent()) {
                tmpKommunaleOrgtilknytninger.put(r.getNavIdent(), tmpOT.get());
            }
        });

        shadowOrgtilknytningerKommunaleRepository.resetWithNewData(tmpKommunaleOrgtilknytninger);

        log.info(
            "last og behandling av kommunale ressurser og orgtilknytninger. Har laget {} temporære kommunale Orgtilknytninger. Tok {}",
            tmpKommunaleOrgtilknytninger.size(),
            durationAsH24(start)
        );
    }

    private List<String> getAktiveKommunaleNavidenter() {
        return ressursStateRepository.getAll().stream()
            .filter(r -> (r.startdato().isEmpty() || r.startdato().get().isBefore(LocalDate.now()))
                && (r.sluttdato().isEmpty() || r.sluttdato().get().isAfter(LocalDate.now()))
            )
            .filter(r -> r.sektor().equals(Sektor.NAV_KOMMUNAL))
            .map(RessursState::navident)
            .toList();
    }

    private Map<String, List<StillingPeriode>> loadStillinger(List<String> kommunaleNavidenter) {
        List<DvhRessursStilling> kommunaleRessursStilllinger =
            ressursStillingLoader.load().stream()
                .filter(rs -> rs.getNavident().isPresent() && kommunaleNavidenter.contains(rs.getNavident().get()))
                .toList();

        return ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(kommunaleRessursStilllinger);
    }

    List<DvhRessurs> loadKommunaleForNavidenter(List<String> aktiveKommunaleNavidenter) {
        LocalDateTime start = LocalDateTime.now();

        List<DvhRessurs> dvhKommunaleRessurser;
        if (unleash.isEnabled("nom.dvh.last_kommunale_fra_ords")) {
            dvhKommunaleRessurser = dvhOrdsService.getKommunaleRessurser(aktiveKommunaleNavidenter);
        } else {
            String url = appProperties.clients().nomProxy().kommunaleRessurserPath();
            log.info("Laster Dvh Kommunale Ressurser fra {}", url);
            var jsonRessurser = dvhExchange(url);
            dvhKommunaleRessurser = StreamSupport.stream(jsonRessurser.get(kommunaleRessurserResponseNode).spliterator(), true)
                .map(r -> new DvhRessurs(r, true))
                .distinct()
                .filter(r -> aktiveKommunaleNavidenter.contains(r.getNavIdent()))
                .toList();
        }

        getOrdsKommunaleRessurserAndCompare(dvhKommunaleRessurser, aktiveKommunaleNavidenter);

        log.info("Lastet {} Dvh Kommunale Ressurser. Tok {}", dvhKommunaleRessurser.size(), durationAsH24(start));
        return dvhKommunaleRessurser;
    }

    private void getOrdsKommunaleRessurserAndCompare(List<DvhRessurs> tdvDvhRessurser, List<String> aktiveKommunaleNavidenter) {
        try {
            var ordsDvhRessurser = dvhOrdsService.getKommunaleRessurser(aktiveKommunaleNavidenter);

            boolean erLikt = true;
            if (ordsDvhRessurser.size() != tdvDvhRessurser.size()) {
                log.error("Antall kommunale ressurser fra TDV og ORDS er ulik. TDV: {}, ORDS: {}", tdvDvhRessurser.size(), ordsDvhRessurser.size());
                erLikt = false;
            }

            var ordsMap = ordsDvhRessurser.stream().collect(Collectors.toMap(DvhRessurs::getNavIdent, Function.identity()));
            int idxSkip = tdvDvhRessurser.size()/200;
            for (int i = 0; i < 200; i++) {
                int idx = i * idxSkip;
                var dvhRessursTdv = tdvDvhRessurser.get(idx);
                var dvhRessursOrds = ordsMap.get(dvhRessursTdv.getNavIdent());
                if (dvhRessursOrds == null || !DvhRessurs.compare(dvhRessursTdv, dvhRessursOrds)) {
                    log.error("kommunale ressurs fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , dvhRessursTdv.getRawData().toPrettyString()
                        , dvhRessursOrds == null ? null : dvhRessursOrds.getRawData().toPrettyString());
                    erLikt = false;
                }
            }
            if (erLikt) {
                log.info("Last av kommunale ressurser fra ORDS (nom_ords_ansatte_buckets.json) er lik last fra TDV (NOM_ANSATTE_BUCKETS_v2)");
            } else {
                log.error("Last av kommunale ressurser fra ORDS (nom_ords_ansatte_buckets.json) er annerledes enn last fra TDV (NOM_ANSATTE_BUCKETS_v2)");
            }
        } catch (Exception e) {
            log.error("Feilet i last av kommunale ressurser fra ORDS (nom_ords_ansatte_buckets.json)", e);
        }
    }

    // Shameless duplication from DvhRestConsumer
    private JsonNode dvhExchange(String urlPath) {
        return nomProxyClient.getWebClient().get()
            .uri(urlPath)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
