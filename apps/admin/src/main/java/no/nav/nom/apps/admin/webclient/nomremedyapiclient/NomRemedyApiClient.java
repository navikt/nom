package no.nav.nom.apps.admin.webclient.nomremedyapiclient;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static no.nav.nom.apps.admin.webclient.WebClientUtils.defaultRetry;

@Slf4j
@Service
public class NomRemedyApiClient {

    private final WebClient nomRemedyApiAuthorizedWebClient;

    public NomRemedyApiClient(WebClient nomRemedyApiAuthorizedWebClient) {
        this.nomRemedyApiAuthorizedWebClient = nomRemedyApiAuthorizedWebClient;
    }

    public Flux<Event> getAllDelayedEvents() {
        return nomRemedyApiAuthorizedWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("admin/getAllDelayedStates")
                .build()
            )
            .retrieve()
            .bodyToFlux(Event.class)
            .retryWhen(defaultRetry);
    }

    public List<Event> getAllDelayedEventsAsList() {
        return nomRemedyApiAuthorizedWebClient.get()
            .uri("admin/getAllDelayedEventsBlocking")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToFlux(Event.class)
            .retryWhen(defaultRetry)
            .collectList()
            .block();
    }
}
