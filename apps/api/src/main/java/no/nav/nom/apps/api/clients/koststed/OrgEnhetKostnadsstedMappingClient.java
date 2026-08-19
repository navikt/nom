package no.nav.nom.apps.api.clients.koststed;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OrgEnhetKostnadsstedMappingClient {

    private static final String CLIENT_REGISTRATION_ID = "orgenhet-kostnadssted-mapping";

    private final RestClient restClient;

    public OrgEnhetKostnadsstedMappingClient(
        OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
        AppProperties appProperties,
        RestClient.Builder builder
    ) {
        OAuth2ClientHttpRequestInterceptor oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(oAuth2AuthorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(_ -> CLIENT_REGISTRATION_ID);

        this.restClient = builder
            .baseUrl(appProperties.clients().orgEnhetKostnadsstedMapping().baseUrl())
            .requestInterceptor(oauth2Interceptor)
            .defaultHeaders(httpHeaders -> {
                httpHeaders.add("Nav-Consumer-Id", appProperties.name());
                httpHeaders.add("Nav-Call-Id", appProperties.name());
            })
            .build();
    }

    public Optional<KostnadsstedMappingResponseDTO> hentKostnadsstedMapping(String orgenhetId) {
        try {
            return Optional.ofNullable(
                restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/mapping").queryParam("orgenhetid", orgenhetId).build())
                    .retrieve()
                    .body(KostnadsstedMappingResponseDTO.class)
            );
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.error("Uforutsett feil ved henting av kostnadssted {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<KostnadsstedMappingResponseDTO> hentKostnadsstedMappingerForGittToppNode(String toppNodeOrgEnhetId) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/mappinger").queryParam("topOrgenhet", toppNodeOrgEnhetId).build())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    public List<KostnadsstedResponseDTO> hentAlleKostnadssteder() {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/mapping").build())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    public KostnadsstedMappingResponseDTO opprettKostnadsstedMapping(String orgEnhetId, String kostnadsstedId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return restClient.post()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/mapping").build())
            .body(new CreateOrgenhetKostnadsstedMappingExternalRequest(
                kostnadsstedId,
                orgEnhetId,
                gyldigFom,
                gyldigTom
            ))
            .retrieve()
            .toEntity(KostnadsstedMappingResponseDTO.class)
            .getBody();
    }
}
