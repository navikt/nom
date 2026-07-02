package no.nav.nom.apps.admin.webclient.nomremedyapiclient;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
public class NomRemedyApiClient {

    private final RestClient nomRemedyApiAuthorizedRestClient;

    public NomRemedyApiClient(RestClient nomRemedyApiAuthorizedRestClient) {
        this.nomRemedyApiAuthorizedRestClient = nomRemedyApiAuthorizedRestClient;
    }

    public Flux<Event> getAllDelayedEvents() {
        List<Event> events = nomRemedyApiAuthorizedRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("admin/getAllDelayedStates")
                .build()
            )
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
        return Flux.fromIterable(events != null ? events : List.of());
    }

    public List<Event> getAllDelayedEventsAsList() {
        List<Event> events = nomRemedyApiAuthorizedRestClient.get()
            .uri("admin/getAllDelayedEventsBlocking")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
        return events != null ? events : List.of();
    }
}
