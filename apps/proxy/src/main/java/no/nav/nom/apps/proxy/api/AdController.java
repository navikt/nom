package no.nav.nom.apps.proxy.api;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.proxy.ldap.OnpremAdConsumer;
import no.nav.nom.contract.ad.AdRessurser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class AdController {

    private final OnpremAdConsumer adConsumer;

    @PostMapping("/ad")
    public ResponseEntity<AdRessurser> hentAdRessurser(@AuthenticationPrincipal Jwt jwt, @RequestBody List<String> body) {
        if (body.size() > 100) {
            throw new IllegalArgumentException("Max 100 per request");
        }
        return ResponseEntity.status(HttpStatus.OK).body(new AdRessurser(adConsumer.findRessurser(body)));
    }
}
