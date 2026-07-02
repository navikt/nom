package no.nav.nom.apps.admin.webclient.nomapiclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class NomApiClient {

    private final RestClient nomApiAuthorizedRestClient;

    public NomApiClient(RestClient nomApiAuthorizedRestClient) {
        this.nomApiAuthorizedRestClient = nomApiAuthorizedRestClient;
    }

    public String getRessursRemedyEnhet(String navident) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/{navident}/remedyEnhet")
                .build(navident)
            )
            .retrieve()
            .body(String.class);
    }

    public String getEpost(String navident) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/{navident}/epost")
                .build(navident)
            )
            .retrieve()
            .body(String.class);
    }

    public String getOrgEnhetRemedyEnhet(String orgEnhetId) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgenheter/getRemedyEnhetId/{orgenhetId}")
                .build(orgEnhetId)
            )
            .retrieve()
            .body(String.class);
    }
}
