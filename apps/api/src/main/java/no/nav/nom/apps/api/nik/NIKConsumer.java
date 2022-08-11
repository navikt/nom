package no.nav.nom.apps.api.nik;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NIKConsumer {

    @Value("${app.nik.baseUrl}")
    private String nikBaseUrl;

    @Value("${app.appName}")
    private String appName;

    private final WebClient webClient;
    
    public ResponseEntity<OpprettNavidentResponse> opprettNavident(String personid, String forbokstav, String identType) {
        val body = OpprettNavidentRequest.builder()
                .personident(personid)
                .forbokstav(forbokstav)
                .identType(identType)
                .build();

        return webClient.post()
                .uri(String.format("%s/navident", nikBaseUrl))
                .headers(httpHeaders -> {
                    httpHeaders.add("Nav-Consumer-Id", appName);
                    httpHeaders.add("Nav-Call-Id", appName);
                })
                .body(Mono.just(body), OpprettNavidentRequest.class)
                .retrieve()
                .toEntity(OpprettNavidentResponse.class)
                .block();
    }
}
