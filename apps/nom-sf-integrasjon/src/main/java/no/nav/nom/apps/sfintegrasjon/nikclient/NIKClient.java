package no.nav.nom.apps.sfintegrasjon.nikclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class NIKClient {

    private final NikProperties nikProperties;
    private final RestClient nikRestClient;

    private final String nikScope;

    public NIKClient(
        NikProperties nikProperties,
        RestClient.Builder nikRestClientBuilder
    ) {
        this.nikProperties = nikProperties;
        this.nikRestClient = RestClient.builder()
            .baseUrl(nikProperties.baseUrl())
            .defaultHeaders(headers -> {
                headers.add("Nav-Consumer-Id", "Zalaris");
                headers.add("Nav-Call-Id", "nom-sf-integrasjon");
            })
            .build();

        this.nikScope = nikProperties.aadScope();
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavident(OpprettNavidentRequest opprettNavidentRequest) {
        return nikRestClient.post()
            .uri("/navident/create")
            .body(opprettNavidentRequest)
            .retrieve()
            .toEntity(OpprettNavidentResponse.class);
    }
}
