package no.nav.nom.apps.sfintegrasjon.orgenhet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Service
public class NomApiClient {

    private final RestClient nomApiAuthorizedRestClient;

    public NomApiClient(RestClient nomApiAuthorizedRestClient) {
        this.nomApiAuthorizedRestClient = nomApiAuthorizedRestClient;
    }

    public Optional<OrgEnhetKostnadsstedMappingResponse> getKostnadsstedForOrgEnhet(String orgEnhetId) {
        try {
            return Optional.ofNullable(nomApiAuthorizedRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/app/orgenheter/kostnadssted/{orgEnhetId}").build(orgEnhetId))
                .retrieve()
                .body(OrgEnhetKostnadsstedMappingResponse.class));
        } catch (HttpClientErrorException.NotFound httpNotFound) {
            return Optional.empty();
        }

    }
}

