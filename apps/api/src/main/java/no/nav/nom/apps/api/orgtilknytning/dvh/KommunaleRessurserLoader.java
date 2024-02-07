    package no.nav.nom.apps.api.orgtilknytning.dvh;

    import com.fasterxml.jackson.databind.JsonNode;
    import lombok.extern.slf4j.Slf4j;
    import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
    import no.nav.nom.apps.api.common.AppProperties;
    import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
    import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.ShadowOrgtilknytningerKommunaleRepository;
    import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
    import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
    import no.nav.nom.contract.nom.Sektor;
    import no.nav.nom.contract.nom.state.OrgtilknytningState;
    import no.nav.nom.contract.nom.state.RessursState;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;

    import java.time.Duration;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;
    import java.util.stream.StreamSupport;

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

    public KommunaleRessurserLoader(NomProxyClient nomProxyClient, AppProperties appProperties, KommunaleRessurserProcessor kommunaleRessurserProcessor,
                                    @Value("${app.dvh-load.nom-kommunale-ressurser-respons-navn}") String kommunaleRessurserResponseNode,
                                    RessursStillingLoader ressursStillingLoader, RessursStillingStrukturerer ressursStillingStrukturerer, ShadowOrgtilknytningerKommunaleRepository shadowOrgtilknytningerKommunaleRepository, RessursStateRepository ressursStateRepository
    ) {
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.kommunaleRessurserProcessor = kommunaleRessurserProcessor;
        this.kommunaleRessurserResponseNode = kommunaleRessurserResponseNode;
        this.ressursStillingLoader = ressursStillingLoader;
        this.ressursStillingStrukturerer = ressursStillingStrukturerer;
        this.shadowOrgtilknytningerKommunaleRepository = shadowOrgtilknytningerKommunaleRepository;
        this.ressursStateRepository = ressursStateRepository;
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
        dvhKommunaleRessurser.forEach( r -> {
            var tmpOT = kommunaleRessurserProcessor.process(r, stillingPerioderPerNavident.get(r.getNavIdent()));
            if (tmpOT.isPresent()) {
                tmpKommunaleOrgtilknytninger.put(r.getNavIdent(), tmpOT.get());
            }
        });

        shadowOrgtilknytningerKommunaleRepository.resetWithNewData(tmpKommunaleOrgtilknytninger);

        log.info("last og behandling av kommunale ressurser og orgtilknytninger. Har laget {} temporære kommunale Orgtilknytninger. Tok {}", tmpKommunaleOrgtilknytninger.size(), durationAsH24(start));
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
                .filter( rs -> rs.getNavident().isPresent() && kommunaleNavidenter.contains(rs.getNavident().get()))
                .toList();

        return ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(kommunaleRessursStilllinger);
    }

    List<DvhRessurs> loadKommunaleForNavidenter(List<String> aktiveKommunaleNavidenter) {
        String url = appProperties.clients().nomProxy().kommunaleRessurserPath();

        log.info("Laster Dvh Kommunale Ressurser fra {}", url);
        LocalDateTime start = LocalDateTime.now();

        var jsonRessurser = dvhExchange(url);
        var dvhKommunaleRessurser = StreamSupport.stream(jsonRessurser.get(kommunaleRessurserResponseNode).spliterator(), true)
            .map(r -> new DvhRessurs(r, true))
            .distinct()
            .filter(r -> aktiveKommunaleNavidenter.contains(r.getNavIdent()))
            .toList();

        log.info("Lastet {} Dvh Kommunale Ressurser. Tok {}", dvhKommunaleRessurser.size(), durationAsH24(start));
        return dvhKommunaleRessurser;
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
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
