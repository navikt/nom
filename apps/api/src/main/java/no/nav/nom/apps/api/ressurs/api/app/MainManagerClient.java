package no.nav.nom.apps.api.ressurs.api.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class MainManagerClient {

    private final WebClient webClient;
    private final AppProperties.MainManager mainManagerProperties;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public MainManagerClient(AppProperties appProperties, WebClient.Builder builder) {
        this.mainManagerProperties = appProperties.clients().mainManager();
        this.webClient = builder.clone()
            .baseUrl(mainManagerProperties.baseUrl())
            .build();
    }

    private String getToken() {
        CachedToken cached = cachedToken.get();
        if (cached != null && cached.isValid()) {
            return cached.accessToken();
        }

        log.info("Henter token fra MainManager");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", mainManagerProperties.username());
        formData.add("password", mainManagerProperties.password());
        formData.add("grant_type", mainManagerProperties.grantType());

        var tokenResponse = webClient.post()
            .uri(mainManagerProperties.tokenPath())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(MainManagerTokenResponse.class)
            .block();

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new IllegalStateException("Fikk ikke token fra MainManager");
        }

        var newToken = new CachedToken(
            tokenResponse.accessToken(),
            Instant.now().plusSeconds(tokenResponse.expiresIn() - 30)
        );
        cachedToken.set(newToken);
        return newToken.accessToken();
    }

    public List<FysiskBrukersted> getFysiskeBrukersteder() {
        log.info("Henter sites fra MainManager");
        try {
            var token = getToken();
            var response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v2/sites")
                    .build()
                )
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .toEntity(MainManagerSitesResponse.class)
                .block();

            var stringResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v2/sites")
                    .build()
                )
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .toEntity(String.class)
                .block();

            if (response == null || response.getBody() == null || response.getBody().items() == null) {
                log.error("Fikk ingen sites fra MainManager");
                return List.of();
            }

            log.info("Hentet sites fra MainManager {}", stringResponse.getBody());

            return response.getBody().items().stream()
                .flatMap(item -> Arrays.stream(
                        isNull(item.identification2()) ? new String[0] : item.identification2.split(","))
                    .map(String::trim)
                    .filter(nr -> !nr.isBlank())
                    .map(nr -> FysiskBrukersted.builder()
                        .nr(nr)
                        .navn(item.namePart())
                        .identification(item.identification())
                        .address(item.address1())
                        .postnummer(item.zipCode())
                        .poststed(item.city())
                        .kommune(item.region())
                        .build()))
                .toList();
        } catch (Exception e) {
            log.error("Feil ved henting av sites fra MainManager", e);
            throw e;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainManagerTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainManagerSitesResponse(
        @JsonProperty("Items") List<MainManagerSiteItem> items
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainManagerSiteItem(
        @JsonProperty("Identification2") String identification2,
        @JsonProperty("NamePart") String namePart,
        @JsonProperty("Identification") String identification,
        @JsonProperty("Address1") String address1,
        @JsonProperty("ZipCode") String zipCode,
        @JsonProperty("City") String city,
        @JsonProperty("Region") String region
    ) {}

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}



