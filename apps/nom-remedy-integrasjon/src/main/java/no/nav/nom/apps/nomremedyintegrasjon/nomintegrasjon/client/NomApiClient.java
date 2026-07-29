package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client;

import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.generated.model.OrgEnhet;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.Ressurs;
import no.nav.nom.generated.model.RessursState;
import no.nav.nom.generated.model.Sektor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Service
public class NomApiClient {

    private final RestClient nomApiAuthorizedRestClient;

    public NomApiClient(RestClient nomApiAuthorizedRestClient) {
        this.nomApiAuthorizedRestClient = nomApiAuthorizedRestClient;
    }

    public Ressurs getRessursByNavident(String navident) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/byNavident/{navident}")
                .build(navident)
            )
            .retrieve()
            .body(Ressurs.class);
    }

    public Ressurs getRessursByFid(String fid) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/byFid/{fid}")
                .build(fid)
            )
            .retrieve()
            .body(Ressurs.class);
    }

    public List<RessursState> getRessurser(Sektor sektor, LocalDate sluttdato) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser")
                .queryParam("sektor", sektor)
                .queryParam("sluttdato", sluttdato)
                .build()
            )
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    public OrgEnhet getOrgEnhet(String orgenhetId) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgenheter/{orgenhetId}")
                .build(orgenhetId)
            )
            .retrieve()
            .body(OrgEnhet.class);
    }

    public String getRemedyEnhetId(String orgenhetId) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgenheter/getRemedyEnhetId/{orgenhetId}")
                .build(orgenhetId)
            )
            .retrieve()
            .body(String.class);
    }

    public List<OrgtilknytningState> getOrgtilknytningerForNavident(String navident) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger")
                .queryParam("navident", navident)
                .build()
            )
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    public OrgtilknytningState getOrgtilknytning(String fid) {
        try {
            return nomApiAuthorizedRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/app/orgtilknytninger/{fid}")
                    .build(fid)
                )
                .retrieve()
                .body(OrgtilknytningState.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new OrgtilknytningNotFoundException(String.format("Orgtilknytning with fid %s not found", fid));
        }
    }

    public String getTelefonnummer(String navident) {
        return nomApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/{navident}/telefonnummer")
                .build(navident)
            )
            .retrieve()
            .body(String.class);
    }
}
