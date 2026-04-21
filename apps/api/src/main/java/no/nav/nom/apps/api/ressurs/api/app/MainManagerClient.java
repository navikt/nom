package no.nav.nom.apps.api.ressurs.api.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
public class MainManagerClient {

    private final WebClient webClient;

    public MainManagerClient(AppProperties appProperties, WebClient.Builder builder) {
        this.webClient = builder.clone()
            .baseUrl(appProperties.clients().mainManager().baseUrl())
            .build();
    }

    public List<FysiskBrukersted> getFysiskeBrukersteder() {
        log.info("Henter sites fra MainManager");
        try {
            var response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v2/sites")
                    .build()
                )
                .retrieve()
                .toEntity(MainManagerSitesResponse.class)
                .block();

            if (response == null || response.getBody() == null || response.getBody().items() == null) {
                log.error("Fikk ingen sites fra MainManager");
                return List.of();
            }

            return response.getBody().items().stream()
                .map(item -> new FysiskBrukersted(
                    item.xid(),
                    item.namePart(),
                    item.address1(),
                    item.zipCode(),
                    item.city(),
                    item.region()
                ))
                .toList();
        } catch (Exception e) {
            log.error("ALERT: Feil ved henting av sites fra MainManager", e);
            throw e;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainManagerSitesResponse(
        @JsonProperty("Items") List<MainManagerSiteItem> items
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainManagerSiteItem(
        @JsonProperty("XID") String xid,
        @JsonProperty("NamePart") String namePart,
        @JsonProperty("Address1") String address1,
        @JsonProperty("ZipCode") String zipCode,
        @JsonProperty("City") String city,
        @JsonProperty("Region") String region
    ) {}
}



