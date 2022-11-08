package no.nav.nom.apps.api.nik;

import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class NIKConsumer {
    private final AppProperties.Nik nikProperties;
    private final TokenRepository tokenRepository;
    private final WebClient webClient;

    public NIKConsumer(TokenRepository tokenRepository, WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.nikProperties = appProperties.nik();
        this.tokenRepository = tokenRepository;
        this.webClient = webClientBuilder
                .baseUrl(appProperties.nik().baseUrl() + "/navident")
                .defaultHeaders(headers -> {
                    headers.add("Nav-Consumer-Id", appProperties.appName());
                    headers.add("Nav-Call-Id", appProperties.appName());
                })
                .build();
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavident(String personid, String forbokstav, String identType, boolean OBO) {
        final var body =
                OpprettNavidentRequest.builder()
                        .personid(personid)
                        .identPrefix(forbokstav)
                        .identType(identType)
                        .build();

        return webClient.post()
                .headers(httpHeaders -> httpHeaders.setBearerAuth(tokenRepository.getAadToken(nikProperties.aadClientId(), OBO).serialize()))
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, response -> Mono.empty()) // nav-ident has its own body type for errors actually caught.
                .toEntity(OpprettNavidentResponse.class)
                .block();
    }
}
