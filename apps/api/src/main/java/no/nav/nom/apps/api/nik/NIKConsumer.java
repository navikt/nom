package no.nav.nom.apps.api.nik;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NIKConsumer {

    private final AppProperties appProperties;
    private final WebClient webClient;
    
    public ResponseEntity<OpprettNavidentResponse> opprettNavident(String personid, String forbokstav, String identType, String token) {
        val body = OpprettNavidentRequest.builder()
                .personident(personid)
                .forbokstav(forbokstav)
                .identType(identType)
                .build();

        return webClient.post()
                .uri(String.format("%s/navident", appProperties.getNik().getBaseUrl()))
                .headers(httpHeaders -> {
                    httpHeaders.add("Nav-Consumer-Id", appProperties.getAppName());
                    httpHeaders.add("Nav-Call-Id", appProperties.getAppName());
                    httpHeaders.setBearerAuth(token);
                })
                .body(Mono.just(body), OpprettNavidentRequest.class)
                .retrieve()
                .toEntity(OpprettNavidentResponse.class)
                .block();
    }
}
