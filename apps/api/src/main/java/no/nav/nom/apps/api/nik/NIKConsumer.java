package no.nav.nom.apps.api.nik;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NIKConsumer {
    private final TokenRepository tokenRepository;

    private final WebClient webClient;

    private final AppProperties appProperties;

    public ResponseEntity<OpprettNavidentResponse> opprettNavident(String personid, String forbokstav, String identType, boolean OBO) {
        val body = OpprettNavidentRequest.builder()
                .personid(personid)
                .identPrefix(forbokstav)
                .identType(identType)
                .build();

        return webClient.post()
                .uri(String.format("%s/navident", appProperties.getNik().getBaseUrl()))
                .headers(httpHeaders -> {
                    httpHeaders.add("Nav-Consumer-Id", appProperties.getAppName());
                    httpHeaders.add("Nav-Call-Id", appProperties.getAppName());
                    httpHeaders.setBearerAuth(tokenRepository.getAadToken(appProperties.getNik().getAadClientId(), OBO).serialize());
                })
                .body(Mono.just(body), OpprettNavidentRequest.class)
                .retrieve()
                .toEntity(OpprettNavidentResponse.class)
                .block();
    }
}
