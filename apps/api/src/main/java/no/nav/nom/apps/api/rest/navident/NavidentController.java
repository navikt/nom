package no.nav.nom.apps.api.rest.navident;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.nik.NIKConsumer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NavidentController {

    private final NIKConsumer nikConsumer;

    @PostMapping
    public ResponseEntity<NavidentResponse> opprettNavident(@RequestBody NavidentRequest request) {
        try {
            val nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getForbokstav(), request.getIdentType(), false);

            if (List.of(HttpStatus.OK, HttpStatus.CREATED).contains(nikResponse.getStatusCode())) {
                return ResponseEntity.ok(NavidentResponse.builder()
                        .navident(nikResponse.getBody().getNavident())
                        .status(nikResponse.getStatusCode())
                        .build());
            }

            return ResponseEntity.ok(NavidentResponse.builder()
                    .status(nikResponse.getStatusCode())
                    .feilmelding(nikResponse.getBody().getFeilmelding())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(NavidentResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .feilmelding("Intern feil inntraff")
                    .build());
        }
    }
}
