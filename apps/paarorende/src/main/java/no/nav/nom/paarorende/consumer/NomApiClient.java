package no.nav.nom.paarorende.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class NomApiClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String IS_AUTHORIZED_RESSURS_SKRIV_PATH = "app/authorizedRessursRedigering/isAuthorized";
    private static final String IS_AUTHORIZED_RESSURSER_SKRIV_PATH = "app/authorizedRessursRedigering/isAuthorizedForNavidents";
    private static final String HAS_NAVIDENT_ACCESS_TO_RESOURCES_FOR_ORGENHET_PATH = "app/authorizedRessursRedigering/hasNavidentAccessToResourcesForOrgenhet";
    private static final String ORGENHETER_PATH = "app/orgenheter";



    private final WebClient nomApi;

    public NomApiClient(WebClient webClient) {
        this.nomApi = webClient;
    }

    public boolean hasAccess(String bruker, String ressurs) {
        log.info("sending hasAccess call to NOM for user and ressurs: {}, {}", bruker, ressurs);
        return Boolean.TRUE.equals(nomApi.post()
            .uri(IS_AUTHORIZED_RESSURS_SKRIV_PATH)
            .bodyValue(new IsAuthorizedRessursWriteRequest(bruker, ressurs))
            .retrieve()
            .bodyToMono(Boolean.class)
            .block());
    }

    public boolean hasAccess(String bruker, List<String> ressurser) {
        log.info("sending hasAccess call to NOM for user and ressurser: {}, {}", bruker, ressurser);
        return Boolean.TRUE.equals(nomApi.post()
            .uri(IS_AUTHORIZED_RESSURSER_SKRIV_PATH)
            .bodyValue(new IsAuthorizedRessurserWriteRequest(bruker, ressurser))
            .retrieve()
            .bodyToMono(Boolean.class)
            .block());
    }

    public boolean hasNavidentAccessToResourcesForOrgenhet(String navident, String orgenhetId) {
        log.info("sending hasNavidentAccessToResourcesForOrgenhet call to NOM for user and orgenhet: {}, {}", navident, orgenhetId);
        return Boolean.TRUE.equals(nomApi.get()
            .uri(uriBuilder -> uriBuilder
                .path(HAS_NAVIDENT_ACCESS_TO_RESOURCES_FOR_ORGENHET_PATH + "/{navident}/{orgenhetId}")
                .build(navident, orgenhetId))
            .retrieve()
            .bodyToMono(Boolean.class)
            .block());
    }

    public boolean isNavidentLederForOrgenhet(String navident, String orgEnhetId) {
        log.info("isNavidentLederForOrgenhet: {}, {}", navident, orgEnhetId);
        return Boolean.TRUE.equals(nomApi.get()
            .uri(uriBuilder -> uriBuilder
                .path(ORGENHETER_PATH + "/isNavIdent/{navident}/LederForOrgenhet/{orgEnhetId}")
                .build(navident, orgEnhetId))
            .retrieve()
            .bodyToMono(Boolean.class)
            .block());
    }

    public List getRessurserInAndUnderliggendeOrgenheter(String orgEnhetId) {
        return nomApi.get()
            .uri(uriBuilder -> uriBuilder
                .path(ORGENHETER_PATH + "/getRessurserInAndUnderliggendeOrgenheter/{orgEnhetId}")
                .build(orgEnhetId))
            .retrieve()
            .bodyToMono(List.class)
            .block();
    }

    record IsAuthorizedRessursWriteRequest(
        String bruker,
        String ressurs
    ) {}

    record IsAuthorizedRessurserWriteRequest(
        String bruker,
        List<String> ressurser
    ) {}
}
