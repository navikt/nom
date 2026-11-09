package no.nav.nom.apps.sfintegrasjon.orgenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.NyRessurs;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.OrgtilknytningerEndring;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.VisningsnavnEndring;
import org.springframework.http.HttpStatusCode;
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

    public void opprettRessurs(NyRessurs ressurs) {
        nomApiAuthorizedRestClient.post()
            .uri("/app/ressurser")
            .body(ressurs)
            .retrieve()
            .onStatus(
                status -> !status.is2xxSuccessful(),
                (_, _) -> {
                    throw new RuntimeException();
                }
            ).toBodilessEntity();
    }

    public void oppdaterVisningsnavn(String navident, VisningsnavnEndring visningsnavnEndring) {
        nomApiAuthorizedRestClient.post()
            .uri(uriBuilder -> uriBuilder.path("/{navident}/navn").build(navident))
            .body(visningsnavnEndring)
            .retrieve()
            .onStatus(
                status -> !status.is2xxSuccessful(),
                (_, _) -> {
                    throw new RuntimeException();
                }
            ).toBodilessEntity();
    }

    public void oppdaterOrgtilknytninger(String navident, OrgtilknytningerEndring orgtilknytningerEndring) {
        nomApiAuthorizedRestClient.post()
            .uri(uriBuilder -> uriBuilder.path("/forRessurs/{navident}/replace").build(navident))
            .body(orgtilknytningerEndring)
            .retrieve()
            .onStatus(
                status -> !status.is2xxSuccessful(),
                (_, _) -> {
                    throw new RuntimeException();
                }
            ).toBodilessEntity();
    }
}

