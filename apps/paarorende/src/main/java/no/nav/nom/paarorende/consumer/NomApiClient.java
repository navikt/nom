package no.nav.nom.paarorende.consumer;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class NomApiClient {

    private static final String LEDER_FOR_PATH = "/app/ressurser/lederFor/{navident}";

    private final WebClient nomApi;

    public NomApiClient(WebClient webClient) {
        this.nomApi = webClient;
    }

    public List<String> getLedereFor(String navident) {
        var ledere = nomApi.get()
            .uri(LEDER_FOR_PATH, navident)
            .retrieve()
            .bodyToMono(RessursLeder[].class)
            .block();

        if (ledere == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(ledere).map(RessursLeder::getNavident).toList();
    }
}
