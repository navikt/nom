package no.nav.nom.apps.api.tilgangsenhet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class NorgService {

    private static final String NORG_BASE_URL = "http://norg2.org/norg2";
    private static final String ENHET_PATH = "/api/v1/enhet";

    private WebClient webClient = WebClient.builder()
        .baseUrl(NORG_BASE_URL)
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
        .build();

    private enum ENHETSTYPE_EXCLUSIONS {
        EKSTERN,
        INTRO,
        UTLAND,
        KO
    }

    public List<NorgEnhet> fetch() {
        try {
            var requestStartTime = System.currentTimeMillis();
            var exclusions = Arrays.stream(ENHETSTYPE_EXCLUSIONS.values()).map(ENHETSTYPE_EXCLUSIONS::name).toList();
            var enheter = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path(ENHET_PATH)
                    .queryParam("enhetStatusListe", List.of("AKTIV", "UNDER_AVVIKLING"))
                    .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<NorgEnhet>>() {})
                .timeout(Duration.ofSeconds(10))
                .block();

            var requestTime = Math.ceil((System.currentTimeMillis() - requestStartTime) / 1000.0);
            log.info("Fetched {} enheter from Norg in {}", Optional.ofNullable(enheter).orElse(Collections.emptyList()).size(), requestTime);

            return Optional.ofNullable(enheter).orElse(Collections.emptyList()).stream()
                .filter(enhet -> enhet.enhetNr.equals("2103") || !exclusions.contains(enhet.type))
                .toList();
        } catch (Exception e) {
            log.warn("Failed on fetching enheter from Norg", e);
            return Collections.emptyList();
        }
    }

    public record NorgEnhet(
        String enhetNr,
        String navn,
        String type
    ) {
        public Tilgangsenhet toTilgangsenhet() {
            return new Tilgangsenhet(this.enhetNr, this.navn);
        }
    }
}
