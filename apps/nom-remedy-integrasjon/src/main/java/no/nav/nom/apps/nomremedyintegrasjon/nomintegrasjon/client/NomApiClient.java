package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client;

import no.nav.nom.generated.model.OrgEnhet;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.Ressurs;
import no.nav.nom.generated.model.RessursState;
import no.nav.nom.generated.model.Sektor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.WebClientUtils.defaultRetry;

@Service
public class NomApiClient {

    private final WebClient nomApiAuthorizedWebClient;

    public NomApiClient(WebClient nomApiAuthorizedWebClient) {
        this.nomApiAuthorizedWebClient = nomApiAuthorizedWebClient;
    }

    public Ressurs getRessursByNavident(String navident) {
        return nomApiAuthorizedWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/byNavident/{navident}")
                .build(navident)
            )
            .retrieve()
            .bodyToMono(Ressurs.class)
            .retryWhen(defaultRetry)
            .block();
    }

    public Ressurs getRessursByFid(String fid) {
        return nomApiAuthorizedWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/byFid/{fid}")
                .build(fid)
            )
            .retrieve()
            .bodyToMono(Ressurs.class)
            .retryWhen(defaultRetry)
            .block();
    }

    public List<RessursState> getRessurser(Sektor sektor, LocalDate sluttdato) {
        return nomApiAuthorizedWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser")
                .queryParam("sektor", sektor)
                .queryParam("sluttdato", sluttdato)
                .build()
            )
            .retrieve()
            .toEntityList(RessursState.class)
            .retryWhen(defaultRetry)
            .block().getBody();
    }

    public OrgEnhet getOrgEnhet(String orgenhetId) {
        return nomApiAuthorizedWebClient.get()
            .uri(uriBuilder -> uriBuilder
                        .path("/app/orgenheter/{orgenhetId}")
                        .build(orgenhetId)
            )
            .retrieve()
            .bodyToMono(OrgEnhet.class)
            .retryWhen(defaultRetry)
            .block();
    }

    public List<OrgtilknytningState> getOrgtilknytningerForNavident(String navident) {
        return nomApiAuthorizedWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger")
                .queryParam("navident", navident)
                .build()
            )
            .retrieve()
            .toEntityList(OrgtilknytningState.class)
            .retryWhen(defaultRetry)
            .block().getBody();
    }

    public OrgtilknytningState getOrgtilknytning(String fid) {
        return nomApiAuthorizedWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}")
                .build(fid)
            )
            .retrieve()
            .bodyToMono(OrgtilknytningState.class)
            .retryWhen(defaultRetry)
            .block();
    }
}
